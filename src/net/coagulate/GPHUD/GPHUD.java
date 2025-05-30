package net.coagulate.GPHUD;

import net.coagulate.Core.BuildInfo.GPHUDBuildInfo;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.MySqlDBConnection;
import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.Core.HTTP.URLDistribution;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.PermissionsGroup;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Modules.Instance.Distribution;
import net.coagulate.GPHUD.Tests.TestFramework;
import net.coagulate.SL.*;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.HTML.ServiceTile;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
Short form here.

[https://sl.coagulate.net/GPHUD/ChangeLog Changes] [secondlife:///app/group/2a6790d0-c594-7467-804b-c8e398970188/about Update Notices] [https://github.com/CoagulateSL/GPHUD/issues Bugs/Requests]

 */
public class GPHUD extends SLModule {
	
	/** The interval in seconds for fast events, like checking event start */
	public static final  int          FAST_MAINT_INTERVAL=60;
	/** The interval in seconds for slow events such as refreshing report quota or checking expiration of regions/instances */
	public static final  int          SLOW_MAINT_INTERVAL=60*60;
	/** The allowed variance for the fast timer */
	public static final  int          MAINT_VAR_SMALL    =5;
	/** The allowed variance for the slow timer */
	public static final  int          MAINT_VAR_LARGE    =15*60;
	private static final int          DAY_MAINT_INTERVAL =60*60*24;
	private static final int          MAINT_VAR_DAY_LARGE=60*60;
	@Nullable static     Logger       log                =null;
	@Nullable static     DBConnection db                 =null;
	
	/**
	 * branding if present, or empty string.  prefixed with a newline if present.
	 *
	 * @return branding if present, or empty string.  prefixed with a newline if present.
	 */
	public static String brandingWithNewline() {
		if (branding().isEmpty()) {
			return "";
		}
		return "\n"+branding();
	}
	
	/**
	 * branding as an additional note, or blank if no branding
	 *
	 * @return branding as an additional note, or blank if no branding
	 */
	public static String branding() {
		if (!Config.getBrandingName().isBlank()) {
			return "This server is run by "+Config.getBrandingName()+" ("+Config.getBrandingOwnerSLURL()+")";
		}
		if (!Config.isOfficial()) {
			return "This server is run by a Third Party";
		}
		return "";
	}
	
	/**
	 * Get the menu panel host/version info, customised for this environment
	 *
	 * @return A block of HTML
	 */
	@Nonnull
	public static String menuPanelEnvironment() {
		if (Config.isOfficial()) {
			return "&gt;&nbsp;"+(Config.getDevelopment()?"DEVELOPMENT":"Production")+"<br>"+"&gt;&nbsp;"+
			       Config.getHostName()+"<br>"+"&gt;&nbsp;<a href=\"/GPHUD/ChangeLog\" target=\"_new\">"+
			       SL.getModule("GPHUD").getBuildDateString()+"</a><br>"+
			       "&gt;&nbsp;<a href=\"/GPHUD/ChangeLog\" target=\"_new\">"+SL.getModule("GPHUD").commitId()+"</a>";
		} else {
			return "&gt;&nbsp;<a href=\"/GPHUD/ChangeLog\" target=\"_new\">"+SL.getModule("GPHUD").getBuildDateString()+
			       "</a><br>"+"&gt;&nbsp;<a href=\"/GPHUD/ChangeLog\" target=\"_new\">"+
			       SL.getModule("GPHUD").commitId()+"</a><br>"+
			       "&gt;&nbsp;<a href=\"https://sl.coagulate.net/landingpage\">(C) Coagulate SL</a><br>"+
			       "&gt;&nbsp;<b>Operated by:</b><br>"+"&gt;&nbsp;"+Config.getBrandingName().replaceAll(" ","&nbsp;")+
			       "<br>"+"&gt;&nbsp;"+Config.getBrandingOwnerHumanReadable().replaceAll(" ","&nbsp;")+"<br>";
			
		}
	}
	
	/**
	 * Returns an SL formatted string regarding the server build
	 *
	 * @return Said string
	 */
	@Nonnull
	public static String serverVersion() {
		return "[https://"+Config.getURLHost()+"/GPHUD/ChangeLog "+SL.getModule("GPHUD").getBuildDateString()+" @"+
		       SL.getModule("GPHUD").commitId()+"] (C) secondlife:///app/agent/"+Config.getCreatorUUID()+"/about";
	}
	
	/**
	 * Get the main GPHUD Logger
	 *
	 * @return The logger
	 */
	@Nonnull
	public static Logger getLogger() {
		return log();
	}
	
	// ---------- STATICS ----------
	
	/**
	 * Get a generic logger if available, otherwise exception.
	 *
	 * @return The GPHUD logger
	 */
	@Nonnull
	public static Logger log() {
		if (log==null) {
			throw new SystemInitialisationException("Log not yet initialised");
		}
		return log;
	}
	
	@Nullable
	@Override
	public Map<ServiceTile,Integer> getServices() {
		final HashMap<ServiceTile,Integer> map=new HashMap<>();
		map.put(new ServiceTile("GPHUD",
		                        "Second generation role-play HUD - used to implement attribute/dice based RP environments",
		                        "/GPHUD/",
		                        "/resources/serviceicon-gphud.png",
		                        commitId(),
		                        getBuildDateString()),10);
		return map;
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
	public void shutdown() {
	}
	
	@Override
	public boolean initialise() {
		GPHUD.log=Logger.getLogger("net.coagulate.GPHUD");
		
		// Initialise the Database layer
		GPHUD.db=new MySqlDBConnection("GPHUD"+(Config.getDevelopment()?"DEV":""),Config.getGPHUDJdbc());
		schemaCheck(GPHUD.db,"gphud",SCHEMA_VERSION);
		
		// Annotation parser
		Classes.initialise();
		
		// tracing, if we're tracing
		// turn on path tracing AFTER initialisation as initialisation may update the schema from the module directly
		if (Config.getDatabasePathTracing()) {
			GPHUD.log().config("Database calling path verification is enabled for GPHUD");
			GPHUD.db.permit("net.coagulate.GPHUD.Data");
		}
		return true;
	}
	
	@Override
	public void maintenance() {
		if (nextRun("GPHUD-Maintenance",FAST_MAINT_INTERVAL,MAINT_VAR_SMALL)) {
			Maintenance.gphudMaintenance();
		}
		if (nextRun("GPHUD-Maintenance-Report-Quota",SLOW_MAINT_INTERVAL,MAINT_VAR_LARGE)) {
			Maintenance.quotaCredits();
		}
		if (nextRun("GPHUD-Instance-Cleanup",SLOW_MAINT_INTERVAL,MAINT_VAR_LARGE)) {
			Maintenance.instanceCleanup();
		}
		if (nextRun("GPHUD-Truncate-Logs",DAY_MAINT_INTERVAL,MAINT_VAR_DAY_LARGE)) {
			Maintenance.truncateLogs();
		}
	}
	
	@Override
	public void maintenanceInternal() {
	}
	
	public final String commitId() {
		return GPHUDBuildInfo.COMMITID;
	}
	
	public Date getBuildDate() {
		return GPHUDBuildInfo.BUILDDATE;
	}
	
	private static final int SCHEMA_VERSION=19;

	@SuppressWarnings("MagicNumber")
	@Override
	protected int schemaUpgrade(final DBConnection db,final String schemaName,int currentVersion) {
		// CHANGE SCHEMA CHECK CALL IN INITIALISE()
		final Logger log=GPHUD.getLogger("SchemaUpgrade");
		if (currentVersion==1) {
			log.config("Schema for GPHUD is at version 1, upgrading to version 2");
			log.config("Add URL index to characters table");
			GPHUD.getDB().d("ALTER TABLE characters ADD INDEX characters_url_index (url)");
			log.config("Add URL index to regions table");
			GPHUD.getDB().d("ALTER TABLE regions ADD INDEX regionss_url_index (url)");
			log.config("Schema upgrade of GPHUD to version 2 is complete");
			currentVersion=2;
		}
		if (currentVersion==2) {
			log.config("Add protocol column to characters table");
			GPHUD.getDB().d("ALTER TABLE `characters` ADD COLUMN `protocol` INT NOT NULL DEFAULT 0 AFTER `regionid`");
			log.config("Add protocol column to regions table");
			GPHUD.getDB().d("ALTER TABLE `regions` ADD COLUMN `protocol` INT NOT NULL DEFAULT 0 AFTER `primuuid`");
			log.config("Add protocol column to objects table");
			GPHUD.getDB().d("ALTER TABLE `objects` ADD COLUMN `protocol` INT NOT NULL DEFAULT 0 AFTER `authnode`");
			log.config("Schema upgrade of GPHUD to version 3 is complete");
			currentVersion=3;
		}
		if (currentVersion==3) {
			log.config("Expand audit columns");
			GPHUD.getDB()
			     .d("ALTER TABLE `audit` CHANGE COLUMN `changetype` `changetype` VARCHAR(128) NULL DEFAULT NULL");
			GPHUD.getDB()
			     .d("ALTER TABLE `audit` CHANGE COLUMN `changeditem` `changeditem` VARCHAR(128) NULL DEFAULT NULL");
			log.config("Schema upgrade of GPHUD to version 4 is complete");
			currentVersion=4;
		}
		if (currentVersion==4) {
			log.config("Add kvprecedence column to charactergroups table");
			GPHUD.getDB()
			     .d("ALTER TABLE `charactergroups` ADD COLUMN `kvprecedence` INT(11) NOT NULL DEFAULT 1 AFTER `owner`");
			log.config("Schema upgrade of GPHUD to version 5 is complete");
			currentVersion=5;
		}
		if (currentVersion==5) {
			log.config("Create table charactersets");
			GPHUD.getDB()
			     .d("CREATE TABLE `charactersets` ("+"	`id` INT NOT NULL AUTO_INCREMENT,"+
			        "	`characterid` INT NOT NULL,"+"	`attributeid` INT NOT NULL,"+
			        "	`element` VARCHAR(128) NOT NULL,"+"	`qty` INT NOT NULL DEFAULT 1,"+
			        "	PRIMARY KEY (`id`),"+"	UNIQUE INDEX `id_UNIQUE` (`id` ASC),"+
			        "	INDEX `characterid_index` (`characterid` ASC),"+
			        "	INDEX `attributeid_index` (`attributeid` ASC),"+"	INDEX `element` (`element` ASC),"+
			        "	CONSTRAINT `characterid_fk`"+"		FOREIGN KEY (`characterid`)"+
			        "		REFERENCES `characters` (`characterid`)"+"		ON DELETE CASCADE"+
			        "		ON UPDATE RESTRICT,"+"	CONSTRAINT `attributeid_fk`"+
			        "		FOREIGN KEY (`attributeid`)"+"		REFERENCES `attributes` (`attributeid`)"+
			        "		ON DELETE CASCADE"+"		ON UPDATE RESTRICT"+")");
			GPHUD.getDB()
			     .d("ALTER TABLE `charactersets` ADD UNIQUE INDEX `charactersets_composite` (`characterid` ASC, `attributeid` ASC, `element` ASC)");
			log.config("Add SET type to attribute attributetype");
			GPHUD.getDB()
			     .d("ALTER TABLE `attributes` CHANGE COLUMN `attributetype` `attributetype` ENUM('INTEGER', 'FLOAT', 'GROUP', 'TEXT', 'COLOR', 'EXPERIENCE', 'CURRENCY', 'SET') NOT NULL");
			log.config("Schema upgrade of GPHUD to version 6 is complete");
			currentVersion=6;
		}
		if (currentVersion==6) {
			log.config("Add various items/inventory tables");
			
			GPHUD.getDB()
			     .d("CREATE TABLE `items` ("+"  `id` INT NOT NULL AUTO_INCREMENT,"+"  `instanceid` INT NOT NULL,"+
			        "  `name` VARCHAR(128) NOT NULL,"+"  `description` VARCHAR(256) NOT NULL DEFAULT '',"+
			        "  `weight` INT NOT NULL DEFAULT 0,"+"  `tradable` TINYINT NOT NULL DEFAULT 1,"+
			        "  `destroyable` TINYINT NOT NULL DEFAULT 1,"+"  PRIMARY KEY (`id`),"+
			        "  UNIQUE INDEX `id_UNIQUE` (`id` ASC),"+
			        "  UNIQUE INDEX `instanceid_itemname_unique` (`instanceid` ASC, `name` ASC),"+
			        "  INDEX `itemname_index` (`name` ASC),"+"  CONSTRAINT `items_instanceid_fk`"+
			        "    FOREIGN KEY (`instanceid`)"+"    REFERENCES `instances` (`instanceid`)"+
			        "    ON DELETE CASCADE"+"    ON UPDATE RESTRICT)");
			GPHUD.getDB()
			     .d("CREATE TABLE `iteminventories` ("+"  `itemid` INT NOT NULL,"+"  `inventoryid` INT NOT NULL,"+
			        "  `permitted` TINYINT NOT NULL DEFAULT 1,"+"  PRIMARY KEY (`itemid`, `inventoryid`),"+
			        "  INDEX `itemid_index` (`itemid` ASC),"+
			        "  INDEX `iteminventories_inventoryid_idx` (`inventoryid` ASC),"+
			        "  CONSTRAINT `iteminventories_inventoryid`"+"    FOREIGN KEY (`inventoryid`)"+
			        "    REFERENCES `attributes` (`attributeid`)"+"    ON DELETE CASCADE"+"    ON UPDATE RESTRICT,"+
			        "  CONSTRAINT `iteminventories_itemid`"+"    FOREIGN KEY (`itemid`)"+
			        "    REFERENCES `items` (`id`)"+"    ON DELETE CASCADE"+"    ON UPDATE RESTRICT)");
			GPHUD.getDB()
			     .d("CREATE TABLE `itemverbs` ("+"  `id` INT NOT NULL AUTO_INCREMENT,"+"  `itemid` INT NOT NULL,"+
			        "  `verb` VARCHAR(64) NOT NULL,"+"  `description` VARCHAR(256) NOT NULL DEFAULT '',"+
			        "  `payload` VARCHAR(4096) NOT NULL DEFAULT '{}',"+"  PRIMARY KEY (`id`),"+
			        "  UNIQUE INDEX `id_UNIQUE` (`id` ASC),"+"  INDEX `itemverbs_itemid` (`itemid` ASC),"+
			        "  INDEX `itemverbs_verb` (`verb` ASC),"+
			        "  UNIQUE INDEX `itemid_itemverbs_unique` (`itemid` ASC, `verb` ASC),"+
			        "  CONSTRAINT `itemverbs_itemid_fk`"+"    FOREIGN KEY (`itemid`)"+"    REFERENCES `items` (`id`)"+
			        "    ON DELETE CASCADE"+"    ON UPDATE RESTRICT)");
			GPHUD.getDB()
			     .d("ALTER TABLE `attributes` CHANGE COLUMN `attributetype` `attributetype` ENUM('INTEGER', 'FLOAT', 'GROUP', 'TEXT', 'COLOR', 'EXPERIENCE', 'CURRENCY', 'SET', 'INVENTORY') NOT NULL");
			log.config("Schema upgrade of GPHUD to version 7 is complete");
			currentVersion=7;
		}
		if (currentVersion==7) {
			log.config("Add report and quota to instances table");
			GPHUD.getDB()
			     .d("ALTER TABLE `instances` "+"ADD COLUMN `reportquota` INT NULL DEFAULT 0,"+
			        "ADD COLUMN `downloadquota` INT NULL DEFAULT 0,"+
			        "ADD COLUMN `nextquotaincrease` INT NULL DEFAULT 0,"+"ADD COLUMN `report` TEXT NULL,"+
			        "ADD COLUMN `reporttds` INT NULL DEFAULT 0");
			
			
			log.config("Schema upgrade of GPHUD to version 8 is complete");
			currentVersion=8;
		}
		if (currentVersion==8) {
			log.config("Add report generating marker to table");
			GPHUD.getDB().d("ALTER TABLE `instances` "+"ADD COLUMN `reporting` INT NULL DEFAULT 0");
			log.config("Schema upgrade of GPHUD to version 9 is complete");
			currentVersion=9;
		}
		if (currentVersion==9) {
			log.config("Increase size of report storage");
			GPHUD.getDB().d("ALTER TABLE `instances` CHANGE COLUMN `report` `report` LONGTEXT NULL DEFAULT NULL");
			log.config("Schema upgrade of GPHUD to version 10 is complete");
			currentVersion=10;
		}
		if (currentVersion==10) {
			log.config("Add metadata column to effects");
			GPHUD.getDB().d("ALTER TABLE `effects` ADD COLUMN `metadata` VARCHAR(1024) NOT NULL DEFAULT ''");
			log.config("Schema upgrade of GPHUD to version 11 is complete");
			currentVersion=11;
		}
		if (currentVersion==11) {
			log.config("Add compiler version column to scripts");
			GPHUD.getDB().d("ALTER TABLE `scripts` ADD COLUMN `compilerversion` INT NOT NULL DEFAULT '0'");
			log.config("Schema upgrade of GPHUD to version 12 is complete");
			currentVersion=12;
		}
		if (currentVersion==12) {
			log.config("Add alias to scripts");
			GPHUD.getDB().d("ALTER TABLE `scripts` ADD COLUMN `alias` VARCHAR(64) NULL DEFAULT NULL");
			log.config("Schema upgrade of GPHUD to version 13 is complete");
			currentVersion=13;
		}
		if (currentVersion==13) {
			log.config("Add script parameter map to scripts table");
			GPHUD.getDB()
			     .d("ALTER TABLE `scripts` ADD COLUMN `parameterlist` VARCHAR(4096) NULL DEFAULT NULL AFTER `compilerversion`");
			log.config("Schema upgrade of GPHUD to version 14 is complete");
			currentVersion=14;
		}
		if (currentVersion==14) {
			log.config("Add instance termination countdown to instances table");
			GPHUD.getDB().d("ALTER TABLE `instances` ADD COLUMN `retireat` INT DEFAULT NULL");
			GPHUD.getDB().d("ALTER TABLE `instances` ADD COLUMN `retirewarn` INT DEFAULT NULL");
			log.config("Schema upgrade of GPHUD to version 15 is complete");
			currentVersion=15;
		}
		if (currentVersion==15) {
			log.config("Add index to audit table for truncation purposes");
			GPHUD.getDB().d("ALTER TABLE audit ADD INDEX audit_timedate (timedate)");
			log.config("Schema upgrade of GPHUD to version 16 is complete");
			currentVersion=16;
		}
		if (currentVersion==16) {
			log.config("Add index to characters table for name resolution at instance");
			GPHUD.getDB().d("ALTER TABLE `characters` ADD UNIQUE INDEX `characters_instance_names` (`instanceid` ASC, `name` ASC)");
			log.config("Schema upgrade of GPHUD to version 17 is complete");
			currentVersion=17;
		}
		if (currentVersion==17) {
			log.config("Add index to characters+pools data");
			GPHUD.getDB().d("ALTER TABLE `characterpools` ADD INDEX `characterpools_character_pools` (`characterid` ASC, `poolname` ASC)");
			log.config("Schema upgrade of GPHUD to version 18 is complete");
			currentVersion=18;
		}
		if (currentVersion==18) {
			log.config("Drop some unused indexes");
			GPHUD.getDB().d("drop index charactergroupkvstore_k on charactergroupkvstore");
			GPHUD.getDB().d("drop index keyword_index on characterkvstore");
			GPHUD.getDB().d("drop index element on charactersets");
			GPHUD.getDB().d("drop index cookie_index on cookies");
			GPHUD.getDB().d("drop index eventvisits_awarded on eventvisits");
			GPHUD.getDB().d("drop index eventvisits_endtime on eventvisits");
			GPHUD.getDB().d("drop index eventvisits_starttime on eventvisits");
			GPHUD.getDB().d("drop index itemid_index on iteminventories");
			GPHUD.getDB().d("drop index itemname_index on items");
			GPHUD.getDB().d("drop index itemverbs_verb on itemverbs");
			GPHUD.getDB().d("drop index messages_expires on messages");
			GPHUD.getDB().d("drop index zonekvstore_key on zonekvstore");
			log.config("Schema upgrade of GPHUD to version 19 is complete");
			currentVersion=19;
		}
		return currentVersion;
		// UPDATE THE CURRENT VERSION NUMBER ABOVE THIS FUNC
	}
	
	@Override
	public void registerChanges() {
		for (final Class<?> c: ClassTools.getAnnotatedClasses(Classes.Change.class)) {
			for (final Classes.Change a: c.getAnnotationsByType(Classes.Change.class)) {
				ChangeLogging.add(new ChangeLogging.Change(a.date(),"GPHUD",a.component().name(),a.type(),a.message()));
			}
		}
		for (final Class<?> c: ClassTools.getAnnotatedClasses(Classes.Changes.class)) {
			for (final Classes.Changes as: c.getAnnotationsByType(Classes.Changes.class)) {
				for (final Classes.Change a: as.value()) {
					ChangeLogging.add(new ChangeLogging.Change(a.date(),
					                                           "GPHUD",
					                                           a.component().name(),
					                                           a.type(),
					                                           a.message()));
				}
			}
		}
	}
	
	/**
	 * Get a subspace of the GPHUD logger
	 *
	 * @param subspace Sub name
	 * @return The logger
	 */
	public static Logger getLogger(final String subspace) {
		return Logger.getLogger(log().getName()+"."+subspace);
	}
	
	
	@Override
	public void startup() {
		URLDistribution.register("/GPHUD/external",new net.coagulate.GPHUD.Interfaces.External.Interface());
		URLDistribution.register("/GPHUD/system",new net.coagulate.GPHUD.Interfaces.System.Interface());
		URLDistribution.register("/GPHUD/",new net.coagulate.GPHUD.Interfaces.User.Interface());
	}
	
	/**
	 * Get the GPHUD DB connection
	 *
	 * @return The DBConnection
	 */
	@Nonnull
	public static DBConnection getDB() {
		if (db==null) {
			throw new SystemInitialisationException("Calling DB before DB is initialised");
		}
		return db;
	}
	
	@Nullable
	@Override
	public Object weakInvoke(final String command,final Object... arguments) {
		if ("im".equalsIgnoreCase(command)) {
			if (!Config.getDistributionRegion().isBlank()) {
				final Region r=Region.findNullable(Config.getDistributionRegion(),false);
				if (r==null) {
					log().warning("Instant messaging services unavailable, Distribution Region badly configured");
					return null;
				}
				final JSONObject json=new JSONObject();
				json.put("instantmessage",arguments[0]);
				json.put("instantmessagemessage",arguments[1]);
				r.sendServerSync(json);
			}
		}
		if ("permit".equalsIgnoreCase(command)) {
			final User user=User.findUsername((String)arguments[0],false);
			user.setPreference("gphud","instancepermit",Integer.toString(UnixTime.getUnixTime()+(60*60*24*7)));
			final net.coagulate.GPHUD.State gpstate=new net.coagulate.GPHUD.State();
			gpstate.setAvatar(user);
			SL.im(user.getUUID(),"""
					===== GPHUD Information =====
					You have been issued a permit to create an installation of GPHUD.
					This permit expires in one week.
					
					You should have been given an item named GPHUD Region Server.
					Rez this on one of your regions and say into local chat
					**createinstance <shortname>
					Replacing <shortname> with a reasonable name for your installation.
					
					Once complete, allow 60 seconds for the device to start up and then
					click the server to be given a HUD.  From here the bottom right quick button
					will take you to the website for further configuration.""");
			Distribution.getServer(gpstate);
		}
		return null;
	}
	
	@Override
	public SelfTest.PassFailRecord selfTest() {
		return new TestFramework().execute();
	}
	
	@Override
	public void preLoadCaches() {
		Char.preLoadCache();
		Attribute.preLoadCache();
		PermissionsGroup.preLoadCache();
		getDB().resetSQLLogs();
	}
}
