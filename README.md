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
| `POST` | `/api/quizzes` | 퀴즈 생성 |
| `POST` | `/api/quizzes/submit` | 정답 제출 |

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
NATIVE_KOREAN
SINO_KOREAN
LOANWORD
PROVERB
IDIOM
GENERAL
NOUN
VERB
ADJECTIVE
ADVERB
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
가람,강,NATIVE_KOREAN
간과하다,대충 보아 넘기다,SINO_KOREAN
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
      "reason": "category must be one of [NATIVE_KOREAN, SINO_KOREAN, LOANWORD, PROVERB, IDIOM, GENERAL, NOUN, VERB, ADJECTIVE, ADVERB]"
    }
  ]
}
```

## Quiz API

Quiz modes:

```text
WORD_TO_MEANING
MEANING_TO_WORD
```

`WORD_TO_MEANING`은 단어를 보고 뜻을 고르는 방식입니다.
`MEANING_TO_WORD`는 뜻을 보고 단어를 고르는 방식입니다.

Create quiz:

```bash
curl -X POST http://localhost:8080/api/quizzes \
  -H "Content-Type: application/json" \
  -d '{
    "category": "NATIVE_KOREAN",
    "mode": "WORD_TO_MEANING",
    "questionCount": 2
  }'
```

Quiz generation rules:

- `category`는 `VocabularyCategory` 값 중 하나여야 합니다.
- 프론트엔드 기본 퀴즈 화면은 `NATIVE_KOREAN`, `SINO_KOREAN`, `LOANWORD`, `PROVERB`, `IDIOM`을 고유어, 한자어, 외래어, 속담, 관용어로 표시합니다.
- `questionCount`는 1 이상이어야 합니다.
- 선택한 category의 어휘에서 랜덤으로 문제를 생성합니다.
- 같은 퀴즈 세트 안에서 동일한 `vocabularyId`는 중복 출제되지 않습니다.
- 각 문제는 정답 1개와 오답 3개를 포함한 4지선다입니다.
- 오답은 같은 category의 다른 Vocabulary에서 가져옵니다.
- 같은 선택지 text가 중복되어 정답이 여러 개처럼 보이지 않도록 생성합니다.
- 선택지 순서는 매번 랜덤으로 섞입니다.
- 퀴즈 문제 자체는 DB에 저장하지 않고 Vocabulary 데이터를 기반으로 동적으로 생성합니다.
- 생성 응답에는 정답이 노출되지 않습니다.

Create quiz response example:

```json
[
  {
    "vocabularyId": 1,
    "mode": "WORD_TO_MEANING",
    "questionText": "사과",
    "options": [
      {
        "optionId": 3,
        "text": "grape"
      },
      {
        "optionId": 1,
        "text": "apple"
      },
      {
        "optionId": 2,
        "text": "banana"
      },
      {
        "optionId": 4,
        "text": "strawberry"
      }
    ]
  }
]
```

Submit answer:

```bash
curl -X POST http://localhost:8080/api/quizzes/submit \
  -H "Content-Type: application/json" \
  -d '{
    "vocabularyId": 1,
    "mode": "WORD_TO_MEANING",
    "selectedOptionId": 1
  }'
```

Submit answer response example:

```json
{
  "correct": true,
  "correctAnswer": "apple",
  "vocabularyId": 1
}
```

Quiz error cases:

- 선택한 category의 어휘가 4개 미만이면 4지선다 퀴즈를 생성할 수 없습니다.
- `questionCount`가 선택한 category의 전체 어휘 수보다 크면 생성할 수 없습니다.
- 같은 category 안에 선택지로 사용할 서로 다른 text가 4개 미만이면 생성할 수 없습니다.
- 존재하지 않는 category 또는 mode는 400 응답으로 처리됩니다.
- 정답 제출 시 클라이언트의 정답 여부는 신뢰하지 않고 서버의 Vocabulary 데이터를 기준으로 판정합니다.

## Current Scope

Implemented in this setup:

- Basic Spring Boot application
- Basic React + Vite application
- Dockerfiles for backend and frontend
- Docker Compose configuration for frontend, backend, and MySQL
- Environment variable based database configuration
- Vocabulary entity and CRUD API
- Vocabulary CSV upload
- Quiz generation API
- Quiz answer submission API

Not implemented yet:

- Frontend quiz UI
- Wrong-answer storage or review
- User accounts
- Authentication or authorization
- AWS resource creation or deployment
