CREATE TABLE janus__connection (
  id         INT(11) NOT NULL AUTO_INCREMENT,
  revisionNr INT(11) NOT NULL,
  name       VARCHAR(255)
                     NOT NULL,
  type       VARCHAR(50)
                     NOT NULL,
  user       INT(11)          DEFAULT NULL,
  created    CHAR(25)
                              DEFAULT NULL,
  ip         CHAR(39)
                              DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE janus__connectionRevision (
  id                   INT(11)  NOT NULL AUTO_INCREMENT,
  eid                  INT(11)  NOT NULL,
  entityid             TEXT     NOT NULL,
  revisionid           INT(11)  NOT NULL,
  state                TEXT,
  type                 TEXT,
  expiration           CHAR(25)          DEFAULT NULL,
  metadataurl          TEXT,
  metadata_valid_until DATETIME          DEFAULT NULL,
  metadata_cache_until DATETIME          DEFAULT NULL,
  allowedall           CHAR(25) NOT NULL DEFAULT 'yes',
  manipulation         MEDIUMTEXT,
  user                 INT(11)           DEFAULT NULL,
  created              CHAR(25)          DEFAULT NULL,
  ip                   CHAR(39)          DEFAULT NULL,
  parent               INT(11)           DEFAULT NULL,
  revisionnote         TEXT,
  active               CHAR(3)  NOT NULL DEFAULT 'yes',
  arp_attributes       TEXT,
  notes                TEXT,
  PRIMARY KEY (id)
);

CREATE TABLE janus__user (
  uid    INT(11) NOT NULL AUTO_INCREMENT,
  userid TEXT,
  PRIMARY KEY (uid)
);

CREATE TABLE janus__metadata (
  connectionRevisionId INT(11)      NOT NULL,
  key_column           VARCHAR(255) NOT NULL DEFAULT '',
  value                TEXT         NOT NULL,
  created              CHAR(25)     NOT NULL,
  ip                   CHAR(39)     NOT NULL,
  PRIMARY KEY (connectionRevisionId, key_column)
);

CREATE TABLE janus__allowedConnection (
  connectionRevisionId INT(11)  NOT NULL,
  remoteeid            INT(11)  NOT NULL,
  created              CHAR(25) NOT NULL,
  ip                   CHAR(39) NOT NULL,
  PRIMARY KEY (connectionRevisionId, remoteeid)
);

CREATE TABLE janus__disableConsent (
  connectionRevisionId INT(11)  NOT NULL,
  remoteeid            INT(11)  NOT NULL,
  created              CHAR(25) NOT NULL,
  ip                   CHAR(39) NOT NULL,
  PRIMARY KEY (connectionRevisionId, remoteeid)
);


INSERT INTO janus__connection (id, revisionNr, name, type, user, created, ip)
VALUES
  (1, 2, 'https://teams.surfconext.nl/shibboleth', 'saml20-sp', 1, '2014-05-02T23:31:13+02:00', '145.101.112.195'),
  (2, 2, 'https://serviceregistry.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp', 'saml20-sp', 1,
   '2014-02-21T10:45:33+01:00', '195.169.127.148'),
  (3, 2, 'https://sso.eur.nl/opensso', 'saml20-idp', 1, '2014-05-23T09:14:20+02:00', '145.101.112.195'),
  (4, 2, 'https://surfguest.nl', 'saml20-idp', 1, '2014-02-18T17:37:34+01:00', '195.169.127.181');


INSERT INTO janus__connectionRevision (id, eid, entityid, revisionid, state, type, expiration, metadataurl, metadata_valid_until, metadata_cache_until, allowedall, manipulation, user, created, ip, parent, revisionnote, active, arp_attributes, notes)
VALUES
  (1, 1, 'https://teams.surfconext.nl/shibboleth', 0, 'prodaccepted', 'saml20-sp', NULL,
      'https://teams.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp', NULL, NULL, 'no', NULL, 1,
   '2011-01-31T09:27:39+01:00', '192.87.109.132', 8, 'No revision note', 'yes',
   'a:3:{s:47:"urn:mace:dir:attribute-def:eduPersonAffiliation";a:1:{i:0;s:1:"*";}s:31:"urn:mace:dir:attribute-def:mail";a:1:{i:0;s:1:"*";}s:44:"urn:mace:dir:attribute-def:preferredLanguage";a:1:{i:0;s:1:"*";}}',
   NULL),
  (2, 1, 'https://teams.surfconext.nl/shibboleth', 1, 'prodaccepted', 'saml20-sp', NULL, 'https://teams.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp', NULL, NULL, 'no', NULL, 1, '2011-01-31T09:27:39+01:00', '192.87.109.132', 8, 'No revision note', 'yes', 'a:3:{s:47:"urn:mace:dir:attribute-def:eduPersonAffiliation";a:1:{i:0;s:1:"*";}s:31:"urn:mace:dir:attribute-def:mail";a:1:{i:0;s:1:"*";}s:44:"urn:mace:dir:attribute-def:preferredLanguage";a:1:{i:0;s:1:"*";}}', NULL),
  (3, 1, 'https://teams.surfconext.nl/shibboleth', 2, 'prodaccepted', 'saml20-sp', NULL, 'https://teams.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp', NULL, NULL, 'no', NULL, 1, '2011-01-31T09:27:39+01:00', '192.87.109.132', 8, 'No revision note', 'yes', 'a:3:{s:47:"urn:mace:dir:attribute-def:eduPersonAffiliation";a:1:{i:0;s:1:"*";}s:31:"urn:mace:dir:attribute-def:mail";a:1:{i:0;s:1:"*";}s:44:"urn:mace:dir:attribute-def:preferredLanguage";a:1:{i:0;s:1:"*";}}', NULL),
  (4, 2, 'https://serviceregistry.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp', 0, 'prodaccepted', 'saml20-sp', NULL, 'https://teams.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp', NULL, NULL, 'yes', NULL, 1, '2011-01-31T09:27:39+01:00', '192.87.109.132', 8, 'No revision note', 'yes', 'a:3:{s:47:"urn:mace:dir:attribute-def:eduPersonAffiliation";a:1:{i:0;s:1:"*";}s:31:"urn:mace:dir:attribute-def:mail";a:1:{i:0;s:1:"*";}s:44:"urn:mace:dir:attribute-def:preferredLanguage";a:1:{i:0;s:1:"*";}}', NULL),
  (5, 2, 'https://serviceregistry.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp', 1, 'testaccepted', 'saml20-sp', NULL, 'https://teams.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp', NULL, NULL, 'yes', NULL, 1, '2011-01-31T09:27:39+01:00', '192.87.109.132', 8, 'No revision note', 'yes', 'a:3:{s:47:"urn:mace:dir:attribute-def:eduPersonAffiliation";a:1:{i:0;s:1:"*";}s:31:"urn:mace:dir:attribute-def:mail";a:1:{i:0;s:1:"*";}s:44:"urn:mace:dir:attribute-def:preferredLanguage";a:1:{i:0;s:1:"*";}}', NULL),
  (6, 2, 'https://serviceregistry.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp', 2, 'prodaccepted', 'saml20-sp', NULL, 'https://teams.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp', NULL, NULL, 'yes', NULL, 1, '2011-01-31T09:27:39+01:00', '192.87.109.132', 8, 'No revision note', 'yes', 'a:3:{s:47:"urn:mace:dir:attribute-def:eduPersonAffiliation";a:1:{i:0;s:1:"*";}s:31:"urn:mace:dir:attribute-def:mail";a:1:{i:0;s:1:"*";}s:44:"urn:mace:dir:attribute-def:preferredLanguage";a:1:{i:0;s:1:"*";}}', NULL),
  (7, 3, 'https://sso.eur.nl/opensso', 0, 'prodaccepted', 'saml20-idp', NULL, NULL, NULL, NULL, 'yes', NULL, 21, '2011-04-08T10:47:38+02:00', '127.0.0.255', NULL, 'Entity created.', 'yes', 'N;', NULL),
  (8, 3, 'https://sso.eur.nl/opensso', 1, 'prodaccepted', 'saml20-idp', NULL, 'https://wayf.surfnet.nl/federate/metadata/saml20/https%253a%252f%252fsso.eur.nl%252fopensso', NULL, NULL, 'yes', NULL, 21, '2011-04-08T10:49:47+02:00', '127.0.0.255', 0, 'No revision note', 'yes', 'N;', NULL),
  (9, 3, 'https://sso.eur.nl/opensso', 2, 'prodaccepted', 'saml20-idp', NULL, 'https://wayf.surfnet.nl/federate/metadata/saml20/https%253a%252f%252fsso.eur.nl%252fopensso', NULL, NULL, 'yes', NULL, 21, '2011-04-08T10:51:30+02:00', '127.0.0.255', 1, 'updated metadata', 'yes', 'N;', NULL),
  (10, 4, 'https://surfguest.nl', 0, 'prodaccepted', 'saml20-idp', NULL, 'https://espee.surfnet.nl/federate/metadata/saml20', NULL, NULL, 'no', NULL, 9, '2012-11-05T11:10:41+01:00', '127.0.0.255', 52, 'No revision note', 'yes', 'N;', NULL),
  (11, 4, 'https://surfguest.nl', 1, 'prodaccepted', 'saml20-idp', NULL, 'https://espee.surfnet.nl/federate/metadata/saml20', NULL, NULL, 'no', NULL, 9, '2012-11-05T11:04:43+01:00', '127.0.0.255', 50, 'No revision note', 'yes', 'N;', NULL),
  (12, 4, 'https://surfguest.nl', 2, 'testaccepted', 'saml20-idp', NULL,
       'https://espee.surfnet.nl/federate/metadata/saml20', NULL, NULL, 'no', NULL, 9, '2012-11-05T11:10:36+01:00',
   '127.0.0.255', 51, 'Allowed UMC-ALL as per CXT-653', 'yes', 'N;', NULL);

INSERT INTO janus__allowedConnection (connectionRevisionId, remoteeid, created, ip)
VALUES
  (3, 3, '2014-01-30T09:11:44+01:00', '195.169.126.114'),
  (10, 2, '2014-01-30T09:11:44+01:00', '195.169.126.114');

INSERT INTO janus__disableConsent (connectionRevisionId, remoteeid, created, ip)
VALUES
  (9, 2, '2013-01-31T09:16:30+01:00', '192.87.109.80');

INSERT INTO janus__metadata (connectionRevisionId, key_column, value, created, ip)
VALUES
  (3, 'AssertionConsumerService:0:Binding', 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST',
   '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (3, 'AssertionConsumerService:0:index', '0', '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (3, 'AssertionConsumerService:0:Location', 'https://serviceregistry.surfconext.nl/simplesaml/module.php/saml/sp/saml2-acs.php/default-sp', '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (3, 'base64attributes', '1', '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (3, 'certData', '', '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (3, 'description:en', 'serviceregistry', '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (3, 'description:nl', 'serviceregistry', '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (3, 'displayName:en', 'serviceregistry', '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (3, 'displayName:nl', 'serviceregistry', '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (3, 'name:en', 'serviceregistry', '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (3, 'name:nl', 'serviceregistry', '2011-01-28T11:17:56+01:00', '195.240.2.130'),
  (12, 'certData', 'nope', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'coin:guest_qualifier', 'None', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'coin:institution_guid', 'ae82fa68-0911-e511-80d0-005056956c1a', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'coin:institution_id', 'EUR', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'coin:publish_in_edugain', '1', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'coin:publish_in_edugain_date', '2015-06-30T13:30:00Z', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:0:contactType', 'technical', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:0:emailAddress', 'vandenbos@ict.eur.nl', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:0:givenName', 'Rob', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:0:surName', 'van den Bos', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:0:telephoneNumber', '010-4081235', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:1:contactType', 'administrative', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:1:emailAddress', 'scholten@ict.eur.nl', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:1:givenName', 'Martin', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:1:surName', 'Scholten', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:2:contactType', 'technical', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:2:emailAddress', 'nope@ict.eur.nl', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:2:givenName', 'Norbert', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:2:surName', 'de Rooy', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:2:telephoneNumber', '010-nope', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'contacts:3:contactType', 'technical', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'description:en', 'Erasmus University Rotterdam', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'description:nl', 'Erasmus Universiteit Rotterdam', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'displayName:en', 'Erasmus University Rotterdam', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'displayName:nl', 'Erasmus Universiteit Rotterdam', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'keywords:en', 'erasmus universiteit rotterdam erasmus university rotterdam eur', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'keywords:nl', 'erasmus universiteit rotterdam erasmus university rotterdam eur', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'logo:0:height', '25', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'logo:0:url', 'https://static.surfconext.nl/logos/idp/eur.png', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'logo:0:width', '108', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'name:en', 'Erasmus University Rotterdam', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'name:nl', 'Erasmus Universiteit Rotterdam', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'OrganizationDisplayName:en', 'Erasmus University Rotterdam', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'OrganizationName:en', 'Erasmus University Rotterdam', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'OrganizationURL:en', 'http://www.eur.nl/', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'shibmd:scope:0:allowed', 'eur.nl', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'SingleSignOnService:0:Binding', 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect', '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'SingleSignOnService:0:Location', 'https://sso.eur.nl/opensso/SSORedirect/metaAlias/Erasmus/idp',
   '2017-08-10T13:31:14+02:00', '145.101.112.195'),
  (12, 'SingleSignOnService:1:Binding', 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST', '2017-08-10T13:31:14+02:00',
   '145.101.112.195'),
  (12, 'SingleSignOnService:1:Location', 'https://sso.eur.nl/opensso/SSOPOST/metaAlias/Erasmus/idp',
   '2017-08-10T13:31:14+02:00', '145.101.112.195');




