# PaatoM Bank_REST Repository

## Prerequisites

- JavaLanguageVersion.of(25) - for example:

```bash
sdk env install java 25.0.2.r25-nik;
sdk default java <VERSION>
```
Next: source bashrc, restart sh

- Docker:

```bash
docker compose -f src/main/docker/postgresql.yml up -d
```
### Node.js and NPM

Before you can build this project, you must install and configure the following dependencies on your machine:

[Node.js](https://nodejs.org/): We use Node to run a development web server and build the project.
Depending on your system, you can install Node either from source or as a pre-packaged bundle.

After installing Node, you should be able to run the following command to install development tools.
You will only need to run this command when dependencies change in [package.json](package.json).

```
npm install
```

## Local environment

- [Local server](http://localhost:8080)
- [Local API doc](http://localhost:8080/swagger-ui.html)

<!-- seed4j-needle-localEnvironment -->

## Start up

```bash
docker compose -f src/main/docker/postgresql.yml up -d
```

```bash
docker compose -f src/main/docker/keycloak.yml up -d
```


<!-- seed4j-needle-startupCommand -->

## Documentation

- [Hexagonal architecture](documentation/hexagonal-architecture.md)
- [Package types](documentation/package-types.md)
- [Assertions](documentation/assertions.md)
- [PostgreSQL](documentation/postgresql.md)
- [Logs Spy](documentation/logs-spy.md)
- [CORS configuration](documentation/cors-configuration.md)

<!-- seed4j-needle-documentation -->
