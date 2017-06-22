# OpenConext-metadata-registry
[![Build Status](https://travis-ci.org/OpenConext/OpenConext-metadata-repository.svg)](https://travis-ci.org/OpenConext/OpenConext-metadata-repository)
[![codecov.io](https://codecov.io/github/OpenConext/OpenConext-metadata-repository/coverage.svg)](https://codecov.io/github/OpenConext/OpenConext-metadata-repository)

Stores and publishes metadata of all entities known to OpenConext

## [Getting started](#getting-started)

### [System Requirements](#system-requirements)

- Java 8
- Maven 3
- MongoDB 3.4.4
- yarn
- create-react-app

## [Building and running](#building-and-running)

### [The mr-server](#mr-server)

This project uses Spring Boot and Maven. To run locally, type:

`cd mr-server`

`mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=dev"`

When developing, it's convenient to just execute the applications main-method, which is in [Application](aa-server/src/main/java/mr/Application.java). Don't forget
to set the active profile to dev.

### [The mr-gui](#mr-gui)

The client is build with react.js and to get initially started:

`cd aa-gui`
`yarn install -g create-react-app` 
`brew install yarn;`

To run locally:

`yarn local`

Browse to the [application homepage](http://localhost:8000/).

When new npm dependencies are added:

`yarn install`

## [Miscellaneous](#miscellaneous)

### [Testing](#testing)

### [New MetaData Type](#new-metadata-type)

New MetaData types must first be described in JSON Schema and the schema must be places in... 

### [Configuration and deployment](#configuration-and-deployment)

On its classpath, the application has an [application.yml](mr-server/src/main/resources/application.yml) file that
contains configuration defaults that are convenient when developing.

When the application actually gets deployed to a meaningful platform, it is pre-provisioned with ansible and the application.yml depends on
environment specific properties in the group_vars. See the project OpenConext-deploy and the role mr for more information.

For details, see the [Spring Boot manual](http://docs.spring.io/spring-boot/docs/1.2.1.RELEASE/reference/htmlsingle/).