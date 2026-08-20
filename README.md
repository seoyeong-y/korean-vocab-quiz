# KBS Korean Vocabulary Quiz

KBS한국어능력시험 대비용 개인 어휘 학습 웹사이트입니다. 현재 PR 1 단계에서는 기능 구현이 아니라 Spring Boot, React, MySQL, Docker Compose 기반의 프로젝트 실행 환경만 구성합니다.

## Tech Stack

- Backend: Java 17, Spring Boot 3.5.16, Maven
- Frontend: React 19, Vite
- Database: MySQL 8.4
- Container: Docker, Docker Compose
- Version Control: Git, GitHub

Java 17은 LTS 버전이며 Spring Boot 3.x와 안정적으로 사용할 수 있어 개인 프로젝트와 Docker 기반 EC2 배포에 적합합니다.

## Project Structure

```text
korean-vocab-quiz/
  backend/              # Spring Boot application
  frontend/             # React + Vite application
  docker-compose.yml    # frontend, backend, mysql local environment
  .env.example          # environment variable template
  .gitignore
  README.md
```

## Ports

| Service | Port |
| --- | --- |
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| MySQL | localhost:3306 |

## Environment Variables

Create a local `.env` file from the example file.

```bash
cp .env.example .env
```

Update the values in `.env` for your local environment. Do not commit `.env`.

Required variables:

```text
MYSQL_DATABASE
MYSQL_USER
MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD
SPRING_PROFILES_ACTIVE
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
APP_CORS_ALLOWED_ORIGIN
VITE_API_BASE_URL
```

## Run With Docker Compose

```bash
docker compose up --build
```

This starts:

- `frontend`
- `backend`
- `mysql`

MySQL data is stored in the named Docker volume `mysql-data`, so database data remains after container restart.

## Stop

```bash
docker compose down
```

To also remove the MySQL volume:

```bash
docker compose down -v
```

## Local Build Commands

Backend build using Docker:

```bash
docker build -t korean-vocab-backend ./backend
```

Frontend build:

```bash
cd frontend
npm install
npm run build
```

## Current Scope

Implemented in this setup:

- Basic Spring Boot application
- Basic React + Vite application
- Dockerfiles for backend and frontend
- Docker Compose configuration for frontend, backend, and MySQL
- Environment variable based database configuration

Not implemented yet:

- Vocabulary entity or CRUD
- CSV upload
- Quiz generation
- Answer checking
- Wrong-answer storage or review
- User accounts
- Authentication or authorization
- AWS resource creation or deployment
