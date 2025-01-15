# OpenConext-manage
[![Build Status](https://travis-ci.org/OpenConext/OpenConext-manage.svg)](https://travis-ci.org/OpenConext/OpenConext-manage)
[![codecov.io](https://codecov.io/github/OpenConext/OpenConext-manage/coverage.svg)](https://codecov.io/github/OpenConext/OpenConext-manage)

Stores and publishes metadata of all entities known to OpenConext

## Disclaimer

### PdP integration

The 8.x.x version of Manage contains functionality to import policies from PdP. This functionality has been removed in 
9.0.0. Before migrating to Manage 9.0.0 ensure that all PdP policies are migrated.

### Janus

Note that the original migration from Janus was removed in version 3.0.0. If you want to run the migrations from a Janus
 database then install version 2.0.8.

## [Getting started](#getting-started)

### [System Requirements](#system-requirements)

- Java 21
- Maven 3
- MongoDB
- yarn
- Node v20.8.0 (best managed with `nvm`, current version in [.nvmrc](manage-gui/.nvmrc))
- ansible

If you have installed MongoDB with your package manager, you'll have to enable transactions:

```
mongosh
> rs.initiate()
```

## [Building and running](#building-and-running)

### [The manage-server](#manage-server)

This project uses Spring Boot and Maven. To run locally, type:

`cd manage-server`

`mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=dev"`

When developing, it's convenient to just execute the applications main-method, which is in [Application](manage-server/src/main/java/manage/Application.java). Don't forget
to set the active profile to dev.

### [The manage-gui](#manage-gui)

The client is build with react and to get initially started:

```
cd manage-gui
yarn install
yarn start
```
See https://github.com/facebook/create-react-app/issues/11762#issuecomment-1002285279 for troubleshooting the proxy settings.

Browse to the [application homepage](http://localhost:3000/).

To add new dependencies:

`yarn add package --dev`

When new yarn dependencies are added:

`yarn install`

To run all JavaScript tests:
```
cd client
yarn test
```
Or to run all the tests and do not watch - like CI:
```
cd client
CI=true yarn test
```

### [Wiki](#wiki)

See the Manage [github wiki](https://github.com/OpenConext/OpenConext-manage/wiki) for
additional documentation.

### [New MetaData Type](#new-metadata-type)

New MetaData types must first be described in JSON Schema and the schema must be placed in src/main/resources/metadata_configuration. This
is all that is required for the server. Of course support for CRUD in the GUI needs to be coded.     

### [Configuration and deployment](#configuration-and-deployment)

On its classpath, the application has an [application.yml](manage-server/src/main/resources/application.yml) file that
contains configuration defaults that are convenient when developing.

When the application actually gets deployed to a meaningful platform, it is pre-provisioned with ansible and the application.yml depends on
environment specific properties in the group_vars. See the project OpenConext-deploy and the role `manage` for more information.

For details, see the [Spring Boot manual](https://docs.spring.io/spring-boot/docs/1.2.1.RELEASE/reference/htmlsingle/).

#### [Optional configuration](#optional-configuration)

The following configurations are not shown in the application.yml file but are available:

| Configuration             | Description                                                                                    | Default value |
|:--------------------------|:-----------------------------------------------------------------------------------------------|:--------------|
| metadata_import.useragent | Set a custom (String) value for the User-Agent header used in SAML metadata retrieval requests | null          |

### [MetaData Import](#metadata-import)

When you want to import existing metaData into your local mongodb you can use the following command:
```
mongoimport --db metadata --collection saml20_idp --type json --file identity-providers.json --jsonArray
```

### Change requests
```
cd ./manage-server/src/test/resources/json
curl -u sysadmin:secret -X POST -H 'Content-Type: application/json' -d '@change_request.json' 'https://manage.test2.surfconext.nl/manage/api/internal/change-requests'
```
Or the other supported flavour: an incremental change
```
curl -u sp-portal:secret -X POST -H 'Content-Type: application/json' -d '@incremental_change_request.json' 'https://manage.test2.surfconext.nl/manage/api/internal/change-requests'
curl -u sp-portal:secret -X POST -H 'Content-Type: application/json' -d '@incremental_change_request.json' 'http://localhost:8080/manage/api/internal/change-requests'
```
 