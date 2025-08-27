# Release notes

Starting from version 9.0.0, we note changes and new features per release in this file.

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
    allow_secret_public_rp: true
```

If set to `true` than the following checks are done:

- RPs that have `public_client: true` MUST not have a secret set;
- RPs that have `public_client: false` MUST have a secret set;
- RPs are configured to use the device code flow MUST not have a secret set.
  If set to `false`, these checks ar edisabled and Manage reverts to the old behaviour (as it was before 9.1.0).

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
