# Tracked-Solutions

This repository is my workspace for two areas of ongoing development:

- **Daily coding solutions** — a place to track the problems I solve, keep readable implementations, and record useful approaches as I practice.
- **Backend development** — a place to build and document backend projects, including their APIs, data stores, and local development setup.

The goal is to keep my problem-solving work consistent and make my backend projects easy to run and understand.

## Repository contents

```text
Solutions/       Daily coding problem solutions
compose.yaml     Local PostgreSQL, MySQL, and Redis services
.env.example    Example local service configuration
Brewfile         macOS development tools
```

As the repository grows, each solution and backend project will include the context needed to understand or run it.

## Local development environment (macOS)

This repository includes a repeatable setup for macOS. It installs a JDK, VS Code, Docker Desktop, and Postman with Homebrew, and runs PostgreSQL, MySQL, and Redis in Docker. Java 17 matches the `spring-petclinic-microservices` project in the workspace.

### Install desktop tools and local CLIs

Install [Homebrew](https://brew.sh/) if it is not already installed, then run:

```sh
brew bundle
```

To use IntelliJ IDEA instead of VS Code, replace `visual-studio-code` with `intellij-idea` in `Brewfile` before running the command.

Open Docker Desktop and wait for it to finish starting. Then check the JDK and Docker tools:

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

PostgreSQL and MySQL are alternatives for most applications; both are available so each project can use the database that fits. These credentials are for local development only. Change the host ports in `.env` if they are already in use.

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
