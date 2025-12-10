# Release notes

Starting from version 9.0.0, we note changes and new features per release in this file.

## 9.6.1
- Disable cookies for outgoing http requests 
  (continuation of [#565](https://github.com/OpenConext/OpenConext-manage/issues/565))

## 9.6.0
- [#562](https://github.com/OpenConext/OpenConext-manage/issues/562)
- Added `typeRequest` field to change requests
- Added configuration for disabling unused schemas
  ([#573](https://github.com/OpenConext/OpenConext-manage/issues/573))
- Removed non-current endpoints when importing new SAML metadata
  ([#577](https://github.com/OpenConext/OpenConext-manage/issues/577))
- Added a copy-to-clipboard button for PDP requests and responses
- Removed the manual `coin:policy_enforcement_decision` toggle. It is now set automatically in the pushed metadata 
  (no longer visible in the UI) 
  ([#477](https://github.com/OpenConext/OpenConext-manage/issues/477))
- Added `docker-compose.yml` and other plumbing for easier local development
  ([#570](https://github.com/OpenConext/OpenConext-manage/issues/570))
- Updated dependencies

### Upgrade note
The following configuration options have been changed:
  - `disabled_metadata_schemas`: (**new**) a list of schemas that should be disabled.  
    Set to `[]` to keep the default behaviour to show all schemas. 
  - `cron.node-cron-job-responsible`: it is now possible to set this to `true` for more than one node, 
    but this is not required.
    Keep it set to `true` on a single host to keep the current behaviour.

The metadata field `coin:policy_enforcement_decision_required` is obsolete.  
It will now be set automatically based on the defined policies but will not be visible in the UI; 
manually set values (which _are_ still visible) will be ignored.
A migration to remove this field from existing entities will follow in a future release.

## 9.5.1
- Fixed bug that caused policy input fields to be extremely slow 
  ([#568](https://github.com/OpenConext/OpenConext-manage/issues/568))

## 9.5.0
- Introduce a database lock to prevent multiple cron jobs running at the same time
- Fix bug that caused backend calls to be too persistent and stick to disabled load balancer backends 
  ([#565](https://github.com/OpenConext/OpenConext-manage/issues/565))
- Add a connection timeout of 15 seconds for the XML import (used in the metadata refresh)
- Improve display of ARP in change requests
  ([#559](https://github.com/OpenConext/OpenConext-manage/issues/559))
- New metadata field `coin:application_name`
- Do not show error in policy form 
- Make sure policy names are unique, because otherwise the push to the PDP will break
  ([#558](https://github.com/OpenConext/OpenConext-manage/issues/558))
- Updated dependencies

## 9.4.0

- Improved dependency management in `pom.xml` files ([#538](https://github.com/OpenConext/OpenConext-manage/issues/538))
- Add 'Organisation' entry ([#535](https://github.com/OpenConext/OpenConext-manage/issues/535))
- Fix support for http proxies ([#530](https://github.com/OpenConext/OpenConext-manage/issues/530))
- Add all push data to push preview page ([#528](https://github.com/OpenConext/OpenConext-manage/issues/528))
- Add custom configuration hook to disallow IdPs with `coin:institution_brin` but no `coin:institution_brin_schac_home` (for
  [OpenConext-myconext#565](https://github.com/OpenConext/OpenConext-myconext/issues/565))
- Allow a PDP stepup policy with an AND condition between multiple values of the same attribute
  ([#544](https://github.com/OpenConext/OpenConext-Manage/issues/544))
- Add "IdP policies" that apply to all SPs except a named few
  ([#545](https://github.com/OpenConext/OpenConext-Manage/issues/545))
- Fill version and revision fiels in the recent activity API output
  ([#548](https://github.com/OpenConext/OpenConext-Manage/issues/548))
- Use `coin:institution_guid` instead of `coin:institution_id` for policy access
  ([#551](https://github.com/OpenConext/OpenConext-Manage/issues/551))
- Change label for SURFconext Invite roles in policies
  ([#552](https://github.com/OpenConext/OpenConext-Manage/issues/552))
- Add coin:policy_enforcement_decision_required flag for IdPs
  ([#553](https://github.com/OpenConext/OpenConext-Manage/issues/553))
- Add tab in policies tab to show all policies that are potentially conflicting
  ([#557](https://github.com/OpenConext/OpenConext-Manage/issues/557))

- Update dependencies

## 9.3.0

- Add a feature toggle to disable the strict "public RP must not have a secret" check that was introduced in 9.1.0
- Allow configuration of the maximum number of selected items in a multi-select enum field
- Add `application_tags` metadata field
- Add dedicated endpoint /internal/stats for stats
- Updated dependencies

### upgrade note

- add the following section in `parameters.yml`:

```yml
feature_toggles:
    allow_secret_public_rp: false
```

If set to `false` than the following checks are done:

- RPs that have `public_client: true` MUST not have a secret set;
- RPs that have `public_client: false` MUST have a secret set;
- RPs are configured to use the device code flow MUST not have a secret set.
  If set to `true`, these checks are disabled and Manage reverts to the old behaviour (as it was before 9.1.0).

## 9.2.0

- Add `/internal/protected/attributes/` (with trailing slash) as alias of `/internal/protected/attributes` (to fix IdP Dashbaord
  problems)
- Ensure `eid` is retained if metadata is updated
- Disallow removing entities that are used in existing
  policies ([#518](https://github.com/OpenConext/OpenConext-manage/issues/518))
- Allow URIs as LoA values, instead of only URLs
- Allow URLs in the contact information fields instead of only email addresses
- Add API test user  (for use with OpenConext Access Dashboard)
- Add `coin:is_test_idp` to IdP schema (for use with OpenConext Access Dashboard)
- Add `profile` to ARP; in the future this can record an 'ARP profile' (like 'anonymous', 'pseudonymous') to select a predefined
  ARP.
- Add `motivation` to ARP
- Add device_code to grants enum for SRAM services
- Prevent duplicate entityids, also between entities in different classes (e.g., `saml20_sp` and `sram`)
- Update dependencies

### upgrade notes

No changes to config files are required.

## 9.1.1

- Fixed the git info in the `/internal/info` API endpoint

## 9.1.0

### changes

- add features for [SBS](https://github.com/SURFscz/SBS)-integration.
    - add `coin:collab_enabled` flag to SPs
    - add schema for SBS services
    - add push for SBS services to Engineblock and OIDCng
    - update `parameters.yml`: add `push.eb.exclude_sram`
- Store encrypted SCIM bearer tokens in `scim_bearer_token` for
  Invite ([OpenConext-Invite#410](https://github.com/OpenConext/OpenConext-Invite/issues/410))
- Add `coin:defaultRAC` to IdP template ([#491](https://github.com/OpenConext/OpenConext-manage/pull/491)) (used in Engineblock
  6.17)
- Fix date format in error endpoint
- Add logic to make sure OIDC public clients do not set a secret and non-public clients
  do ([#495](https://github.com/OpenConext/OpenConext-manage/issues/495))
- Make the SAML-attribute from which super-admin memberships are read, configurable from
  application.yml ([#504](https://github.com/OpenConext/OpenConext-manage/issues/504))
- Make the attributes used in PDP policies configurable ([#505](https://github.com/OpenConext/OpenConext-manage/issues/505))
- Add ssh-key attribute
- Fix push-ok message in gui when OIDC and PDP are disabled ([#510](https://github.com/OpenConext/OpenConext-manage/issues/510)
- Automatically do a push when metadata is auto-refreshed ([#135](https://github.com/OpenConext/OpenConext-manage/issues/135))
- updated dependencies

### upgrade notes

The following changes to `parameter.yml` are necessary when upgrading to 9.1.0:

- add `push.eb.exclude_sram`: this controls whether to push `sram` entities to [SBS](https://github.com/SURFscz/SBS). If you
  don't use SBS, set to to `true`.
- add `security.super_user_attribute_name`: this controls which attribute to use for superuser authorization. If you want to
  keep the previous authorization method, set this to `is-member-of`
- rename `security.super_user_teams_names` to `security.super_user_values`
- add `policies.allowed_attributes`: this points to a file containing the allowed attributes in PDP policies. Point it to a
  file (e.g., `file:///opt/openconext/manage/attributes.json`) or set to `classpath:/policies/allowed_attributes.json` for the
  default behaviour.
- add `policies.extra_saml_attributes`: this points to a file containing extra attributes to be sent tot the PDP. Point it to a
  file (e.g., `file:///opt/openconext/manage/attributes.json`) or set to `classpath:/policies/extra_saml_attributes.json` for
  the default.

The stricter checks for RPs with `public_client: true` might prevent you from making changes to RPs which fail the stricter
check for defined secrets. If you have such RPs (i.e., with `public_client: true` and `secret` set, or with
`public_client: false` and no `secret` set), please consider upgrading to 9.3.0 instead.)

## 9.0.1

- Expanded "shibmb" index range from 0-9 to 0-30.
- Introduce multiple discovery-screen entries per IdP ([#457](https://github.com/OpenConext/OpenConext-manage/issues/457)).
  The changes are backward compatible with older Engineblock versions, but to useuse this feature, you need Enginblock 6.17.
    - allow indices >0 for logo entries (`logo:<index>:url`, `logo:<index>:width`, `logo:<index<:height`)
    - replace `keywords:<lang>` by `keywords:<index>:<lang>`
    - introduce `discoveryName:<index>:<lang>` replace the Name properties for the discovery screen
- Migrated to Spring Boot 3
- Be more strict in the return values of the push endpoints ([#132](https://github.com/OpenConext/OpenConext-manage/issues/132))
- Updated dependencies
