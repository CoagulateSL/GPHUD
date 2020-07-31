package net.coagulate.GPHUD;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.MariaDBConnection;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.PageMapper;
import net.coagulate.SL.SLCore;
import net.coagulate.SL.SLModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;
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
public class GPHUD extends SLModule {
	public static final int MAJORVERSION=3;
	public static final int MINORVERSION=16;
	public static final int BUGFIXVERSION=10;
	public static final String COMMITID ="MANUAL";
	public final int majorVersion() { return MAJORVERSION; }
	public final int minorVersion() { return MINORVERSION; }
	public final int bugFixVersion() { return BUGFIXVERSION; }
	public final String commitId() { return COMMITID; }
	public static final String version() { return MAJORVERSION+"."+MINORVERSION+"."+BUGFIXVERSION; }
	// config KV store
	private static final Map<String,String> CONFIG=new TreeMap<>();
	@Nullable
	static Logger log;
	@Nullable
	static DBConnection db;

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

	@Nonnull
	public static String menuPanelEnvironment() {
		return "&gt; "+(Config.getDevelopment()?"DEVELOPMENT":"Production")+"<br>&gt; "+Config.getHostName()+"<br>&gt; <a href=\"https://sl.coagulate.net/Docs/GPHUD/index.php/Release_Notes.html#head\" target=\"_new\">"+version()+"</a><br>&gt; <a href=\"/Docs/GPHUD/index.php/Release_Notes.html#head\" target=\"_new\">"+ SLCore.getBuildDate()+"</a>";
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

	@Nonnull
	public static String serverVersion() {
		return "GPHUD Cluster "+version()+" "+SLCore.getBuildDate()+" (C) secondlife:///app/agent/8dc52677-bea8-4fc3-b69b-21c5e2224306/about / Iain Price, Coagulate";
	}

	@Nonnull
	public static Logger log() {
		if (log==null) { throw new SystemInitialisationException("Log not yet initialised"); }
		return log;
	}

	@Nonnull
	@Override
	public String getName() {
		return "GPHUD";
	}

	@Nonnull
    @Override
	public String getDescription() {
		return "General Purpose Heads Up Display Toolkit";
	}

	@Override
	public void initialise() {
		GPHUD.log= Logger.getLogger("net.coagulate.GPHUD");

		// Initialise the Database layer
		GPHUD.db=new MariaDBConnection("GPHUD"+(Config.getDevelopment()?"DEV":""),Config.getGPHUDJdbc());
		schemaCheck(GPHUD.db,"gphud",2);

		// Annotation parser
		Classes.initialise();
	}

	@Override
	public void maintenance() {
		if (nextRun("GPHUD-Maintenance",60)) { Maintenance.gphudMaintenance(); }
	}

	@Override
	protected int schemaUpgrade(DBConnection db, String schemaname, int currentversion) {
		Logger log=GPHUD.getLogger("SchemaUpgrade");
		if (currentversion==1) {
			log.config("Schema for GPHUD is at version 1, upgrading to version 2");
			log.config("Add URL index to characters table");
			GPHUD.getDB().d("ALTER TABLE characters ADD INDEX characters_url_index (url)");
			log.config("Add URL index to regions table");
			GPHUD.getDB().d("ALTER TABLE regions ADD INDEX regionss_url_index (url)");
			log.config("Schema upgrade of GPHUD to version 2 is complete"); currentversion=2;
		}
		return currentversion;
	}

	@Override
	public void startup() {
		PageMapper.exact("/GPHUD/external",new net.coagulate.GPHUD.Interfaces.External.Interface());
		PageMapper.exact("/GPHUD/system",new net.coagulate.GPHUD.Interfaces.System.Interface());
		PageMapper.prefix("/GPHUD/",new net.coagulate.GPHUD.Interfaces.User.Interface());
	}

	@Override
	public void shutdown() {

	}
}
