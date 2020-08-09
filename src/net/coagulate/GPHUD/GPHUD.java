package net.coagulate.GPHUD;

import net.coagulate.Core.BuildInfo.GPHUDBuildInfo;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.MariaDBConnection;
import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.PageMapper;
import net.coagulate.SL.SL;
import net.coagulate.SL.SLModule;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.logging.Logger;

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
	public final int majorVersion() { return GPHUDBuildInfo.MAJORVERSION; }
	public final int minorVersion() { return GPHUDBuildInfo.MINORVERSION; }
	public final int bugFixVersion() { return GPHUDBuildInfo.BUGFIXVERSION; }
	public final String commitId() { return GPHUDBuildInfo.COMMITID; }
	public Date getBuildDate() { return GPHUDBuildInfo.BUILDDATE; }

	/** branding as an additional note, or blank if no branding
	 *
	 * @return branding as an additional note, or blank if no branding
	 */
	public static String branding() {
		if (!Config.getBrandingName().isBlank()) {
			return "This server is run by "+Config.getBrandingName()+" ("+Config.getBrandingOwnerSLURL()+")";
		}
		if (!Config.isOfficial()) { return "This server is run by a Third Party"; }
		return "";
	}

	/** branding if present, or empty string.  prefixed with a newline if present.
	 *
	 * @return  branding if present, or empty string.  prefixed with a newline if present.
	 */
	public static String brandingWithNewline() {
		if (branding().isEmpty()) { return ""; }
		return "\n"+branding();
	}

	public static String version() { return GPHUDBuildInfo.MAJORVERSION+"."+GPHUDBuildInfo.MINORVERSION+"."+GPHUDBuildInfo.BUGFIXVERSION; }
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

	@Nonnull
	public static String menuPanelEnvironment() {
		if (Config.isOfficial()) {
			return "&gt;&nbsp;" + (Config.getDevelopment() ? "DEVELOPMENT" : "Production") + "<br>" +
					"&gt;&nbsp;" + Config.getHostName() + "<br>" +
					"&gt;&nbsp;<a href=\"https://sl.coagulate.net/Docs/GPHUD/index.php/Release_Notes.html#head\" target=\"_new\">" + version() + "</a><br>" +
					"&gt;&nbsp;<a href=\"/Docs/GPHUD/index.php/Release_Notes.html#head\" target=\"_new\">" + SL.getStackBuildDate() + "</a>";
		} else {
			return  "&gt;&nbsp;<a href=\"https://sl.coagulate.net/Docs/GPHUD/index.php/Release_Notes.html#head\" target=\"_new\">" + version() + "</a><br>" +
					"&gt;&nbsp;<a href=\"/Docs/GPHUD/index.php/Release_Notes.html#head\" target=\"_new\">" + SL.getStackBuildDate() + "</a><br>"+
					"&gt;&nbsp;<a href=\"https://sl.coagulate.net/landingpage\">(C) Coagulate SL</a><br>" +
					"&gt;&nbsp;<b>Operated by:</b><br>" +
					"&gt;&nbsp;" + Config.getBrandingName().replaceAll(" ","&nbsp;") + "<br>" +
					"&gt;&nbsp;" + Config.getBrandingOwnerHumanReadable().replaceAll(" ","&nbsp;") + "<br>";

		}
	}

	@Nonnull
	public static String serverVersion() {
		return "GPHUD Stack " + version() + " " + SL.getStackBuildDate() + " (C) secondlife:///app/agent/" + Config.getCreatorUUID() + "/about / Iain Price, Coagulate";
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

		// tracing, if we're tracing
		if (Config.getDatabasePathTracing()) {
			GPHUD.log().config("Database calling path verification is enabled for GPHUD");
			GPHUD.db.permit("net.coagulate.GPHUD.Data");
		}
	}

	@Override
	public void maintenance() {
		if (nextRun("GPHUD-Maintenance",60,5)) { Maintenance.gphudMaintenance(); }
	}

	@Override
	protected int schemaUpgrade(DBConnection db, String schemaname, int currentversion) {
		Logger log=GPHUD.getLogger("SchemaUpgrade");
		if (currentversion==1) {
			log.config("Schema for GPHUD is at version 1, upgrading to version 2");
			log.config("Add URL index to characters table");
			GPHUD.getDB().d("ALTER TABLE characters ADD INDEX characters_url_index (url)");
			log.config("Add URL index to regions table");
			//noinspection SpellCheckingInspection
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

	@Override
	public Object weakInvoke(String command, Object... arguments) {
		if (command.equalsIgnoreCase("im")) {
			if (!Config.getDistributionRegion().isBlank()) {
				Region r=Region.findNullable(Config.getDistributionRegion(),false);
				if (r==null) {
					log().warning("Instant messaging services unavailable, Distribution Region badly configured");
					return null;
				}
				JSONObject json=new JSONObject();
				json.put("instantmessage", arguments[0]);
				json.put("instantmessagemessage", arguments[1]);
				r.sendServerSync(json);
			}
		}
		return null;
	}
}
