/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `adminnotes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `adminnotes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tds` int(11) NOT NULL,
  `instanceid` int(11) NOT NULL,
  `adminid` int(11) NOT NULL,
  `targetuser` int(11) NOT NULL,
  `targetchar` int(11) DEFAULT NULL,
  `note` text NOT NULL,
  `adminonly` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `adminnotes_instanceid` (`instanceid`),
  KEY `adminnotes_adminid` (`adminid`),
  KEY `adminnotes_targetuser` (`targetuser`),
  KEY `adminnotes_targetchar` (`targetchar`),
  CONSTRAINT `adminnotes_adminid_fk` FOREIGN KEY (`adminid`) REFERENCES `sl`.`users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `adminnotes_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE,
  CONSTRAINT `adminnotes_targetchar_fk` FOREIGN KEY (`targetchar`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE,
  CONSTRAINT `adminnotes_targetuser_fk` FOREIGN KEY (`targetuser`) REFERENCES `sl`.`users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `aliases`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `aliases` (
  `aliasid` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `template` varchar(4096) NOT NULL,
  PRIMARY KEY (`aliasid`),
  UNIQUE KEY `aliasid_UNIQUE` (`aliasid`),
  KEY `aliases_instanceid_fk_idx` (`instanceid`),
  KEY `aliases_name` (`name`),
  CONSTRAINT `aliases_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2283 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attributes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `attributes` (
  `attributeid` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `selfmodify` tinyint(4) NOT NULL,
  `attributetype` enum('INTEGER','FLOAT','GROUP','TEXT','COLOR','EXPERIENCE','CURRENCY') NOT NULL,
  `grouptype` varchar(32) DEFAULT NULL,
  `usesabilitypoints` tinyint(4) NOT NULL,
  `required` tinyint(4) NOT NULL,
  `defaultvalue` varchar(1024) DEFAULT NULL,
  `templatable` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`attributeid`),
  UNIQUE KEY `attributeid_UNIQUE` (`attributeid`),
  KEY `attributes_instanceid_fk_idx` (`instanceid`),
  KEY `attributes_name` (`name`),
  CONSTRAINT `attributes_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2148 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `audit` (
  `timedate` int(11) DEFAULT NULL,
  `instanceid` int(11) DEFAULT NULL,
  `sourceavatarid` int(11) DEFAULT NULL,
  `sourcecharacterid` int(11) DEFAULT NULL,
  `destavatarid` int(11) DEFAULT NULL,
  `destcharacterid` int(11) DEFAULT NULL,
  `changetype` varchar(64) DEFAULT NULL,
  `changeditem` varchar(64) DEFAULT NULL,
  `oldvalue` varbinary(4096) DEFAULT NULL,
  `newvalue` varbinary(4096) DEFAULT NULL,
  `notes` varbinary(4096) DEFAULT NULL,
  `sourcename` varchar(256) DEFAULT NULL,
  `sourceowner` int(11) DEFAULT NULL,
  `sourcedeveloper` int(11) DEFAULT NULL,
  `sourceregion` int(11) DEFAULT NULL,
  `sourcelocation` varchar(64) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `instanceid_fk_idx` (`instanceid`),
  KEY `sourcecharacterid_fk_idx` (`sourcecharacterid`),
  KEY `destcharacterid_fk_idx` (`destcharacterid`),
  KEY `sourceregion_fk_idx` (`sourceregion`),
  KEY `audit_sourcedeveloper_fk_idx` (`sourcedeveloper`),
  KEY `audit_sourceowner_fk_idx` (`sourceowner`),
  KEY `audit_sourceavatarid_fk_idx` (`sourceavatarid`),
  KEY `audit_destavatarid_fk_idx` (`destavatarid`),
  KEY `audit_tds` (`instanceid`,`timedate`),
  CONSTRAINT `audit_destavatarid_fk` FOREIGN KEY (`destavatarid`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `audit_destcharacterid_fk` FOREIGN KEY (`destcharacterid`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE,
  CONSTRAINT `audit_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE,
  CONSTRAINT `audit_sourceavatarid_fk` FOREIGN KEY (`sourceavatarid`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `audit_sourcecharacterid_fk` FOREIGN KEY (`sourcecharacterid`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE,
  CONSTRAINT `audit_sourcedeveloper_fk` FOREIGN KEY (`sourcedeveloper`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `audit_sourceowner_fk` FOREIGN KEY (`sourceowner`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `audit_sourceregion_fk` FOREIGN KEY (`sourceregion`) REFERENCES `regions` (`regionid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=514768 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `charactergroupkvstore`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `charactergroupkvstore` (
  `charactergroupid` int(11) NOT NULL,
  `k` varchar(128) NOT NULL,
  `v` varbinary(4096) NOT NULL,
  PRIMARY KEY (`charactergroupid`,`k`),
  KEY `charactergroupkvstore_charactergroupid` (`charactergroupid`),
  KEY `charactergroupkvstore_k` (`k`),
  CONSTRAINT `charactergroupkvstore_fk_charactergroupid` FOREIGN KEY (`charactergroupid`) REFERENCES `charactergroups` (`charactergroupid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `charactergroupmembers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `charactergroupmembers` (
  `charactergroupid` int(11) NOT NULL,
  `characterid` int(11) NOT NULL,
  `isadmin` tinyint(4) NOT NULL DEFAULT '0',
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `entitygroupid_index` (`charactergroupid`),
  KEY `entityid_index` (`characterid`),
  CONSTRAINT `charactergroupmembers_entitygroupid_fk` FOREIGN KEY (`charactergroupid`) REFERENCES `charactergroups` (`charactergroupid`) ON DELETE CASCADE,
  CONSTRAINT `charactergroupmembers_entityid_fk` FOREIGN KEY (`characterid`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=49278 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `charactergroups`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `charactergroups` (
  `charactergroupid` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(128) NOT NULL,
  `open` tinyint(4) NOT NULL DEFAULT '0',
  `type` varchar(128) DEFAULT NULL,
  `owner` int(11) DEFAULT NULL,
  PRIMARY KEY (`charactergroupid`),
  KEY `instanceid_fk_idx` (`instanceid`),
  KEY `type_index` (`type`),
  KEY `charactergroups_owner_idx` (`owner`),
  CONSTRAINT `charactergroups_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE,
  CONSTRAINT `charactergroups_owner` FOREIGN KEY (`owner`) REFERENCES `characters` (`characterid`)
) ENGINE=InnoDB AUTO_INCREMENT=3973 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `characterkvstore`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `characterkvstore` (
  `characterid` int(11) NOT NULL,
  `k` varchar(128) NOT NULL,
  `v` varbinary(4096) NOT NULL,
  PRIMARY KEY (`characterid`,`k`),
  KEY `entityid_index` (`characterid`),
  KEY `keyword_index` (`k`),
  CONSTRAINT `characterkvstore_entityid_fk` FOREIGN KEY (`characterid`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `characterpools`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `characterpools` (
  `characterid` int(11) NOT NULL,
  `poolname` varchar(128) DEFAULT NULL,
  `adjustment` int(11) NOT NULL,
  `adjustedbycharacter` int(11) DEFAULT NULL,
  `adjustedbyavatar` int(11) DEFAULT NULL,
  `description` varchar(1024) NOT NULL,
  `timedate` int(11) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `characterid_index` (`characterid`),
  KEY `poolname_index` (`poolname`),
  KEY `adjustedby_fk_idx` (`adjustedbycharacter`),
  KEY `timedate_index` (`timedate`),
  KEY `characterpools_adjustedbyavatar_fk_idx` (`adjustedbyavatar`),
  CONSTRAINT `characterpools_adjustedby_fk` FOREIGN KEY (`adjustedbycharacter`) REFERENCES `characters` (`characterid`) ON DELETE SET NULL,
  CONSTRAINT `characterpools_adjustedbyavatar_fk` FOREIGN KEY (`adjustedbyavatar`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `characterpools_characterid_fk` FOREIGN KEY (`characterid`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=83593 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `characters`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `characters` (
  `characterid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `instanceid` int(11) NOT NULL,
  `owner` int(11) NOT NULL,
  `playedby` int(11) DEFAULT NULL,
  `lastactive` int(11) NOT NULL,
  `retired` tinyint(4) NOT NULL,
  `url` varchar(256) DEFAULT NULL,
  `urlfirst` int(11) DEFAULT NULL,
  `urllast` int(11) DEFAULT NULL,
  `authnode` varchar(128) DEFAULT NULL,
  `zoneid` int(11) DEFAULT NULL,
  `regionid` int(11) DEFAULT NULL,
  PRIMARY KEY (`characterid`),
  KEY `instance_index` (`instanceid`),
  KEY `authnode_index` (`authnode`),
  KEY `characters_zoneid_fk_idx` (`zoneid`),
  KEY `characters_regionid_fk` (`regionid`),
  KEY `characters_playedby_fk_idx` (`playedby`),
  KEY `characters_owner_fk_idx` (`owner`),
  KEY `characters_url_index` (`url`),
  CONSTRAINT `characters_instance_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE,
  CONSTRAINT `characters_owner_fk` FOREIGN KEY (`owner`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `characters_playedby_fk` FOREIGN KEY (`playedby`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `characters_regionid_fk` FOREIGN KEY (`regionid`) REFERENCES `regions` (`regionid`) ON DELETE SET NULL,
  CONSTRAINT `characters_zoneid_fk` FOREIGN KEY (`zoneid`) REFERENCES `zones` (`zoneid`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=33748 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cookies`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cookies` (
  `cookie` varchar(64) NOT NULL,
  `expires` int(11) NOT NULL,
  `renewable` tinyint(4) DEFAULT '1',
  `avatarid` int(11) DEFAULT NULL,
  `characterid` int(11) DEFAULT NULL,
  `instanceid` int(11) DEFAULT NULL,
  PRIMARY KEY (`cookie`),
  KEY `cookie_index` (`cookie`),
  KEY `entityid_fk_idx` (`characterid`),
  KEY `cookies_instanceid_fk_idx` (`instanceid`),
  KEY `cookies_avatarid_fk_idx` (`avatarid`),
  CONSTRAINT `cookies_avatarid_fk` FOREIGN KEY (`avatarid`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `cookies_entityid_fk` FOREIGN KEY (`characterid`) REFERENCES `characters` (`characterid`) ON DELETE SET NULL,
  CONSTRAINT `cookies_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `currencies`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `currencies` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `basecoin` varchar(32) NOT NULL,
  `basecoinshort` varchar(16) NOT NULL,
  `tradable` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `currencies_instance_index` (`instanceid`),
  KEY `currencies_name_index` (`name`),
  CONSTRAINT `currencies_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `currencycoins`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `currencycoins` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `currencyid` int(11) NOT NULL,
  `coinname` varchar(64) NOT NULL,
  `coinnameshort` varchar(32) NOT NULL,
  `basemultiple` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `currencycoins_currencyid_fk_idx` (`currencyid`),
  CONSTRAINT `currencycoins_currencyid_fk` FOREIGN KEY (`currencyid`) REFERENCES `currencies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `effects`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `effects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `effects_instanceid` (`instanceid`),
  CONSTRAINT `effects_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=323 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `effectsapplications`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `effectsapplications` (
  `effectid` int(11) NOT NULL,
  `characterid` int(11) NOT NULL,
  `expires` int(11) DEFAULT NULL,
  PRIMARY KEY (`effectid`,`characterid`),
  KEY `effectsapplications_characterid_fk_idx` (`characterid`),
  CONSTRAINT `effectsapplications_characterid_fk` FOREIGN KEY (`characterid`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE,
  CONSTRAINT `effectsapplications_effectid_fk` FOREIGN KEY (`effectid`) REFERENCES `effects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `effectskvstore`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `effectskvstore` (
  `effectid` int(11) NOT NULL,
  `k` varchar(128) NOT NULL,
  `v` varbinary(4096) NOT NULL,
  PRIMARY KEY (`effectid`,`k`),
  CONSTRAINT `effectskvstore_effectid_fk` FOREIGN KEY (`effectid`) REFERENCES `effects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `events`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `events` (
  `eventid` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(128) NOT NULL,
  PRIMARY KEY (`eventid`),
  UNIQUE KEY `eventid_UNIQUE` (`eventid`),
  KEY `events_instanceid_fk_idx` (`instanceid`),
  CONSTRAINT `events_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=68 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `eventskvstore`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `eventskvstore` (
  `eventid` int(11) NOT NULL,
  `k` varchar(128) NOT NULL,
  `v` varbinary(4096) NOT NULL,
  PRIMARY KEY (`eventid`,`k`),
  CONSTRAINT `eventskvstore_eventid_fk` FOREIGN KEY (`eventid`) REFERENCES `events` (`eventid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `eventslocations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `eventslocations` (
  `eventlocationid` int(11) NOT NULL AUTO_INCREMENT,
  `eventid` int(11) NOT NULL,
  `zoneid` int(11) NOT NULL,
  PRIMARY KEY (`eventlocationid`),
  UNIQUE KEY `eventlocationsid_UNIQUE` (`eventlocationid`),
  KEY `eventslocations_eventid_fk_idx` (`eventid`),
  KEY `eventslocations_zoneid_fk_idx` (`zoneid`),
  CONSTRAINT `eventslocations_eventid_fk` FOREIGN KEY (`eventid`) REFERENCES `events` (`eventid`) ON DELETE CASCADE,
  CONSTRAINT `eventslocations_zoneid_fk` FOREIGN KEY (`zoneid`) REFERENCES `zones` (`zoneid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=88 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `eventsschedule`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `eventsschedule` (
  `eventsscheduleid` int(11) NOT NULL AUTO_INCREMENT,
  `eventid` int(11) NOT NULL,
  `starttime` int(11) NOT NULL,
  `endtime` int(11) NOT NULL,
  `started` tinyint(4) NOT NULL DEFAULT '0',
  `repeatinterval` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`eventsscheduleid`),
  UNIQUE KEY `eventsscheduleid_UNIQUE` (`eventsscheduleid`),
  KEY `eventsschedule_eventid_fk_idx` (`eventid`),
  CONSTRAINT `eventsschedule_eventid_fk` FOREIGN KEY (`eventid`) REFERENCES `events` (`eventid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=158 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `eventvisits`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `eventvisits` (
  `characterid` int(11) NOT NULL,
  `eventscheduleid` int(11) NOT NULL,
  `starttime` int(11) NOT NULL,
  `endtime` int(11) DEFAULT NULL,
  `awarded` int(11) NOT NULL DEFAULT '0',
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `eventvisits_characterid_fk_idx` (`characterid`),
  KEY `eventvisits_starttime` (`starttime`),
  KEY `eventvisits_endtime` (`endtime`),
  KEY `eventvisits_awarded` (`awarded`),
  KEY `eventvisits_eventscheduleid` (`eventscheduleid`),
  CONSTRAINT `eventvisits_characterid_fk` FOREIGN KEY (`characterid`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE,
  CONSTRAINT `eventvisits_eventsschedule_fk` FOREIGN KEY (`eventscheduleid`) REFERENCES `eventsschedule` (`eventsscheduleid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2163 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `instancedevelopers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instancedevelopers` (
  `instanceid` int(11) NOT NULL,
  `developerid` int(11) NOT NULL,
  `queries` int(11) NOT NULL DEFAULT '0',
  `bytes` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`instanceid`,`developerid`),
  KEY `instancedevelopers_developerid_fk_idx` (`developerid`),
  CONSTRAINT `instancedevelopers_developerid_fk` FOREIGN KEY (`developerid`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `instancedevelopers_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `instancekvstore`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instancekvstore` (
  `instanceid` int(11) NOT NULL,
  `k` varchar(128) NOT NULL,
  `v` varbinary(4096) NOT NULL,
  PRIMARY KEY (`instanceid`,`k`),
  KEY `indexid_instance` (`instanceid`),
  CONSTRAINT `instancekvstore_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `instances`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instances` (
  `instanceid` int(11) NOT NULL AUTO_INCREMENT,
  `owner` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`instanceid`),
  KEY `instances_owner_fk_idx` (`owner`),
  CONSTRAINT `instances_owner_fk` FOREIGN KEY (`owner`) REFERENCES `sl`.`users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=193 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `landmarks`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `landmarks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `regionid` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `x` float NOT NULL,
  `y` float NOT NULL,
  `z` float NOT NULL,
  `lookatx` float NOT NULL,
  `lookaty` float NOT NULL,
  `lookatz` float NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `landmarks_regionid` (`regionid`),
  KEY `landmarks_name` (`name`),
  CONSTRAINT `landmarks_regionid_fk` FOREIGN KEY (`regionid`) REFERENCES `regions` (`regionid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=253 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `menus`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `menus` (
  `menuid` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `description` varchar(256) NOT NULL,
  `json` varchar(4096) NOT NULL,
  PRIMARY KEY (`menuid`),
  KEY `menus_instanceid_fk_idx` (`instanceid`),
  KEY `menus_name_index` (`name`),
  CONSTRAINT `menus_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1128 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `messages`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `messages` (
  `messageid` int(11) NOT NULL AUTO_INCREMENT,
  `characterid` int(11) NOT NULL,
  `expires` int(11) NOT NULL,
  `json` varchar(4096) NOT NULL,
  PRIMARY KEY (`messageid`),
  KEY `messages_characterid` (`characterid`),
  KEY `messages_expires` (`expires`),
  CONSTRAINT `messages_characterid` FOREIGN KEY (`characterid`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1293 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `objects`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `objects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(45) NOT NULL,
  `name` varchar(64) NOT NULL,
  `regionid` int(11) NOT NULL,
  `owner` int(11) NOT NULL,
  `location` varchar(40) NOT NULL,
  `lastrx` int(11) NOT NULL,
  `objecttype` int(11) DEFAULT NULL,
  `url` varchar(128) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  `authnode` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid_UNIQUE` (`uuid`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `objects_regionid_fk_idx` (`regionid`),
  KEY `objects_owner_fk_idx` (`owner`),
  CONSTRAINT `objects_owner_fk` FOREIGN KEY (`owner`) REFERENCES `sl`.`users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `objects_regionid_fk` FOREIGN KEY (`regionid`) REFERENCES `regions` (`regionid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=353 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `objecttypes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `objecttypes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `behaviour` varchar(4096) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `objecttypes_instanceid_fk_idx` (`instanceid`),
  CONSTRAINT `objecttypes_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=328 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permissions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `permissions` (
  `permissionsgroupid` int(11) NOT NULL,
  `permission` varchar(64) NOT NULL,
  PRIMARY KEY (`permissionsgroupid`,`permission`),
  KEY `permissionsgroupid_index` (`permissionsgroupid`),
  CONSTRAINT `permissions_permissionsgroup_fk` FOREIGN KEY (`permissionsgroupid`) REFERENCES `permissionsgroups` (`permissionsgroupid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permissionsgroupmembers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `permissionsgroupmembers` (
  `permissionsgroupid` int(11) NOT NULL,
  `avatarid` int(11) NOT NULL,
  `caninvite` tinyint(4) NOT NULL DEFAULT '0',
  `cankick` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`permissionsgroupid`,`avatarid`),
  KEY `permissionsgroupid_index` (`permissionsgroupid`),
  KEY `permissionsgroupmembers_avatar_fk_idx` (`avatarid`),
  CONSTRAINT `permissionsgroupmembers_avatar_fk` FOREIGN KEY (`avatarid`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `permissionsgroupmembers_permissionsgroup_fk` FOREIGN KEY (`permissionsgroupid`) REFERENCES `permissionsgroups` (`permissionsgroupid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permissionsgroups`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `permissionsgroups` (
  `permissionsgroupid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `instanceid` int(11) NOT NULL,
  PRIMARY KEY (`permissionsgroupid`),
  KEY `instanceid_index` (`instanceid`),
  CONSTRAINT `permissionsgroups_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=293 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ping`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ping` (
  `ping` int(11) NOT NULL,
  PRIMARY KEY (`ping`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `primarycharacters`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `primarycharacters` (
  `avatarid` int(11) NOT NULL,
  `instanceid` int(11) NOT NULL,
  `entityid` int(11) NOT NULL,
  PRIMARY KEY (`avatarid`,`instanceid`),
  KEY `avatarid_index` (`avatarid`),
  KEY `instanceid_index` (`instanceid`),
  KEY `entityid_index` (`entityid`),
  CONSTRAINT `primarycharacters_avatarid_fk` FOREIGN KEY (`avatarid`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `primarycharacters_entityid_fk` FOREIGN KEY (`entityid`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE,
  CONSTRAINT `primarycharacters_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `regionkvstore`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `regionkvstore` (
  `regionid` int(11) NOT NULL,
  `k` varchar(128) NOT NULL,
  `v` varbinary(4096) NOT NULL,
  PRIMARY KEY (`k`,`regionid`),
  KEY `regionid_index` (`regionid`),
  CONSTRAINT `regionkvstore_regionid_fk` FOREIGN KEY (`regionid`) REFERENCES `regions` (`regionid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `regions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `regions` (
  `regionid` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  `urllast` int(11) DEFAULT NULL,
  `authnode` varchar(128) DEFAULT NULL,
  `regionserverversion` int(11) DEFAULT NULL,
  `regionserverdatetime` int(11) DEFAULT NULL,
  `regionhudversion` int(11) DEFAULT NULL,
  `regionhuddatetime` int(11) DEFAULT NULL,
  `regionx` int(11) DEFAULT NULL,
  `regiony` int(11) DEFAULT NULL,
  `retired` tinyint(4) NOT NULL DEFAULT '0',
  `primuuid` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`regionid`),
  UNIQUE KEY `name_UNIQUE` (`name`),
  KEY `instance_index` (`instanceid`),
  CONSTRAINT `regions_instance_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=228 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schemaversions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `schemaversions` (
  `name` varchar(64) NOT NULL,
  `version` int(11) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scriptruns`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scriptruns` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bytecode` mediumblob NOT NULL,
  `initialiser` mediumblob NOT NULL,
  `respondant` int(11) NOT NULL,
  `expires` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `respondant_index` (`respondant`),
  CONSTRAINT `scriptruns_respondant_fk` FOREIGN KEY (`respondant`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=60898 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scripts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scripts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `source` mediumtext,
  `sourceversion` int(11) DEFAULT NULL,
  `bytecode` mediumblob,
  `bytecodeversion` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `scripts_instanceid` (`instanceid`),
  KEY `scripts_name` (`name`),
  CONSTRAINT `scripts_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=548 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `visits`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `visits` (
  `avatarid` int(11) NOT NULL,
  `characterid` int(11) NOT NULL,
  `regionid` int(11) DEFAULT NULL,
  `starttime` int(11) NOT NULL,
  `endtime` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `characterid_fk_idx` (`characterid`),
  KEY `regionid_fk_idx` (`regionid`),
  KEY `visits_avatarid_fk_idx` (`avatarid`),
  KEY `endime_index` (`characterid,`endtime`),
  KEY `endtime_region_index` (`regionid`,`endtime`),
  KEY `visits_noendtime_avatar` (`endtime`,`avatarid`),
  CONSTRAINT `visits_avatarid_fk` FOREIGN KEY (`avatarid`) REFERENCES `sl`.`users` (`id`),
  CONSTRAINT `visits_characterid_fk` FOREIGN KEY (`characterid`) REFERENCES `characters` (`characterid`) ON DELETE CASCADE,
  CONSTRAINT `visits_regionid_fk` FOREIGN KEY (`regionid`) REFERENCES `regions` (`regionid`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=528888 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zoneareas`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zoneareas` (
  `zoneareaid` int(11) NOT NULL AUTO_INCREMENT,
  `zoneid` int(11) NOT NULL,
  `regionid` int(11) NOT NULL,
  `x1` int(11) DEFAULT NULL,
  `y1` int(11) DEFAULT NULL,
  `z1` int(11) DEFAULT NULL,
  `x2` int(11) DEFAULT NULL,
  `y2` int(11) DEFAULT NULL,
  `z2` int(11) DEFAULT NULL,
  PRIMARY KEY (`zoneareaid`),
  KEY `zoneareas_regionid_fk_idx` (`regionid`),
  KEY `zoneareas_zoneid_fk_idx` (`zoneid`),
  CONSTRAINT `zoneareas_regionid_fk` FOREIGN KEY (`regionid`) REFERENCES `regions` (`regionid`) ON DELETE CASCADE,
  CONSTRAINT `zoneareas_zoneid_fk` FOREIGN KEY (`zoneid`) REFERENCES `zones` (`zoneid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=173 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zonekvstore`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zonekvstore` (
  `zoneid` int(11) NOT NULL,
  `k` varchar(128) NOT NULL,
  `v` varbinary(4096) NOT NULL,
  PRIMARY KEY (`zoneid`,`k`),
  KEY `zonekvstore_zoneid` (`zoneid`),
  KEY `zonekvstore_key` (`k`),
  CONSTRAINT `zonekvstore_zoneid_fk` FOREIGN KEY (`zoneid`) REFERENCES `zones` (`zoneid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zones`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zones` (
  `zoneid` int(11) NOT NULL AUTO_INCREMENT,
  `instanceid` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`zoneid`),
  UNIQUE KEY `zoneid_UNIQUE` (`zoneid`),
  KEY `zones_name` (`name`),
  KEY `zones_instanceid` (`instanceid`),
  CONSTRAINT `zones_instanceid_fk` FOREIGN KEY (`instanceid`) REFERENCES `instances` (`instanceid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

insert into `schemaversions`(`name`,`version`) values('gphud',1);
