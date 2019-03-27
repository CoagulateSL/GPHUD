package net.coagulate.GPHUD;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.MariaDBConnection;
import net.coagulate.Core.Tools.LogHandler;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;

/** Bootstrap class for GPHUD.  Run me.
 Initialise config file
 Initialise systems in order
 GPHUD HTTP listener
 Relax.
 * @author Iain Price <gphud@predestined.net>
 */
public class GPHUD {
    public static String hostname="UNSET";
    public static Integer nodeid=null;
    private static Logger log=null;
    public static Logger getLogger(String subspace) { return Logger.getLogger(log.getName()+"."+subspace); }
    public static Logger getLogger() { return log; }
    private static DBConnection db=null;
    public static DBConnection getDB() { return db; }

    // config KV store
    private static final Map<String,String> CONFIG=new TreeMap<>();
    public static final String VERSION="v3.5.3";
    
    public static boolean DEV=false; // make this auto detect some day... or in the ini file :P
    
    // return codes
    // 1 - configurational problem during startup
    // 2 - unauthenticated shutdown called
    // 3 - Listener crashed (?)
    // 4 - Lost control of maintenance thread (it ran for 45 seconds, failed to interrupt() and exit and failed to (deprecated) stop() and exit)
    
    /**
     * @param args the command line arguments
     * @throws net.coagulate.GPHUD.SystemException
     */
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws SystemException, UserException {  
        LogHandler.initialise();
        log=Logger.getLogger("net.coagulate.GPHUD");
        // Load DB hostname, username and password, from local disk.  So we dont have credentials in Git.
        log.config("GPHUD Server starting up... "+VERSION);
        try {
            hostname=java.net.InetAddress.getLocalHost().getHostName().replaceAll(".coagulate.net","");
            log.config("Server operating on node "+Interface.getNode());
            validateNode(hostname);
        } 
        catch (UnknownHostException e)
        { throw new SystemException("Unable to resolve local host name",e); }

        log.config("Loading configuration file...");
        if (args.length!=1) { 
            log.severe("Incorrect number of command line parameters, should be exactly 1, the location of a configuration file.");
            System.exit(1);
        }
        
        // config
        loadConfig(args[0]);
        log.config("Loaded "+CONFIG.size()+" configuration elements from the file");
        validateConfig();

        if (get("DEV").equals("1")) { DEV=true; log.config("Configuration declares us as a DEVELOPMENT NODE"); }
        // Initialise the Database layer
        dbInit();
        
        // Annotation parser
        Classes.initialise();

        // finally open the listener
        HTTPListener.initialise(Integer.parseInt(get("PORT")));
        //
        
        log.config("Database is ready, HTTP socket is open, startup has successfully completed.");
        // twiddle thumbs
        log.info("Main thread entering pre-maintenance sleep.");
        syncToMinute();
        log.info("Main thread entering maintenance loop.");
        
        while (1==1) // until shutdown time, however we do that
        {
            try {
                Maintenance thread=new Maintenance();
                thread.start();
                try { Thread.sleep(45000); }
                catch (InterruptedException e) { } 
                if (thread.isAlive()) { thread.interrupt(); log.warning("Maintenance loop ran for 45 seconds, interrupting!"); }
                try { Thread.sleep(5000); }
                catch (InterruptedException e) { }
                if (thread.isAlive()) { log.severe("Maintenance loop ran for 45 seconds and failed to interrupt within 5 seconds!"); }
                try { Thread.sleep(5000); }
                catch (InterruptedException e) { }
                if (thread.isAlive()) { log.severe("Maintenance failed interrupt, trying to force STOP()!"); 
                    thread.stop();
                }
                syncToMinute();
                if (thread.isAlive()) { log.severe("Maintenance loop failed to stop().  Terminating application."); System.exit(4); }
                    
            }
            catch (Exception e) { 
                log.log(SEVERE,"Maintenance thread threw unchecked exception?",e);
            }
        }
        // error, unreachable code :P
        //HTTPListener.shutdown();
        //GPHUD.getDB().shutdown();
        //System.exit(0);
    }
    
    public static void loadConfig(String filename) {
        BufferedReader file=null;
        try {
            file = new BufferedReader(new FileReader(filename));
            String line=file.readLine();
            while (line!=null) {
                line=line.trim();
                if (line.length()!=0 && !line.startsWith("#")) {
                    // if its not blank or comment, then process as a "KEY=VALUE" line
                    int splitat=line.indexOf('=');
                    if (splitat==-1 || splitat==(line.length()-1)) {
                        // = not found, or the split (=) is the last character, so finding the 'value' would probably array out of bounds.  Setting blank values is not supported :P
                        log.warning("Invalid configuration line: "+line);
                    } else {
                        String key=line.substring(0,splitat);
                        String value=line.substring(splitat+1);
                        key=key.toUpperCase();
                        if (CONFIG.containsKey(key)) { log.warning("Duplicate definition of "+key+" in configuration file, using last declaration"); }
                        CONFIG.put(key,value);
                    }
                }
                line=file.readLine();
            }
        } catch (FileNotFoundException e) {
            log.log(SEVERE,"File not found accessing "+filename,e);
            System.exit(1);
        } catch (IOException e) {
            log.log(SEVERE,"IOException reading configuration file "+filename,e);
            System.exit(1);
        } finally {
            try { if (file!=null) { file.close(); } } catch (IOException e) {}
        }
    }

    private static void validateConfig(){
        // basically a list of musthaves
        boolean ok=true; // allow us to make all checks rather than bail at the first problem
        ok=requireConfig("DBHOST") && ok;
        ok=requireConfig("DBNAME") && ok;
        ok=requireConfig("DBUSER") && ok;
        ok=requireConfig("DBPASS") && ok;
        if (!ok) { 
            log.severe("Failed configuration validation.  Exiting."); 
            System.exit(1);
        }
        // support some default settings too
        defaultConfig("PORT","13579");
        defaultConfig("UNAUTHENTICATEDSHUTDOWN","0");
        defaultConfig("DEV","0");
    }

    private static boolean requireConfig(String keyword) {
        keyword=keyword.toUpperCase();
        if (CONFIG.containsKey(keyword)) { return true; }
        log.severe("Missing mandatory configuration element '"+keyword+"'");
        return false;
    }
    
    private static void defaultConfig(String keyword,String value) {
        keyword=keyword.toUpperCase();
        if (CONFIG.containsKey(keyword)) { return; }
        CONFIG.put(keyword,value);
    }
    
    public static String get(String keyword) { return CONFIG.get(keyword.toUpperCase()); }

    private static void validateNode(String node) throws SystemException {
        if (    node.equalsIgnoreCase("luna") ||
                node.equalsIgnoreCase("sol") ||
                node.equalsIgnoreCase("saturn") ||
                node.equalsIgnoreCase("mars") ||
                node.equalsIgnoreCase("neptune") ||
                node.equalsIgnoreCase("pluto")
            )
        { return ; }
        throw new SystemException("Unrecognised node name, this would break the scheduler");
    }
    
    // spread the servers over a cycle (which is modulo 4), which is just a reduced timestamp anyway (timesync IMPORTANT!)
    // note zones 1 and 3 are left blank, to allow a little time desync safety (a few minutes worth, which is more than my system monitoring allows, but avoiding running them back to back is good).
    // luna being a stand alone dev system runs both 1 and 3.  as does sol if DEV
    public static boolean ourCycle(int cyclenumber) {
        cyclenumber=cyclenumber%2;
        String node=hostname;
        if (DEV) { return true; }
        if (node.equalsIgnoreCase("sol") && cyclenumber==0) { return true; }     // sol, runs slot 0 on production
        if (node.equalsIgnoreCase("pluto") && cyclenumber==1) { return true; }  // pluto only runs prod, and runs in slot 1
        return false;
    }

    public static String environment() {
        String node=hostname;
        if (DEV) { return "[==DEVELOPMENT // "+node+"==]\n \n"; }
        return "[Production // "+node+"]\n \n";
    }
    public static String menuPanelEnvironment() {
        return "&gt; "+(DEV?"DEVELOPMENT":"Production")+"<br>&gt; "+hostname;
    }
    private static void syncToMinute() {
        int seconds=Calendar.getInstance().get(Calendar.SECOND);
        seconds=60-seconds;
        try {Thread.sleep((long) (seconds*1000.0)); } catch (InterruptedException e) {}
    }
    
    public static void dbInit() {
        db=new MariaDBConnection("GPHUD",get("DBHOST"),get("DBUSER"),get("DBPASS"),get("DBNAME"));
    }

    public static void purgeURL(String url) { 
        //System.out.println("Purge URL "+url);
        try {
            getDB().d("update characters set playedby=null, url=null, urlfirst=null, urllast=null where url=?",url);
        } catch (DBException ex) {
            GPHUD.getLogger().log(SEVERE,"Failed to purge URL from characters",ex);
        }
        try {
            getDB().d("update regions set url=null,authnode=null where url=?",url);
        } catch (DBException ex) {
            GPHUD.getLogger().log(SEVERE,"Failed to purge URL from regions",ex);
        }
    }    

    public static void initialiseAsModule(boolean isdev,String jdbc,String hostname,int nodeid) {
        GPHUD.hostname=hostname;
        GPHUD.nodeid=nodeid;
        log=Logger.getLogger("net.coagulate.GPHUD");
        // Load DB hostname, username and password, from local disk.  So we dont have credentials in Git.
        log.config("GPHUD as module starting up... "+VERSION);
        log.config("Server operating on node "+hostname);
        //Classes.initialise(); if (1==1) { System.exit(0); }        

        if (isdev) { DEV=isdev; log.config("Configuration declares us as a DEVELOPMENT NODE"); }
        // Initialise the Database layer
        db=new MariaDBConnection("GPHUD"+(isdev?"DEV":""),jdbc);
        
        // Annotation parser
        Classes.initialise();
    }
    
}
