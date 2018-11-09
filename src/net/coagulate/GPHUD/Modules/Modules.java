package net.coagulate.GPHUD.Modules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import static java.util.logging.Level.SEVERE;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**  Static superclass that handles all the modules and delegating things around, accumulating info, etc.
 * Groups functionality into presented configuration pages.
 * @author iain
 */
public abstract class Modules {
    static Map<String,Module> modules=new TreeMap<>();
    static void register(Module mod) throws SystemException {
        String name=mod.getName();
        if (modules.containsKey(name.toLowerCase())) {
            throw new SystemException("Duplicate Module definition for "+name);
        }
        modules.put(name.toLowerCase(),mod);
    }
    /** Get a list of all modules registered.
     * 
     * @return 
     */
    public static List<Module> getModules() {
        List<Module> set=new ArrayList<>();
        for (String name:modules.keySet()) { set.add(modules.get(name)); }
        return set;
    }
    
    /** Check a given module exists.
     * 
     * @param modulename
     * @return 
     */    
    public static boolean exists(String modulename) { return modules.containsKey(modulename.toLowerCase()); }
       
    /** Get a module, optionally checking if its enabled.
     * 
     * @param st Session state, used for enablement check, or null to skip enablement check.
     * @param nameorreference A name of a module, or a reference (e.g. module.reference)
     * @return the Module
     * @throws UserException  if the module doesn't exist, or isn't enabled.
     */
    public static Module get(State st,String nameorreference) throws UserException, SystemException {
        Module m=modules.get(extractModule(nameorreference).toLowerCase());
        if (st!=null && !m.isEnabled(st)){ throw new UserException("Module "+m.getName()+" is not enabled in this instance"); }
        if (st==null) { return m; }
        // check dependancies
        if (m.requires(st).isEmpty()) { return m; }
        String[] dependancies=m.requires(st).split(",");
        for (String dependancy:dependancies) {
            Module m2=get(null,dependancy);
            if (!m2.isEnabled(st)) { throw new UserException("Module "+m.getName()+" is not enabled in this instance because it depends on "+m2.getName()+" which is disabled"); }
        }
        return m;
    }
    
    /** Extracts a module name from a name or reference.
     * 
     * @param qualified Name in format of "module" or "module.reference"
     * @return The module section of the name
     * @throws UserException if the module does not exist or the input is in invalid format
     */
    public static String extractModule(String qualified) throws UserException {
        //System.out.println("QUALIFIED:"+qualified);
        String[] parts=qualified.split("\\.");
        if (parts.length<1 || parts.length>2) { throw new UserException("Invalid format, must be module or module.reference but we received "+qualified); }
        String name=parts[0];
        //System.out.println("NAME:"+name);
        if (!modules.containsKey(name.toLowerCase())) { throw new UserException("Module "+name+" does not exist."); }
        return name;
    }
    
    /** Extracts the second part of a reference.
     * 
     * @param qualified A reference (module.reference)
     * @return The reference part
     * @throws UserException if the module does not exist or the input is in invalid format
     */
    public static String extractReference(String qualified) throws UserException {
        String[] parts=qualified.split("\\.");
        if (parts.length!=2) { throw new UserException("Invalid format, must be module.reference but we received "+qualified); }
        extractModule(qualified); // validates the module
        String reference=parts[1];
        return reference;
    }

    // validate a KV mapping exists
    public static void validateKV(State st,String key) throws UserException, SystemException {
        get(null,key).validateKV(st,key);
    }
    public static URL getURL(State st,String url) throws UserException, SystemException {
        return getURL(st,url,true);
    }

    /** Gets a page by URL.
     *
     * @param st Session state
     * @param url URL
     * @param exception True to exception on unknown URL, otherwise returns null
     * @return URL object, or null (if exception==false)
     * @throws UserException on page not found if and only if exception is true
     * @throws SystemException if multiple URLs match (internal error)
     */
    public static URL getURL(State st,String url,boolean exception) throws UserException, SystemException {
        if (url.startsWith("/GPHUD/")) { url=url.substring(6); }
        URL literal=null;
        URL relaxed=null;
        for (Module mod:modules.values()) {
            if (mod.isEnabled(st)) {
                URL proposed=mod.getURL(st, url);
                if (proposed!=null) { 
                    if (proposed.url().endsWith("*")) {
                        if (relaxed!=null) {
                            // break if matching prefix length, otherwise...
                            if (relaxed.url().length()==proposed.url().length()) { throw new SystemException("Multiple relaxed matches between "+proposed.url()+" and "+relaxed.url()); }
                            if (relaxed.url().length()<proposed.url().length()) { relaxed=proposed; }  // pick longer prefix
                        }
                        else
                        { relaxed=proposed; }
                    } else {
                        if (literal!=null) { throw new SystemException("Multiple literal matches between "+proposed.url()+" and "+literal.url()); }
                        literal=proposed;
                    }
                }
            }
        }
        // if there's a strict match
        if (literal!=null) { return literal; }
        // otherwise the relaxed match
        if (relaxed!=null) { return relaxed; }
        // if not then its a 404.  do we exception or return null?
        if (exception) { throw new UserException("404 // Page not found ["+url+"]"); }
        return null;
    }

    public static void validatePermission(State st,String requirespermission) throws UserException, SystemException {
        get(null,requirespermission).validatePermission(st,extractReference(requirespermission));
    }

    public static Command getCommand(State st,String proposedcommand) throws UserException, SystemException {
        return get(st,proposedcommand).getCommand(st,extractReference(proposedcommand));
    }    
    
    public static void getHtmlTemplate(State st, String qualifiedcommandname) throws UserException, SystemException {
        get(st,qualifiedcommandname).getCommand(st,extractReference(qualifiedcommandname)).getHtmlTemplate(st);
    }
    
    public static JSONObject getJSONTemplate(State st,String qualifiedcommandname) throws UserException, SystemException {
        Module module = get(st,qualifiedcommandname);
        if (module==null) { throw new UserException("Unable to resolve module in "+qualifiedcommandname); }
        Command command = module.getCommand(st,extractReference(qualifiedcommandname));
        if (command==null) { throw new UserException("Unable to resolve command in "+qualifiedcommandname); }
        return command.getJSONTemplate(st);
    }

    public static Response getJSONTemplateResponse(State st, String command) throws UserException, SystemException { return new JSONResponse(getJSONTemplate(st,command)); }

    public static Response run(State st,String console) throws UserException,SystemException {
        if (console==null || console.equals("")) { return new ErrorResponse("No console string supplied"); }
        String words[]=console.split(" ");
        int i=0;
        String command=words[0].toLowerCase();
        if (command.startsWith("*")) { command=command.substring(1); }
        Command c=null;
        try { c=getCommand(st,command); } catch (UserException e) { return new ErrorResponse("Unable to find command '"+command+"' - "+e.getLocalizedMessage()); }
        if (c==null) { return new ErrorResponse("Failed to find command "+command); }
        if (c.permitConsole()==false) { return new ErrorResponse("Command '"+command+"' can not be called from the console"); }
        SafeMap parameters=new SafeMap();
        for (Argument arg:c.getArguments()) {            
            i++;
            if (i>=words.length) { return new ErrorResponse("Not enough parameters supplied"); }
            String argname=arg.getName();
            if (words[i]==null || words[i].isEmpty() || words[i].equals("-")) {
                parameters.put(argname,null); 
            } else {
                boolean respectnewlines=false;
                switch (arg.type()) {
                    case TEXT_MULTILINE:
                        respectnewlines=true;
                    default:
                        if (words[i].startsWith("\"")) {
                            if (words[i].endsWith("\"")) { // weird but sure
                                parameters.put(argname,words[i]);
                            } else {
                                // is a multi word thing
                                String string="";
                                while (!words[i].endsWith("\"")) {
                                    if (!string.isEmpty()) { string=string+" "; }
                                    string=string+words[i];
                                    i++;
                                }
                                string+=" "+words[i];
                                if (!respectnewlines) { 
                                    string=string.replaceAll("\n","");
                                    string=string.replaceAll("\r","");
                                }
                                string=string.replaceFirst("^\"","");
                                string=string.replaceAll("\"$","");
                                //System.out.println(string);
                                parameters.put(argname,string);
                            }
                        } else {
                            // is a single word thing
                            parameters.put(argname,words[i]);
                        }
                        break;
                }
            }
        }
        Response response=c.run(st,parameters);
        return response;
    }

    
    public static Response run(State st, String qualifiedcommandname, SafeMap parametermap) throws UserException, SystemException {
        if (st==null) { throw new SystemException("Null state"); }
        if (qualifiedcommandname==null) { throw new SystemException("Null command"); }
        if (qualifiedcommandname.equalsIgnoreCase("console")) { return run(st,parametermap.get("console")); }
        Module module = get(st,qualifiedcommandname);
        if (module==null) { throw new UserException("Unknown module in "+qualifiedcommandname); }
        Command command = module.getCommand(st,extractReference(qualifiedcommandname));
        if (command==null) { throw new UserException("Unknown command in "+qualifiedcommandname); }
        return command.run(st,parametermap);
    }
    public static Response run(State st,String qualifiedcommandname,List<String> args) throws UserException, SystemException {
        return run(st,qualifiedcommandname,args.toArray(new String[]{}));
    }    
    public static Response run(State st,String qualifiedcommandname,String[] args) throws UserException, SystemException {
        return get(st,qualifiedcommandname).getCommand(st,extractReference(qualifiedcommandname)).run(st,args);
    }

    public static Permission getPermission(State st, String qualifiedname) throws UserException, SystemException {
        return get(st,qualifiedname).getPermission(st,extractReference(qualifiedname));
    }

    public static KV getKVDefinition(State st,String qualifiedname) throws UserException, SystemException {
        KV kv=null;
        if (qualifiedname==null) { throw new SystemException("Null qualified name for KV definition?"); }
        if (qualifiedname.toLowerCase().endsWith(".enabled")) {
            kv=get(null,qualifiedname).getKVDefinition(st, extractReference(qualifiedname));
        } else {
            kv=get(st,qualifiedname).getKVDefinition(st, extractReference(qualifiedname));
        }
        if (kv==null) { throw new SystemException("Failed to resolve KV definition "+qualifiedname); }
        return kv;
    }

    public static Set<String> getKVList(State st) {
        Set<String> kvs=new TreeSet<>();
        for (Module m:getModules())
        {
            if (m.isEnabled(st)) {
                for (String s:m.getKVDefinitions(st).keySet()) {
                    kvs.add(m.getName()+"."+s);
                }
            }
        }
        return kvs;
    }
    public static Set<KV> getKVSet(State st) {
        Set<KV> kvs=new HashSet<>();
        for (Module m:getModules())
        {
            if (m.isEnabled(st)) {
                Map<String, KV> getkvs = m.getKVDefinitions(st);
                for (String s:getkvs.keySet()) {
                    kvs.add(getkvs.get(s));
                }
            }
        }
        return kvs;
    }
    public static SafeMap getConveyances(State st) {
        SafeMap convey=new SafeMap();
        for (Module mod:getModules()) {
            try {
                if (mod.isEnabled(st)) {
                    for (String key:mod.getKVDefinitions(st).keySet()) {
                        KV kv=mod.getKVDefinition(st, key);
                        // this seems broken?
                        String value=st.getKV(kv.fullname()).value();
                        if (!kv.conveyas().isEmpty()) { convey.put(kv.conveyas(), value); }
                    }
                }
            }
            catch (Exception e) {
                st.logger().log(SEVERE,"Exception compiling conveyance",e);
            }
                       
        }
        return convey;
    }

    public static Pool getPool(State st,String qualifiedname) throws UserException, SystemException {
        return get(st,qualifiedname).getPool(st,extractReference(qualifiedname));
    }
    public static Pool getPoolNotNull(State st,String qualifiedname) throws UserException, SystemException {
        Pool pool=getPool(st,qualifiedname);
        if (pool==null) { throw new UserException("Unable to find pool "+qualifiedname); }
        return pool;
    }

    public static void simpleHtml(State st, String command, SafeMap values) throws UserException, SystemException {
        if (command==null) { throw new SystemException("Null command"); }
        if (values==null) { throw new SystemException("Null values"); }
        if (st==null) { throw new SystemException("Null state"); }
        Module m=get(st,command);
        if (m==null) { throw new UserException("No such module in "+command); }
        Command c=m.getCommand(st,extractReference(command));
        if (c==null) { throw new UserException("No such command in "+command); }
        c.simpleHtml(st, values);
    }

    public static void initialiseInstance(State st) {
        for (Module module:getModules()) {
            module.initialiseInstance(st);
        }
    }

    public static Map<String, KV> getKVAppliesTo(State st,TableRow dbo) {
        Map<String, KV> filtered = new TreeMap<>();
        for (Module m:getModules()) {
            Map<String, KV> fullset = m.getKVAppliesTo(st, dbo);
            filtered.putAll(fullset);
        }
        return filtered;
    }


    
}
