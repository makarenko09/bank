# PaatoM Bank_REST Repository

## Prerequisites

- JavaLanguageVersion.of(25) - for example, with SDKMAN:

1. Install SDKMAM,
   Next step: source bashrc, restart sh
2. Put next and sourse your shell:

```bash
#THIS MUST BE AT THE END OF THE FILE FOR SDKMAN TO WORK!!

export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]] && source "$SDKMAN_DIR/bin/sdkman-init.sh"" >> ~/.bashrc
```

- Docker

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
docker compose -f docker-compose.yml up -d
```

- See as work KeyCloak App: [...2026-02-23 17:50:28,511 INFO [io.quarkus] (main) Keycloak 26.5.3 on JVM (powered by Quarkus 3.27.2) started in 10.866s. Listening on: http://0.0.0.0:9081.⁠ Management interface listening on http://0.0.0.0:9000 ...\]](http://localhost:9081)

<!-- seed4j-needle-startupCommand -->

## Documentation

- [Hexagonal architecture](documentation/hexagonal-architecture.md)
- [Package types](documentation/package-types.md)
- [Assertions](documentation/assertions.md)
- [PostgreSQL](documentation/postgresql.md)
- [Logs Spy](documentation/logs-spy.md)
- [CORS configuration](documentation/cors-configuration.md)

<!-- seed4j-needle-documentation -->

## FAQ

###### \- API doc:

Открой в браузере:
http://localhost:8080/swagger-ui/index.html
или http://localhost:8080/swagger-ui.html

> DEBUG
> Для проверки спецификации: http://localhost:8080/v3/api-docs
