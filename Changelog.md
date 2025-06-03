# Release notes

Starting from version 9.0.0, we note changes and new features per release in this file.


## 9.1

- add features for [SBS](https://github.com/SURFscz/SBS)-integration.
  - add `coin:collab_enabled` flag to SPs
  - add schema for SBS services
  - add push for SBS services to Engineblock and OIDCng
  - update `parameters.yml`: add `push.eb.exclude_sram`
- Store encrypted SCIM bearer tokens in `scim_bearer_token` for Invite ([OpenConext-Invite#410](https://github.com/OpenConext/OpenConext-Invite/issues/410))
- Add `coin:defaultRAC` to IdP template ([#491](https://github.com/OpenConext/OpenConext-manage/pull/491)) (used in Engineblock 6.17)
- Fix date format in error endpoint
- Add logic to make sure OIDC public clients do not set a secret and non-public clients do ([#495](https://github.com/OpenConext/OpenConext-manage/issues/495))
- Make the SAML-attribute from which super-admin memberships are read, configurable from application.yml ([#504](https://github.com/OpenConext/OpenConext-manage/issues/504))
  - update `parameters.yml`:
    - replace `security.super_user_teams_names` by `security.super_user_attribute_name` and `security.super_user_values`
- Make the attributes used in PDP policies configurable ([#505](https://github.com/OpenConext/OpenConext-manage/issues/505))
  - update `parameters.yml`:
    - add `policies.allowed_attributes` and `policies.extra_saml_attributes`
- Add ssh-key attribute 
- Fix push-ok message in gui when OIDC and PDP are disabled ([#510](https://github.com/OpenConext/OpenConext-manage/issues/510)
- Automatically do a push when metadata is auto-refreshed ([#135](https://github.com/OpenConext/OpenConext-manage/issues/135))
- updated dependencies
    
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
