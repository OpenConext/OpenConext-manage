# OpenConext-manage
[![Build Status](https://travis-ci.org/OpenConext/OpenConext-manage.svg)](https://travis-ci.org/OpenConext/OpenConext-manage)
[![codecov.io](https://codecov.io/github/OpenConext/OpenConext-manage/coverage.svg)](https://codecov.io/github/OpenConext/OpenConext-manage)

Stores and publishes metadata of all entities known to OpenConext

## [Getting started](#getting-started)

### [System Requirements](#system-requirements)

- Java 8
- Maven 3
- MongoDB 3.4.4
- yarn
- NodeJS 7.10.0 (best managed with `nvm`, current version in [.node-version](mr-gui/.node-version) or do `brew switch node 7.10.0`)
- create-react-app

## [Building and running](#building-and-running)

### [The manage-server](#manage-server)

This project uses Spring Boot and Maven. To run locally, type:

`cd manage-server`

`mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=dev"`

When developing, it's convenient to just execute the applications main-method, which is in [Application](manage-server/src/main/java/mr/Application.java). Don't forget
to set the active profile to dev.

### [The mr-gui](#mr-gui)

The client is build with react.js and to get initially started:

`cd aa-gui`
`brew install yarn;`
`yarn install -g create-react-app` 


To run locally:

`yarn start`

Browse to the [application homepage](http://localhost:3000/).

To add new dependencies:

`yarn add package --dev`

When new yarn dependencies are added:

`yarn install`

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
environment specific properties in the group_vars. See the project OpenConext-deploy and the role mr for more information.

For details, see the [Spring Boot manual](http://docs.spring.io/spring-boot/docs/1.2.1.RELEASE/reference/htmlsingle/).