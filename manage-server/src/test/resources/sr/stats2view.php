#!/usr/bin/env php
<?php
require('config.php');

date_default_timezone_set('UTC');


function OpenDatabases($db)
{
	# open new database connection
	$dbh = new PDO( $db['dsn'], $db['user'], $db['password']);

	# throw exceptions is queries fail
	$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

	# set session settings correctly
	$dbh->query("SET NAMES 'utf8';");
	#$dbh->query("SET time_zone = 'Europe/Amsterdam';");
	$dbh->query("SET storage_engine=InnoDB;");
	$dbh->query("SET sql_mode='NO_AUTO_VALUE_ON_ZERO';");

	return $dbh;
}

function ClearView()
{
	global $CONFIG;
	/** @var PDO $dbh */
	$dbh = $CONFIG['dbh-view'];

	print "Clearing database";

	$q = "
		DROP VIEW IF EXISTS
			`statsview_v_idp`,
			`statsview_v_sp`,
			`statsview_v_connections`,
			`statsview_v_periodstats`,
			`statsview_v_periodstats_idptotal`,
			`statsview_v_periodstats_sptotal`
	";
	$dbh->query($q);


	$q = "
		DROP TABLE IF EXISTS
			`statsview_v_idp`,
			`statsview_v_sp`,
			`statsview_periodstats_idptotal`,
			`statsview_periodstats_sptotal`,
			`statsview_periodstats`,
			`statsview_period`,
			`statsview_idp-sp`,
			`statsview_sp`,
			`statsview_idp`,
			`statsview_institution`;
	";
	$dbh->query($q);

	$dbh->query("DROP FUNCTION IF EXISTS GetInstitutionId;");

	print "\n";
}

function CreateViewSchema()
{
	global $CONFIG;
	/** @var PDO $dbh */
	$dbh = $CONFIG['dbh-view'];

	print "Populating database";

	# table holds institution info from IDD/CRM
	$dbh->query("
		CREATE TABLE IF NOT EXISTS `statsview_institution` (
			`id`        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
			`abbr`      VARCHAR(32) NULL DEFAULT NULL,
			`guid`      CHAR(36) CHARACTER SET 'ascii' NULL DEFAULT NULL,
			`type`      VARCHAR(64) NULL DEFAULT NULL,
			`type_code` CHAR(8) CHARACTER SET 'ascii' NULL DEFAULT NULL,
			`name`      VARCHAR(256) NULL DEFAULT NULL,
			UNIQUE INDEX (`abbr`),
			UNIQUE INDEX (`guid`),
			       INDEX (`type`),
			       INDEX (`type_code`)
		) CHARACTER SET='utf8';
	");
	$dbh->query("
		INSERT INTO `statsview_institution`
			(`id`,`abbr`,`type`,`name`)
			VALUES (0,NULL,NULL,NULL);
	");
	$dbh->query("
		CREATE FUNCTION GetInstitutionId(institutionAbbr VARCHAR(32)) RETURNS INT UNSIGNED
		BEGIN
			DECLARE InstId INT UNSIGNED;
			DECLARE CONTINUE HANDLER FOR NOT FOUND BEGIN END;
			SELECT id INTO InstId
				FROM statsview_institution
				WHERE abbr = institutionAbbr;
			IF ISNULL(InstId) THEN RETURN 0; END IF;
			RETURN InstId;
		END
	");

	# table holds list of all IdPs
	$dbh->query("
		CREATE TABLE IF NOT EXISTS `statsview_idp` (
			`id`               INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
			`name`             VARCHAR(4096) NULL DEFAULT NULL,
			`entityid`         VARCHAR(4096) NULL DEFAULT NULL,
			`environment`      CHAR(2) NULL DEFAULT NULL,
			`institution_id`   INT UNSIGNED NOT NULL DEFAULT 0,
			`sr_active`        TINYINT(1) NULL DEFAULT NULL,
			`from`             TIMESTAMP NULL DEFAULT NULL,
			`to`               TIMESTAMP NULL DEFAULT NULL,
			UNIQUE INDEX (`entityid`(255),`environment`,`institution_id`),
			FOREIGN KEY  (`institution_id`) REFERENCES `statsview_institution` (`id`)
		) CHARACTER SET='utf8';
	");
	# trigger to make sure that we can't have two entries with identical env and entitid and NULL institution_id
	# as mysql doesn't take into account NULL values in uniqueness
	$dbh->query('
		CREATE TRIGGER statsview_idp_t1 BEFORE INSERT on statsview_idp
		FOR EACH ROW
		BEGIN
			IF ( ISNULL(NEW.institution_id) )
			THEN
				SET NEW.institution_id=0;
			END IF;
		END
	');
	$dbh->query('
		CREATE TRIGGER statsview_idp_t2 BEFORE UPDATE on statsview_idp
		FOR EACH ROW
		BEGIN
			IF ( ISNULL(NEW.institution_id) )
			THEN
				SET NEW.institution_id=0;
			END IF;
		END
	');

	# table holds list of all SPs
	$dbh->query("
		CREATE TABLE IF NOT EXISTS `statsview_sp` (
			`id`               INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
			`name`             VARCHAR(4096) NULL DEFAULT NULL,
			`entityid`         VARCHAR(4096) NULL DEFAULT NULL,
			`environment`      CHAR(2) NULL DEFAULT NULL,
			`institution_id`   INT UNSIGNED NOT NULL DEFAULT 0,
			`sr_active`        TINYINT(1) NULL DEFAULT NULL,
			`from`             TIMESTAMP NULL DEFAULT NULL,
			`to`               TIMESTAMP NULL DEFAULT NULL,
			UNIQUE INDEX (`entityid`(255),`environment`,`institution_id`),
			FOREIGN KEY  (`institution_id`) REFERENCES `statsview_institution` (`id`)
		) CHARACTER SET='utf8';
	");
	$dbh->query('
		CREATE TRIGGER statsview_sp_t1 BEFORE INSERT on statsview_sp
		FOR EACH ROW
		BEGIN
			IF ( ISNULL(NEW.institution_id) )
			THEN
				SET NEW.institution_id=0;
			END IF;
		END
	');
	$dbh->query('
		CREATE TRIGGER statsview_sp_t2 BEFORE UPDATE on statsview_sp
		FOR EACH ROW
		BEGIN
			IF ( ISNULL(NEW.institution_id) )
			THEN
				SET NEW.institution_id=0;
			END IF;
		END
	');

	# table holds list of all (effective) IdP-SP connections on certain date
	$dbh->query("
		CREATE TABLE IF NOT EXISTS `statsview_idp-sp` (
			`idp_id`   INT UNSIGNED NOT NULL,
			`sp_id`    INT UNSIGNED NOT NULL,
			`date`     DATE,
			UNIQUE INDEX (`idp_id`,`sp_id`,`date`),
			FOREIGN KEY (`idp_id`) REFERENCES `statsview_idp` (`id`) ON UPDATE CASCADE,
			FOREIGN KEY (`sp_id` ) REFERENCES `statsview_sp`  (`id`) ON UPDATE CASCADE
		) CHARACTER SET='utf8';
	");

	# table holds list of all known periods (weeks, months, quarters, years)
	# including total number of logins and unique users
	$dbh->query("
		CREATE TABLE `statsview_period` (
			`id`          INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
			`type`        CHAR(1) NOT NULL,
			`period`      INT(2) UNSIGNED NOT NULL,
			`year`        INT(4) UNSIGNED NOT NULL,
			`environment` CHAR(2) NOT NULL,
			`from`        TIMESTAMP NULL DEFAULT NULL,
			`to`          TIMESTAMP NULL DEFAULT NULL,
			`logins`      INT UNSIGNED NULL DEFAULT NULL,
			`users`       INT UNSIGNED NULL DEFAULT NULL,
			`created`     TIMESTAMP NULL DEFAULT NULL,
			`updated`     timestamp NULL DEFAULT NULL,
			UNIQUE KEY (`period`,`year`,`environment`,`type`),
			KEY (`period`,`year`),
			KEY (`type`),
			KEY (`environment`)
		) CHARACTER SET='utf8';
	");

	# table holds total number of logins and unique users for each
	# IdP-SP combination and period
	$dbh->query("
		CREATE TABLE `statsview_periodstats` (
			`period_id` INT UNSIGNED NOT NULL,
			`idp_id`    INT UNSIGNED NOT NULL,
			`sp_id`     INT UNSIGNED NOT NULL,
			`logins`    INT UNSIGNED NULL DEFAULT NULL,
			`users`     INT UNSIGNED NULL DEFAULT NULL,
			PRIMARY KEY (`period_id`,`idp_id`,`sp_id`),
			FOREIGN KEY (`period_id`) REFERENCES `statsview_period` (`id`),
			FOREIGN KEY (`idp_id`   ) REFERENCES `statsview_idp`    (`id`) ON UPDATE CASCADE,
			FOREIGN KEY (`sp_id`    ) REFERENCES `statsview_sp`     (`id`) ON UPDATE CASCADE
		) CHARACTER SET='utf8';
	");

	# table holds total number of logins and unique users for each
	# IdP and period
	$dbh->query("
		CREATE TABLE `statsview_periodstats_idptotal` (
			`period_id` INT UNSIGNED NOT NULL,
			`idp_id`    INT UNSIGNED NOT NULL,
			`logins`    INT UNSIGNED NULL DEFAULT NULL,
			`users`     INT UNSIGNED NULL DEFAULT NULL,
			PRIMARY KEY (`period_id`,`idp_id`),
			FOREIGN KEY (`period_id`) REFERENCES `statsview_period` (`id`),
			FOREIGN KEY (`idp_id`   ) REFERENCES `statsview_idp`    (`id`) ON UPDATE CASCADE
		) CHARACTER SET='utf8';
	");

	# table holds total number of logins and unique users for each
	# SP and period
	$dbh->query("
		CREATE TABLE `statsview_periodstats_sptotal` (
			`period_id` INT UNSIGNED NOT NULL,
			`sp_id`     INT UNSIGNED NOT NULL,
			`logins`    INT UNSIGNED NULL DEFAULT NULL,
			`users`     INT UNSIGNED NULL DEFAULT NULL,
			PRIMARY KEY (`period_id`,`sp_id`),
			FOREIGN KEY (`period_id`) REFERENCES `statsview_period` (`id`),
			FOREIGN KEY (`sp_id`    ) REFERENCES `statsview_sp`     (`id`) ON UPDATE CASCADE
		) CHARACTER SET='utf8';
	");

	# view combining idp and institution info
	$dbh->query("
		CREATE OR REPLACE VIEW `statsview_v_idp`
		AS
			SELECT
				a.id, a.name, a.entityid, a.environment,
				b.abbr AS 'inst_abbr',
				b.type AS 'inst_type',
				b.name AS 'inst_name'
			FROM `statsview_idp` AS `a`
			LEFT JOIN `statsview_institution` AS `b`
				ON a.institution_id = b.id;
	");

	# view combining sp and institution info
	$dbh->query("
		CREATE OR REPLACE VIEW `statsview_v_sp`
		AS
			SELECT
				a.id, a.name, a.entityid, a.environment,
				b.abbr AS 'inst_abbr',
				b.type AS 'inst_type',
				b.name AS 'inst_name'
			FROM `statsview_sp` AS `a`
			LEFT JOIN `statsview_institution` AS `b`
				ON a.institution_id = b.id;
	");

	# view combining connection info
	$dbh->query("
		CREATE OR REPLACE VIEW `statsview_v_connections`
		AS
			SELECT
				c.date,
				i.name AS idp_name, i.entityid AS idp_entityid, i.environment AS idp_env,
				ii.abbr AS idp_inst_abbr, ii.name AS idp_inst_name, ii.type AS idp_inst_type,
				s.name AS sp_name,  s.entityid AS sp_entityid,  s.environment AS sp_env,
				si.abbr AS sp_inst_abbr, si.name AS sp_inst_name, si.type AS sp_inst_type
			FROM `statsview_idp-sp` AS c
			LEFT JOIN statsview_idp AS i ON  c.idp_id=i.id
			LEFT JOIN statsview_sp  AS s ON  c.sp_id =s.id
			LEFT JOIN statsview_institution as ii ON ii.id=i.institution_id
			LEFT JOIN statsview_institution as si ON si.id=s.institution_id
	");

	$dbh->query("
		CREATE OR REPLACE VIEW `statsview_v_periodstats`
		AS
			SELECT
				ps.logins, ps.users,
				p.type as period_type, p.period, p.year, p.from as period_from, p.to as period_to,
				i.name AS idp_name, i.entityid AS idp_entityid, i.environment AS idp_env,
				ii.abbr AS idp_inst_abbr, ii.name AS idp_inst_name, ii.type AS idp_inst_type,
				s.name AS sp_name,  s.entityid AS sp_entityid,  s.environment AS sp_env,
				si.abbr AS sp_inst_abbr, si.name AS sp_inst_name, si.type AS sp_inst_type
			FROM statsview_periodstats AS ps
			LEFT JOIN statsview_period AS p ON p.id=ps.period_id
			LEFT JOIN statsview_idp    AS i ON i.id=ps.idp_id
			LEFT JOIN statsview_sp     AS s ON s.id=ps.sp_id
			LEFT JOIN statsview_institution as ii ON ii.id=i.institution_id
			LEFT JOIN statsview_institution as si ON si.id=s.institution_id
	");

	$dbh->query("
		CREATE OR REPLACE VIEW `statsview_v_periodstats_idptotal`
		AS
			SELECT
				p.type as period_type, p.period, p.year, p.from as period_from, p.to as period_to,
				ps.logins, ps.users,
				i.name AS idp_name, i.entityid AS idp_entityid, i.environment AS idp_env,
				ii.abbr AS idp_inst_abbr, ii.name AS idp_inst_name, ii.type AS idp_inst_type
			FROM statsview_periodstats_idptotal AS ps
			LEFT JOIN statsview_period AS p ON p.id=ps.period_id
			LEFT JOIN statsview_idp    AS i ON i.id=ps.idp_id
			LEFT JOIN statsview_institution as ii ON ii.id=i.institution_id
	");

	$dbh->query("
		CREATE OR REPLACE VIEW `statsview_v_periodstats_sptotal`
		AS
			SELECT
				p.type as period_type, p.period, p.year, p.from as period_from, p.to as period_to,
				ps.logins, ps.users,
				s.name AS sp_name,  s.entityid AS sp_entityid,  s.environment AS sp_env,
				si.abbr AS sp_inst_abbr, si.name AS sp_inst_name, si.type AS sp_inst_type
			FROM statsview_periodstats_sptotal AS ps
			LEFT JOIN statsview_period AS p ON p.id=ps.period_id
			LEFT JOIN statsview_sp     AS s ON s.id=ps.sp_id
			LEFT JOIN statsview_institution as si ON si.id=s.institution_id
	");

	print "\n";
}

function SaveIDDCache($filename,$klanten)
{
	$data = array( 'date'=>time(), 'kis_data'=>$klanten );
	$json = json_encode($data);
	$fh = fopen($filename,'wb');
	fwrite($fh,$json);
	fclose($fh);
}

function LoadIDDCache($filename,$timeout)
{
	if (!is_file($filename)) return false;

	$json = file_get_contents($filename);
	if ($json===false) return false;

	$data = json_decode($json);
	if ($data===false) return false;

	if (!property_exists($data,'date') or !property_exists($data,'kis_data')) return false;
	if ($data->date+$timeout>time()) return $data->kis_data;
	return false;
}

function FillInstitutions()
{
	global $CONFIG;
	/** @var PDO $view */
	$view  = $CONFIG['dbh-view'];

	# get uid of current user (yes, this is fun, thank you PHP)
	# can't rely on posix_getuid() as that's non-standard
	$stat = stat('/proc/self');
	$uid  = $stat['uid'];

	$cache_filename = sys_get_temp_dir()."/stats2view.{$uid}.idd.cache";
	$cache_timeout  = 24*60*60;

	# lookup table for doelgroep types
	# https://sofie.surfnet.nl/pages/viewpage.action?spaceKey=Klantinformatie&title=KIS%20Webservices%20-%20enums
	$doelgroeptable = array(
		  0 => 'Geen doelgroep', 
                  1 => 'Academische Ziekenhuizen',
		  2 => 'Bibliotheken',
		  4 => 'BVE-instellingen',
		  9 => 'HBO-instellingen',
		 11 => 'Internationaal en Netherlight aanbieders',
		 12 => 'Klant SURFconext',
		 14 => 'Overig Hoger Onderwijs',
		 18 => 'Researchinstellingen',
		 22 => 'Opleidingsziekenhuizen STZ',
		 23 => 'Universiteiten',
		 24 => 'Research for profit',
		100 => 'Overige',
	);

	print "Filling Institution data...";

	$klanten = LoadIDDCache($cache_filename,$cache_timeout);

	if ($klanten===false)
	{
		# zoek alle klanten op in KIS
		try
		{
			# connectie naar KIS
			$client = new SoapClient(
				$CONFIG['KIS']['wsdl'],
				array(
					'login'    => $CONFIG['KIS']['user'],
					'password' => $CONFIG['KIS']['password'],
					'soap_version' => SOAP_1_1,
					'trace' => true,
					'connection_timeout' => 300,
					'default_socket_timeout' => 300,
				)
			);

			$result =  $client->getKlantenByDienst(array('Dienstafkorting'=>'SURFconext'));

			if (!$result->getKlantenByDienstResult
				or !$result->getKlantenByDienstResult->Output->Klant_basic)
			{
				/** @noinspection PhpUndefinedFunctionInspection */
				throw SoapFault("Invalid result: no `getKlantenByDienstResult' found");
			}
		}
		catch (SoapFault $e)
		{
			print "Update failed: couldn't complete KIS query getKlantenByDienst\n";
			print "Error: " . $e->getMessage() . "\n";
			print getExceptionTraceAsString($e) . "\n";
			exit(1);
		}

		$klanten = $result->getKlantenByDienstResult->Output->Klant_basic;
		saveIDDCache($cache_filename,$klanten);
	}

	print ".";

	#
	# insert institutions into database
	#

	$view->beginTransaction();

	$sth = $view->prepare('
		INSERT INTO `statsview_institution`
		(`abbr`,`guid`,`type_code`,`type`,`name`) VALUES (:abbr,:guid,:tcode,:type,:name)
		ON DUPLICATE KEY UPDATE abbr=:abbr, guid=:guid, type_code=:tcode, type=:type, name=:name
	');

	print ".";

	/** @noinspection PhpWrongForeachArgumentTypeInspection */
	foreach ($klanten as $k)
	{
		if (!isset($doelgroeptable[$k->DoelgroepValue]))
		{
			printf("Onbekende doelgroep %s (%s) gevonden voor instelling %s/%s\n",
				$k->DoelgroepCode, $k->DoelgroepValue, $k->Organisatiecode, $k->OrganisatieGUID);
			exit(1);
		}
		#printf("'%s'\t'%s'\t'%s'\n",$k->Organisatiecode,$dg,$k->Organisatienaam);

		$sth->execute(array(
			':abbr'  => $k->Organisatiecode,
			':guid'  => $k->OrganisatieGUID,
			':tcode' => $k->DoelgroepValue,
			':type'  => $doelgroeptable[$k->DoelgroepValue],
			':name'  => $k->Organisatienaam,
		));
		$sth->closeCursor();
	}

	$view->commit();

	print "\n";
}

/**
 * @param string $type
 * @throws Exception
 */
function FillEntities($type)
{
	global $CONFIG;
	/** @var PDO $stats */
	/** @var PDO $view */
	$stats = $CONFIG['dbh-stats'];
	$view  = $CONFIG['dbh-view'];

	if ($type!='idp' and $type!='sp') throw new Exception("Unknown type $type");

	print "Filling $type data...";

	# insert query for entities
	$q_insert = $view->prepare("
		INSERT INTO `statsview_{$type}`
			(`name`,`entityid`,`environment`,`from`,`to`,`institution_id`)
		VALUES (:name,:entity,:env,:from,:to,GetInstitutionId(:inst))
		ON DUPLICATE KEY UPDATE
			`name`=:name,
			`from`=:from,
			`to`=:to
	");

	# update query for entities
	$q_update = $view->prepare("
		UPDATE `statsview_{$type}`
		SET `name`=:name,
		    `from`=:from,
		    `to`=:to
		WHERE id=:id
	");

	# mysql is kind of braindead, so we need to check for duplicates manually
	$q_check = $view->prepare("
		SELECT e.id
		FROM `statsview_{$type}` as e
		LEFT JOIN statsview_institution as i on i.id=e.institution_id
		WHERE e.entityid=:entity and e.environment=:env AND i.abbr<=>:inst
	");


	$view->beginTransaction();

	# fetch entity data from stats
	# complicated query, because we want to find the first date a certain entity was recorded
	$q = "
		SELECT
			e.`{$type}_name`        as 'name',
			e.`{$type}_entityid`    as 'entityid',
			e.`{$type}_environment` as 'environment',
			IFNULL(e.`{$type}_datefrom`,CONCAT(d.day_day,' 04:00:00')) as 'datefrom',
			IF(YEAR(e.`{$type}_dateto`)=2038,null,e.`{$type}_dateto`) as 'dateto',
			e.`{$type}_m_coin:institution_id` as 'iafko'
		FROM      log_analyze_{$type}    AS e
		JOIN      log_analyze_day{$type} AS de ON de.day{$type}_{$type}_id=e.{$type}_id
		LEFT JOIN log_analyze_day        AS d  ON d.day_id=de.day{$type}_day_id
		GROUP BY {$type}_id
	";
	foreach ($stats->query($q) as $p)
	{
		#print_r($p);

		# manually check for duplicates
		$q_check->execute(array(
				':entity' => $p['entityid'],
				':env'    => $p['environment'],
				':inst'   => $p['iafko'],
		));

		if ($oldentry = $q_check->fetch(PDO::FETCH_ASSOC)) {
			# update entry
			$q_update->execute(array(
					':id'     => $oldentry['id'],
					':name'   => $p['name'],
					':from'   => $p['datefrom'],
					':to'     => $p['dateto'],
			));
		}
		else
		{
			# insert entry
			$q_insert->execute(array(
					':name'   => $p['name'],
					':entity' => $p['entityid'],
					':env'    => $p['environment'],
					':from'   => $p['datefrom'],
					':to'     => $p['dateto'],
					':inst'   => $p['iafko'],
			));
		}
	}

	# done
	$q_insert->closeCursor();
	$view->commit();

	print "\n";

}

# todo: the stuff below is all rather ineffcient
# rewrite, and assume sr, tmp, and stats-view are all in the same database, so we can make a
# direct INSERT..SELECT statement, whcih should be quite a bit faster than the queries below.
function FillConnections($datum = "now")
{
	global $CONFIG;
	/** @var PDO $sr */
	/** @var PDO $view */
	$sr    = $CONFIG['dbh-sr'];
	$view  = $CONFIG['dbh-view'];
	$tmp   = $CONFIG['db-sr']['temp'];

	$date = new DateTime($datum);
	$datestr = $date->format('Y-m-d');

	print "Filling SPxIdP connection data for date '{$datestr}'...";

	# prepare data in sr database
	$sr->exec("SET @tz = 'Europe/Amsterdam'");
	$sr->exec("SET @date = '$datestr'");
	$sr->exec("SET time_zone = @tz");
	$sr->exec("DROP TABLE IF EXISTS {$tmp}.tmp_con1, {$tmp}.tmp_con2, {$tmp}.tmp_connections;");
	$sr->exec("
		# note that dates in janus tables are stored as string, so we need to do an explicit and ugly conversion
		CREATE TEMPORARY TABLE IF NOT EXISTS {$tmp}.tmp_con1
		SELECT *,CONVERT_TZ(SUBSTR(created,1,19),SUBSTR(created,20),@tz) as created2
		FROM janus__connectionRevision AS cr
		WHERE cr.revisionid = (
			SELECT revisionid
			FROM janus__connectionRevision AS x
			WHERE x.eid=cr.eid AND CONVERT_TZ(SUBSTR(x.created,1,19),SUBSTR(x.created,20),@tz)<DATE(@date)
			ORDER BY revisionid DESC
			LIMIT 1
		)
		ORDER BY eid;
	");
	# fix `created` column (was CHAR, will be TIMESTAMP)
	$sr->exec("ALTER TABLE {$tmp}.tmp_con1 DROP   COLUMN created");
    $sr->exec("ALTER TABLE {$tmp}.tmp_con1 CHANGE COLUMN created2 created TIMESTAMP");

	$sr->exec("
		# mysql can't open a temp table more than once in a query
		# yeah, pretty braindead
		CREATE TEMPORARY TABLE IF NOT EXISTS {$tmp}.tmp_con2
			SELECT * FROM {$tmp}.tmp_con1;
	");
	$sr->exec("
		# calculate the cross table IdP x SP
		# and calculate if a connection is allowed (based on allow_all and whitelist)
		# blacklist is ignored here, as we don't use it in SURFconext
		CREATE TEMPORARY TABLE {$tmp}.tmp_connections
		SELECT
			DATE(@date),
			f1.entityid as 'idp_entityid', f2.entityid as 'sp_entityid',
			f1.state as 'idp_env', f2.state as 'sp_env',
			f1.eid as `idp_eid`, f1.revisionid as `idp_rev`,
			f2.eid as `sp_eid`,  f2.revisionid as `sp_rev`,
            m1.`value` as 'idp_inst', m2.`value` as 'sp_inst',
			(f1.allowedall='yes' OR NOT ISNULL(c1.created))
				AND (f2.allowedall='yes' OR NOT ISNULL(c2.created)) as 'connected',
			f1.allowedall='yes' as 'idp_all', NOT ISNULL(c1.created) as 'idp_allowed',
			f2.allowedall='yes' as 'sp_all' , NOT ISNULL(c2.created) as 'sp_allowed',
			null
		FROM {$tmp}.tmp_con1 as f1
		CROSS JOIN {$tmp}.tmp_con2 as f2
		LEFT JOIN janus__allowedConnection AS c1 ON f1.id=c1.connectionRevisionId AND c1.remoteeid=f2.eid
		LEFT JOIN janus__allowedConnection AS c2 ON f2.id=c2.connectionRevisionId AND c2.remoteeid=f1.eid
        LEFT JOIN janus__metadata AS m1 ON f1.id=m1.connectionRevisionId AND m1.`key`='coin:institution_id'
        LEFT JOIN janus__metadata AS m2 ON f2.id=m2.connectionRevisionId AND m2.`key`='coin:institution_id'
		WHERE f1.type='saml20-idp' AND f2.type='saml20-sp'
			AND f1.state=f2.state
		HAVING connected>0
		;
	");

	$view->beginTransaction();

	# clear any old data from the connection table
	$numrows = $view->exec("
		DELETE FROM `statsview_idp-sp` WHERE `date`='$datestr';
	");
	# query to insert
	# idp_id and sp_id are search for dynamically from the specified (eid,rev)
	# vars
	# for the revision, we search for the latest version recorded by
	# log_analyze (which only records a new revision if the state, entityid,
	# or institution_id changes)
	$insert = $view->prepare("
		INSERT INTO `statsview_idp-sp` (`date`,`idp_id`,`sp_id`)
		VALUES (DATE(:date), (
			SELECT id FROM `statsview_idp`
			WHERE entityid=:idp_entity and environment=:idp_env AND institution_id=GetInstitutionId(:idp_inst)
			LIMIT 1
		),(
			SELECT id FROM `statsview_sp`
			WHERE entityid=:sp_entity and environment=:sp_env AND institution_id=GetInstitutionId(:sp_inst)
			LIMIT 1
		))
	");

	# ok, jump through loops to prevent the AUTO_INC counter to increment on each NxM failed insert
	# so make sure inserts are only attempted for data that will not fail uniqueness requirements
	$insert_idp = $view->prepare("
		INSERT INTO statsview_idp (entityid,environment,institution_id)
			SELECT *
			FROM ( SELECT :entity AS 'entityid', :env AS 'env', GetInstitutionId(:inst) as 'inst' ) t
			WHERE (
				SELECT COUNT(*) FROM statsview_idp AS i
		        WHERE i.entityid=t.entityid AND i.environment=t.env AND i.institution_id=t.inst
			)=0
	");
	$insert_sp = $view->prepare("
		INSERT INTO statsview_sp (entityid,environment,institution_id)
			SELECT *
			FROM ( SELECT :entity AS 'entityid', :env AS 'env', GetInstitutionId(:inst) as 'inst' ) t
			WHERE (
				SELECT COUNT(*) FROM statsview_sp AS i
		        WHERE i.entityid=t.entityid AND i.environment=t.env AND i.institution_id=t.inst
			)=0
	");

	# select all connections
	$q = "
		SELECT
			idp_entityid,idp_env,idp_inst,
			sp_entityid ,sp_env ,sp_inst,
			connected
		FROM ${tmp}.tmp_connections
		WHERE connected>0
	";
	foreach ( $sr->query($q) as $row )
	{
		if     ($row['idp_env']=='prodaccepted') $idp_env='PA';
		elseif ($row['idp_env']=='testaccepted') $idp_env='TA';
		else throw new Exception("Unknown state {$row['idp_env']}");

		if     ($row['sp_env']=='prodaccepted') $sp_env='PA';
		elseif ($row['sp_env']=='testaccepted') $sp_env='TA';
		else throw new Exception("Unknown state {$row['sp_env']}");

		try {
			# make sure IdP and SP exist in the DB: there might not have bene any logins
			# from/to/the SP/IdP, so they won't be registered in teh stats db
			$insert_idp->execute(array(
					':entity' => $row['idp_entityid'],
					':env'    => $idp_env,
					':inst'   => $row['idp_inst'],
			));
			$insert_sp->execute(array(
					':entity' => $row['sp_entityid'],
					':env'    => $sp_env,
					':inst'   => $row['sp_inst'],
			));

			# insert the actual connection
			$insert->execute(array(
				':date'       => $datestr,
				':idp_entity' => $row['idp_entityid'],
				':idp_env'    => $idp_env,
				':idp_inst'   => $row['idp_inst'],
				':sp_entity'  => $row['sp_entityid'],
				':sp_env'     => $sp_env,
				':sp_inst'    => $row['sp_inst'],
			));
		} catch (PDOException $e)
		{
			print "\n";
			print "query failed:\n";
			print_r($row);
			print $e;
			print "\n";
			exit();
		}
	}

	$view->commit();
	$sr->exec("DROP TABLE IF EXISTS {$tmp}.tmp_con1, {$tmp}.tmp_con2, {$tmp}.tmp_connections;");

	print "\n";

}

function FillPeriods()
{
	global $CONFIG;
	/** @var PDO $stats */
	/** @var PDO $view */
	$stats = $CONFIG['dbh-stats'];
	$view  = $CONFIG['dbh-view'];

	print "Filling Period data...";

	# insert period data from log_analyze
	$view->beginTransaction();

	# insert query for periods
	$q_insert = $view->prepare('
		INSERT INTO `statsview_period`
			(`type`,`period`,`year`,`environment`,`from`,`to`,`logins`,`users`,`created`,`updated`)
		VALUES (:type,:period,:year,:env,:from,:to,:logins,:users,:created,:updated)
		ON DUPLICATE KEY UPDATE updated=:updated, logins=:logins, users=:users
	');

	# first select multi-day periods
	$q = "
		SELECT *
		FROM `log_analyze_period`;
	";
	foreach ($stats->query($q) as $period)
	{
		# insert data
		$q_insert->execute(array(
			':type'    => $period['period_type'],
			':period'  => $period['period_period'],
			':year'    => $period['period_year'],
			':env'     => $period['period_environment'],
			':from'    => $period['period_from'],
			':to'      => $period['period_to'],
			':logins'  => $period['period_logins'],
			':users'   => $period['period_users'],
			':created' => $period['period_created'],
			':updated' => $period['period_updated'],
		));
	}

	# and then insert the individual days
	$q = "
		SELECT *
		FROM `log_analyze_day`;
	";
	foreach ($stats->query($q) as $day)
	{
		$dt = new DateTime($day['day_day']);

		# insert data
		$q_insert->execute(array(
			':type'    => 'd',
			':period'  => intval($dt->format('z'))+1,
			':year'    => $dt->format('Y'),
			':env'     => $day['day_environment'],
			':from'    => $dt->format('Y-m-d 00:00:00'),
			':to'      => $dt->format('Y-m-d 23:59:59'),
			':logins'  => $day['day_logins'],
			':users'   => $day['day_users'],
			':created' => $day['day_created'],
			':updated' => $day['day_updated'],
		));
	}

	$view->commit();

	print "\n";
}

function FillPeriodStats()
{
	global $CONFIG;
	/** @var PDO $stats */
	/** @var PDO $view */
	$stats = $CONFIG['dbh-stats'];
	$view  = $CONFIG['dbh-view'];

	print "Filling PeriodStats data...";

	# insert logins/users per (IdP,SP)from log_analyze
	$view->beginTransaction();

	# insert query
	$q_insert = $view->prepare('
		INSERT INTO `statsview_periodstats`
			(`period_id`,`idp_id`,`sp_id`,`logins`,`users`)
			VALUES ((
					SELECT id FROM statsview_period
					WHERE type=:type AND period=:period AND year=:year AND environment=:env
					LIMIT 1
				),(
					SELECT id FROM `statsview_idp`
					WHERE entityid=:idp_entity and environment=:idp_env AND institution_id=GetInstitutionId(:idp_inst)
				),(
					SELECT id FROM `statsview_sp`
					WHERE entityid=:sp_entity and environment=:sp_env AND institution_id=GetInstitutionId(:sp_inst)
				),:logins,:users)
		ON DUPLICATE KEY UPDATE logins=:logins, users=:users
	');

	# first insert data from multi-day periods (weeks, months, etc)
	print "periods...";
	$q = "
        SELECT
			periodstats_logins, periodstats_users,
			period_type, period_period, period_year, period_environment,
			idp_entityid, idp_environment, `idp_m_coin:institution_id`,
			sp_entityid,  sp_environment,  `sp_m_coin:institution_id`
		FROM `log_analyze_periodstats` as `ps`
		LEFT JOIN log_analyze_period AS `p` ON p.period_id=ps.periodstats_period_id
		LEFT JOIN log_analyze_idp    AS `i` ON i.idp_id=ps.periodstats_idp_id
		LEFT JOIN log_analyze_sp     AS `s` ON s.sp_id=ps.periodstats_sp_id
		WHERE periodstats_idp_id IS NOT NULL AND periodstats_sp_id IS NOT NULL;
	";
	foreach ($stats->query($q,PDO::FETCH_ASSOC) as $ps)
	{
		# insert data
		$q_insert->execute(array(
			':type'       => $ps['period_type'],
			':period'     => $ps['period_period'],
			':year'       => $ps['period_year'],
			':env'        => $ps['period_environment'],
			':idp_entity' => $ps['idp_entityid'],
			':idp_env'    => $ps['idp_environment'],
			':idp_inst'   => $ps['idp_m_coin:institution_id'],
			':sp_entity'  => $ps['sp_entityid'],
			':sp_env'     => $ps['sp_environment'],
			':sp_inst'    => $ps['sp_m_coin:institution_id'],
			':logins'     => $ps['periodstats_logins'],
			':users'      => $ps['periodstats_users'],
		));
	}


	# then insert data from individual days
	print "days...";
	$q = "
		SELECT
			d.day_day, d.day_environment,
			i.idp_entityid, i.idp_environment, i.`idp_m_coin:institution_id`,
			s.sp_entityid,  s.sp_environment,  s.`sp_m_coin:institution_id`,
			st.stats_logins, st.stats_users
		FROM log_analyze_stats   AS st
		LEFT JOIN log_analyze_day      AS `d` on d.day_id=st.stats_day_id
		LEFT JOIN log_analyze_provider AS `p` ON p.provider_id=st.stats_provider_id
		LEFT JOIN log_analyze_idp      AS `i` ON i.idp_id=p.provider_idp_id
		LEFT JOIN log_analyze_sp       AS `s` ON s.sp_id=p.provider_sp_id
	";
	foreach ($stats->query($q) as $ps)
	{
		$dt = new DateTime($ps['day_day']);

		# insert data
		$q_insert->execute(array(
			':type'       => 'd',
			':period'     => intval($dt->format('z'))+1,
			':year'       => $dt->format('Y'),
			':env'        => $ps['day_environment'],
			':idp_entity' => $ps['idp_entityid'],
			':idp_env'    => $ps['idp_environment'],
			':idp_inst'   => $ps['idp_m_coin:institution_id'],
			':sp_entity'  => $ps['sp_entityid'],
			':sp_env'     => $ps['sp_environment'],
			':sp_inst'    => $ps['sp_m_coin:institution_id'],
			':logins'     => $ps['stats_logins'],
			':users'      => $ps['stats_users'],
		));
	}

	$view->commit();

	print "\n";
}

function FillPeriodStatsTotals($type)
{
	global $CONFIG;
	/** @var PDO $stats */
	/** @var PDO $view */
	$stats = $CONFIG['dbh-stats'];
	$view  = $CONFIG['dbh-view'];

	if ($type!='idp' and $type!='sp') throw new Exception("Unknown type $type");

	print "Filling PeriodStats {$type} totals data...";

	# insert IdP data from log_analyze
	$view->beginTransaction();

	# insert query
	$q_insert = $view->prepare("
		INSERT INTO `statsview_periodstats_{$type}total`
			(`period_id`,`{$type}_id`,`logins`,`users`)
			VALUES ((
					SELECT id FROM statsview_period
					WHERE type=:type AND period=:period AND year=:year AND environment=:env
					LIMIT 1
				),(
					SELECT id FROM `statsview_{$type}`
					WHERE entityid=:entity and environment=:entityenv AND institution_id=GetInstitutionId(:inst)
				),:logins,:users)
		ON DUPLICATE KEY UPDATE logins=:logins, users=:users
	");

	# first do longer periods
	print "periods...";
	$q = "
		SELECT
			period{$type}_logins as 'logins',
			period{$type}_users  as 'users',
			period_type, period_period, period_year, period_environment,
			{$type}_entityid as 'entity_id',
			{$type}_environment as 'entity_env',
			`{$type}_m_coin:institution_id` as 'entity_inst'
		FROM `log_analyze_period{$type}` AS `ps`
		LEFT JOIN log_analyze_period     AS `p` ON p.period_id=ps.period{$type}_period_id
		LEFT JOIN log_analyze_{$type}    AS `i` ON i.{$type}_id=ps.period{$type}_{$type}_id
	";
	foreach ($stats->query($q) as $ps)
	{
		# insert data
		$q_insert->execute(array(
			':type'       => $ps['period_type'],
			':period'     => $ps['period_period'],
			':year'       => $ps['period_year'],
			':env'        => $ps['period_environment'],
			':entity'     => $ps['entity_id'],
			':entityenv'  => $ps['entity_env'],
			':inst'       => $ps['entity_inst'],
			':logins'     => $ps['logins'],
			':users'      => $ps['users'],
		));
	}

	# then handle individual days
	print "days...";
	$q = "
		SELECT
			day{$type}_logins as 'logins',
			day{$type}_users as 'users',
			DAYOFYEAR(day_day) as 'day', YEAR(day_day) as 'year', day_environment,
			{$type}_entityid as 'entity_id',
			{$type}_environment as 'entity_env',
			`{$type}_m_coin:institution_id` as 'entity_inst'
		FROM `log_analyze_day{$type}` as `ps`
		LEFT JOIN log_analyze_day AS `d` ON d.day_id=ps.day{$type}_day_id
		LEFT JOIN log_analyze_{$type} AS `i` ON i.{$type}_id=ps.day{$type}_{$type}_id
        order by DATE(day_day)
	";
	foreach ($stats->query($q) as $ps)
	{
		# insert data
		$q_insert->execute(array(
			':type'       => 'd',
			':period'     => $ps['day'],
			':year'       => $ps['year'],
			':env'        => $ps['day_environment'],
			':entity'     => $ps['entity_id'],
			':entityenv'  => $ps['entity_env'],
			':inst'       => $ps['entity_inst'],
			':logins'     => $ps['logins'],
			':users'      => $ps['users'],
		));
	}

	$view->commit();

	print "\n";
}


/**
 * proper array slice (i.e. return only elements of an array whose keys are specified)
 * @param array $array
 * @param array $keys
 * @return array
 */
function _array_slice_assoc($array,$keys) {
	return array_intersect_key($array,array_flip($keys));
}

/**
 * compare the specifies fields of each of the records of $array1 and $array2 and return a list of records only present in $array1, only present in $array2, and present in both
 * @param $array1 array
 * @param $array2 array
 * @param $fieldlist array
 * @return array(array,array,array)
 */
function _dedup_array($array1,$array2,$fieldlist)
{
	$array1_only = array();
	$array2_only = array();
	$array_both  = array();
	$array1_lookup = array();
	$array2_lookup = array();
	$all_keys      = array();

	# build an index of keys in the two input arrays
	# the key consists of a concatenation of the fields specified in $fieldlist
	foreach ($array1 as $a1)
	{
		$key = implode('|', _array_slice_assoc($a1,$fieldlist));
		$array1_lookup[$key] = $a1;
		$all_keys[$key] = 1;
	}
	foreach ($array2 as $a2)
	{
		$key = implode('|', _array_slice_assoc($a2,$fieldlist));
		$array2_lookup[$key] = $a2;
		$all_keys[$key] = 1;
	}

	if (0)
	{
		print "---\n";
		print "a1_lookup: ";
		print_r($array1_lookup);
		print "a2_lookup: ";
		print_r($array2_lookup);
		print "keys:      ";
		print_r($all_keys);
		print "---\n";
	}

	# now loop over all keys and sort the entries into the correct output array
	foreach (array_keys($all_keys) as $key)
	{
		if     (!isset($array1_lookup[$key])) $array2_only[] = $array2_lookup[$key];
		elseif (!isset($array2_lookup[$key])) $array1_only[] = $array1_lookup[$key];
		else                                  $array_both[]  = array_merge($array2_lookup[$key],$array1_lookup[$key]);
	}

	return array($array_both,$array1_only,$array2_only);
}

function __test_dedup_array()
{
	$a1 = array(
			array("hoi" => 'hoi 0', "bla" => 0, "foo" => 2),
			array("hoi" => 'hoi 1', "bla" => 1, "foo" => 2),
			array("hoi" => 'hoi 2', "bla" => 2, "foo" => 2),
	);
	$a2 = array(
			array('hoi' => 'hoi 2', "bla" => 22, "baz" => 4),
			array('hoi' => 'hoi 3', "bla" => 3, "baz" => 4),
			array('hoi' => 'hoi 4', "bla" => 4, "baz" => 4),
	);

	print "a1: ";
	print_r($a1);
	print "a2: ";
	print_r($a2);
	print "=====\n";
	list($a12_both, $a1_only, $a2_only,) = _dedup_array($a1, $a2, array("hoi", "bla"));
	print "both:    ";
	print_r($a12_both);
	print "a1 only: ";
	print_r($a1_only);
	print "a2 only: ";
	print_r($a2_only);
	exit(0);
}

function SyncEntitiesAndSR($type)
{
	global $CONFIG;
	/** @var PDO $sr */
	/** @var PDO $view */
	$sr    = $CONFIG['dbh-sr'];
	$view  = $CONFIG['dbh-view'];

	print "Syncing with Service Registry ($type)";

	# prepare data in sr database
	$sr->exec("SET @tz = '+00:00'");
	$sr->exec("SET time_zone = @tz");

	# list of all current entities in SR
	$q = "
		SELECT
			cr.entityid,
			CASE cr.state WHEN 'prodaccepted' THEN 'PA' WHEN 'testaccepted' THEN 'TA' ELSE '?' END AS 'environment',
			m1.`value` as 'inst',
			(
				SELECT MIN(Xcr.created)
				FROM sr.janus__connectionRevision AS Xcr
				LEFT JOIN sr.janus__metadata      AS Xm  ON Xm.connectionRevisionId=Xcr.id AND Xm.`key`='coin:institution_id'
				WHERE Xcr.type='saml20-{$type}' AND Xcr.entityid=cr.entityid and Xcr.state=cr.state and Xm.`value`<=>m1.`value`
			) AS 'from',
			IFNULL(m3.`value`,m2.`value`) AS 'name'
		FROM      janus__connection         AS c
		LEFT JOIN janus__connectionRevision AS cr ON cr.eid=c.id AND cr.revisionid=c.revisionNr
		LEFT JOIN janus__metadata           AS m1  ON m1.connectionRevisionId=cr.id AND m1.`key`='coin:institution_id'
		LEFT JOIN janus__metadata           AS m2  ON m2.connectionRevisionId=cr.id AND m2.`key`='name:en' AND m2.`value`!=''
		LEFT JOIN janus__metadata           AS m3  ON m3.connectionRevisionId=cr.id AND m3.`key`='name:nl' AND m3.`value`!=''
		WHERE cr.`type`='saml20-{$type}'
	";
	$sr_entities = $sr->query($q)->fetchAll(PDO::FETCH_ASSOC);

	# check that all institution abbrs are correct
	$select = $view->prepare('SELECT GetInstitutionId(:abbr)');
	foreach ($sr_entities as &$e)
	{
		if ($e['inst']===null) continue;
		$select->execute(array( ':abbr' => $e['inst'] ));
		if ($select->fetchColumn(0)==0) $e['inst'] = null;
	}

	# list of all current entities in stats-view, including the last time we recorded a login
	$q = "
		SELECT entityid, environment, abbr AS 'inst', (
			SELECT MAX(p.`to`)
			FROM      statsview_periodstats_{$type}total AS t
			LEFT JOIN statsview_period                   AS p ON p.id=t.period_id
			WHERE t.{$type}_id=e.id AND p.`type`='d' AND t.`logins`>0
		) AS 'lastseen'
		FROM statsview_{$type} AS e
		LEFT JOIN statsview_institution AS i ON i.id=e.institution_id
	";
	$view_entities = $view->query($q)->fetchAll(PDO::FETCH_ASSOC);

	list($entities_both,$entities_only_sr,$entities_only_sv) = _dedup_array($sr_entities,$view_entities,array('entityid','environment','inst'));

	if (0)
	{
		print "ServiceRegistry: ";   print_r($sr_entities);
		print "Stats-view: ";        print_r($view_entities);
		print "both: ";              print_r($entities_both);
		print "sv-only: ";           print_r($entities_only_sv);
		print "sr-only: ";           print_r($entities_only_sr);
	}

	print ".";

	$view->query('START TRANSACTION');

	# entities present in both SR and stats-view need to have the end_date set to null and the sr_active flag to true
	$q = "
		UPDATE statsview_{$type}
		SET `sr_active`=:active,`to`=:to, `name`=:name
		WHERE entityid=:entityid AND environment=:env AND institution_id=GetInstitutionId(:inst)
	";
	$update = $view->prepare($q);
	print ".";
	foreach ($entities_both as $e)
	{
		#print " - {$e['entityid']}-{$e['environment']}-{$e['inst']}\n";
		$update->execute(array(
			':active'   => 1,
			':to'       => null,
			':entityid' => $e['entityid'],
			':name'     => $e['name'],
			':env'      => $e['environment'],
			':inst'     => $e['inst'],
		));
	}

	# entities present only in SR are introduced in stats-view, too
	$q = "
		INSERT INTO statsview_{$type}
		(`entityid`,`environment`,`institution_id`,`name`,`sr_active`,`from`,`to`)
		VALUES (:entityid,:env,GetInstitutionId(:inst),:name,:active,:from,:to)
	";
	$update = $view->prepare($q);
	print ".";
	foreach ($entities_only_sr as $e)
	{
		#print " - {$e['entityid']}-{$e['environment']}-{$e['inst']}\n";
		$update->execute(array(
				':entityid' => $e['entityid'],
				':env'      => $e['environment'],
				':inst'     => $e['inst'],
				':name'     => $e['name'],
				':active'   => 1,
				':from'     => $e['from'],
				':to'       => null,
		));
	}

	# entities present only in statsview are deactivated
	$q = "
		UPDATE statsview_{$type}
		SET `sr_active`=:active,`to`=:to
		WHERE entityid=:entityid AND environment=:env AND institution_id=GetInstitutionId(:inst)
	";
	$update = $view->prepare($q);
	$now = new DateTime('now',new DateTimeZone('UTC'));
	$nowstr = $now->format('Y-m-d H:M:S');
	print ".";
	foreach ($entities_only_sv as $e)
	{
		#print " - {$e['entityid']}-{$e['environment']}-{$e['inst']}\n";
		$update->execute(array(
				':entityid' => $e['entityid'],
				':env'      => $e['environment'],
				':inst'     => $e['inst'],
				':active'   => 0,
				':to'       => $e['lastseen'],
		));
	}

	$view->query('COMMIT');

	print "\n";
}

### main
function main()
{
	global $CONFIG;

	# INPUT
	$ARGS = getopt('c::si');

	if (!isset($ARGS['c']) and !isset($ARGS['s']) and !isset($ARGS['i']))
	{
		print "Please specify:\n";
		print " -s  update statsview database\n";
		print " -c  record connections table\n";
		print " -i  clean and reinitialize database\n";
		exit(1);
	}

	$CONFIG['dbh-sr'   ] = OpenDatabases( $CONFIG['db-sr'   ] );
	$CONFIG['dbh-stats'] = OpenDatabases( $CONFIG['db-stats'] );
	$CONFIG['dbh-view' ] = OpenDatabases( $CONFIG['db-view' ] );

	if (isset($ARGS['s']))
	{
		FillInstitutions();
		FillEntities('idp');
		FillEntities('sp');
		FillPeriods();
		FillPeriodStats();
		FillPeriodStatsTotals('idp');
		FillPeriodStatsTotals('sp');
		SyncEntitiesAndSR('idp');
		SyncEntitiesAndSR('sp');
	}
	elseif (isset($ARGS['c']))
	{
		$date = "now";
		if ($ARGS['c']) $date=$ARGS['c'];
		FillInstitutions();
		FillConnections($date);
		SyncEntitiesAndSR('idp');
		SyncEntitiesAndSR('sp');
	}
	elseif (isset($ARGS['i']))
	{
		$res = readline("Do you really want to delete all data?!!");
		if (strtolower($res)!='y')
		{
			print "Ok, bailing out.\n";
			exit(2);
		}

		ClearView();
		CreateViewSchema();

	}
}

main();


?>
