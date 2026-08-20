# KBS Korean Vocabulary Quiz

KBS한국어능력시험 대비용 개인 어휘 학습 웹사이트입니다.

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
| MySQL | localhost:3307 |

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
MYSQL_PORT
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

Backend test using Docker:

```bash
docker run --rm -v "$PWD/backend":/app -w /app maven:3.9-eclipse-temurin-17 mvn test
```

Frontend build:

```bash
cd frontend
npm install
npm run build
```

## Vocabulary API

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/vocabularies` | 어휘 생성 |
| `GET` | `/api/vocabularies` | 어휘 목록 조회 |
| `GET` | `/api/vocabularies/{id}` | 어휘 단건 조회 |
| `PUT` | `/api/vocabularies/{id}` | 어휘 수정 |
| `DELETE` | `/api/vocabularies/{id}` | 어휘 삭제 |
| `POST` | `/api/vocabularies/csv` | CSV 어휘 대량 등록 |

Request body:

```json
{
  "word": "가교",
  "meaning": "둘 사이를 이어 주는 것",
  "category": "GENERAL",
  "exampleSentence": "그는 양국 협력의 가교 역할을 했다."
}
```

`category`를 생략하면 `GENERAL`로 저장됩니다.

Available categories:

```text
GENERAL
NOUN
VERB
ADJECTIVE
ADVERB
IDIOM
```

## Vocabulary CSV Upload

CSV upload endpoint:

```bash
curl -X POST http://localhost:8080/api/vocabularies/csv \
  -F "file=@vocabularies.csv"
```

CSV format:

```csv
word,meaning,category
가교,둘 사이를 이어 주는 것,GENERAL
간과하다,대충 보아 넘기다,VERB
```

Required columns:

- `word`
- `meaning`
- `category`

Processing rules:

- New valid rows are saved and counted as `success`.
- Rows with the same `word`, `meaning`, and `category` as existing DB data are not errors. They are counted as `skipped`.
- Duplicate rows in the same CSV upload are counted as `skipped`.
- Rows missing `word`, `meaning`, or `category` are counted as `failed`.
- Rows with a `category` that is not in `VocabularyCategory` are counted as `failed`.
- Each failed row includes `rowNumber` and `reason`.
- Skipped rows also include `rowNumber` and `reason`.
- `successCount + skippedCount + failedCount = totalCount`.

Response example:

```json
{
  "totalCount": 4,
  "successCount": 1,
  "skippedCount": 1,
  "failedCount": 2,
  "skippedRows": [
    {
      "rowNumber": 2,
      "reason": "Already exists with the same word, meaning, and category."
    }
  ],
  "failedRows": [
    {
      "rowNumber": 3,
      "reason": "word is required"
    },
    {
      "rowNumber": 4,
      "reason": "category must be one of [GENERAL, NOUN, VERB, ADJECTIVE, ADVERB, IDIOM]"
    }
  ]
}
```

## Current Scope

Implemented in this setup:

- Basic Spring Boot application
- Basic React + Vite application
- Dockerfiles for backend and frontend
- Docker Compose configuration for frontend, backend, and MySQL
- Environment variable based database configuration
- Vocabulary entity and CRUD API
- Vocabulary CSV upload

Not implemented yet:

- Quiz generation
- Answer checking
- Wrong-answer storage or review
- User accounts
- Authentication or authorization
- AWS resource creation or deployment
