# Tracked-Solutions

## Local development environment (macOS)

This repository includes a repeatable setup for macOS. It installs a JDK, VS Code, Docker Desktop, and Postman with Homebrew, and runs PostgreSQL, MySQL, and Redis in Docker. Java 17 matches the `spring-petclinic-microservices` project in the workspace.

### Install the desktop tools and local CLIs

Install [Homebrew](https://brew.sh/) if it is not already installed, then from the repository root run:

```sh
brew bundle
```

This uses `Brewfile`. To use IntelliJ IDEA instead of VS Code, replace `visual-studio-code` with `intellij-idea` in that file before running the command.

Open Docker Desktop once and wait for it to finish starting:

```sh
open -a Docker
```

Check the JDK and Docker tools:

```sh
java -version
docker --version
docker compose version
```

### Start PostgreSQL, MySQL, and Redis

```sh
cp .env.example .env
docker compose up -d
docker compose ps
```

Connection details with the default `.env` values:

| Service | Address | Database | Username | Password |
| --- | --- | --- | --- | --- |
| PostgreSQL | `localhost:5432` | `app` | `app` | `localdev` |
| MySQL | `localhost:3306` | `app` | `app` | `localdev` |
| Redis | `localhost:6379` | — | — | — |

PostgreSQL and MySQL are alternatives for most applications; both are included here so you can choose per project. These credentials are only for local development. Change the host ports in `.env` if they are already in use.

### Useful commands

```sh
docker compose logs -f                 # Follow service logs
docker compose down                    # Stop services; preserve database data
docker compose down -v                 # Stop services and delete database data
psql 'postgresql://app:localdev@localhost:5432/app'
mysql -h 127.0.0.1 -P 3306 -u app -plocaldev app
redis-cli -h localhost -p 6379 ping    # Expect PONG
```

Use Postman to send requests to your local application, typically at `http://localhost:<app-port>`. Open this folder in VS Code with `code .` after installation.
