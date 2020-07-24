package net.coagulate.GPHUD;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.MariaDBConnection;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.GPHUD.Data.Char;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * Bootstrap class for GPHUD.  Run me.
 * Initialise config file
 * Initialise systems in order
 * GPHUD HTTP listener
 * Relax.
 *
 * @author Iain Price <gphud@predestined.net>
 */

/*
Shortform here.

[XX Release Notes] | [https://bugs.coagulate.net/ Bugs/Requests] | [secondlife:///app/group/2a6790d0-c594-7467-804b-c8e398970188/about Update Notices]

 */
public class GPHUD {
	public static final String VERSION="v3.16.2";
	public static final String VERSION_DATE="July 15th 2020";
	// config KV store
	private static final Map<String,String> CONFIG=new TreeMap<>();
	public static String hostname="UNSET";
	public static boolean DEV; // make this auto detect some day... or in the ini file :P
	@Nullable
	private static Logger log;
	@Nullable
	private static DBConnection db;

	// ---------- STATICS ----------
	public static Logger getLogger(final String subspace) { return Logger.getLogger(log().getName()+"."+subspace); }

	@Nonnull
	public static Logger getLogger() {
		return log();
	}

	@Nonnull
	public static DBConnection getDB() {
		if (db==null) { throw new SystemInitialisationException("Calling DB before DB is initialised"); }
		return db;
	}

	public static String get(@Nonnull final String keyword) { return CONFIG.get(keyword.toUpperCase()); }

	// spread the servers over a cycle (which is modulo 4), which is just a reduced timestamp anyway (timesync IMPORTANT!)
	// note zones 1 and 3 are left blank, to allow a little time desync safety (a few minutes worth, which is more than my system monitoring allows, but avoiding running
	// them back to back is good).
	// luna being a stand alone dev system runs both 1 and 3.  as does sol if DEV
	public static boolean ourCycle(int cyclenumber) {
		cyclenumber=cyclenumber%2;
		final String node=hostname;
		if (DEV) { return true; }
		if ("sol".equalsIgnoreCase(node) && cyclenumber==0) { return true; }     // sol, runs slot 0 on production
		if ("pluto".equalsIgnoreCase(node) && cyclenumber==1) {
			return true;
		}  // pluto only runs prod, and runs in slot 1
		return false;
	}

	@Nonnull
	public static String menuPanelEnvironment() {
		return "&gt; "+(DEV?"DEVELOPMENT":"Production")+"<br>&gt; "+hostname+"<br>&gt; <a href=\"https://sl.coagulate.net/Docs/GPHUD/index.php/Release_Notes.html#head\" target=\"_new\">"+GPHUD.VERSION+"</a><br>&gt; <a href=\"/Docs/GPHUD/index.php/Release_Notes.html#head\" target=\"_new\">"+GPHUD.VERSION_DATE+"</a>";
	}

	// TODO THIS MESSES WITH URLS
	public static void purgeURL(final String url) {
		try {
			for (final ResultsRow row: getDB().dq("select characterid,regionid from characters where url=?",url)) {
				try {
					final int charid=row.getInt("characterid");
					final Char ch=Char.get(charid);
					final State st=State.getNonSpatial(ch);
					getDB().d("update eventvisits set endtime=UNIX_TIMESTAMP() where characterid=?",charid);
					final Integer regionid=row.getIntNullable("regionid");
					if (regionid!=null) {
						final int howmany=getDB().dqinn("select count(*) from visits visits where endtime is null and characterid=? and regionid=?",charid,regionid);
						if (howmany>0) {
							st.logger()
							  .info("HUD disconnected (404) from avatar "+st.getAvatar().getName()+" as character "+st.getCharacter()
							                                                                                          .getName()+", not reported as region leaver.");
						}
						getDB().d("update visits set endtime=UNIX_TIMESTAMP() where characterid=? and regionid=? and endtime is null",charid,regionid);
					}
				}
				catch (@Nonnull final Exception e) {
					GPHUD.getLogger("Character").log(WARNING,"Exception during per character purgeURL",e);
				}
			}
			getDB().d("update characters set playedby=null, url=null, urlfirst=null, urllast=null, authnode=null,zoneid=null,regionid=null where url=?",url);
			getDB().d("update objects set url=null where url=?",url);
		}
		catch (@Nonnull final DBException ex) {
			GPHUD.getLogger().log(SEVERE,"Failed to purge URL from characters",ex);
		}
		try {
			getDB().d("update regions set url=null,authnode=null where url=?",url);
		}
		catch (@Nonnull final DBException ex) {
			GPHUD.getLogger().log(SEVERE,"Failed to purge URL from regions",ex);
		}
	}

	public static void initialiseAsModule(final boolean isdev,
	                                      final String jdbc,
	                                      final String hostname) {
		GPHUD.hostname=hostname;
		log=Logger.getLogger("net.coagulate.GPHUD");
		// Load DB hostname, username and password, from local disk.  So we dont have credentials in Git.
		log().config("GPHUD as module starting up... "+VERSION);
		log().config("Server operating on node "+hostname);
		//Classes.initialise(); if (1==1) { System.exit(0); }

		if (isdev) {
			DEV=true;
			log().config("Configuration declares us as a DEVELOPMENT NODE");
		}
		// Initialise the Database layer
		db=new MariaDBConnection("GPHUD"+(isdev?"DEV":""),jdbc);

		// Annotation parser
		Classes.initialise();
	}

	@Nonnull
	public static String serverVersion() {
		return "GPHUD Cluster "+VERSION+" "+VERSION_DATE+" (C) secondlife:///app/agent/8dc52677-bea8-4fc3-b69b-21c5e2224306/about / Iain Price, Coagulate";
	}

	@Nonnull
	public static Logger log() {
		if (log==null) { throw new SystemInitialisationException("Log not yet initialised"); }
		return log;
	}
}
