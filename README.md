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

Optional AI extraction variables:

```text
GEMINI_API_KEY
GEMINI_MODEL
```

`GEMINI_API_KEY`는 이미지 기반 어휘 추출 기능에서만 백엔드가 사용합니다.
프론트엔드로 전달하거나 Git에 커밋하지 마세요.

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
| `POST` | `/api/vocabularies/image/extract` | 이미지 기반 어휘 후보 추출 |
| `POST` | `/api/vocabularies/batch` | 검수 완료 어휘 일괄 저장 |
| `POST` | `/api/quizzes` | 퀴즈 생성 |
| `POST` | `/api/quizzes/submit` | 정답 제출 |
| `GET` | `/api/wrong-answers` | 오답 목록 조회 |
| `POST` | `/api/wrong-answers/quizzes` | 오답 복습 퀴즈 생성 |
| `DELETE` | `/api/wrong-answers/{id}` | 오답 개별 삭제 |
| `DELETE` | `/api/wrong-answers` | 오답 전체 삭제 |

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

## Admin Image Vocabulary Extraction

관리자 화면에서는 책이나 학습자료 이미지를 업로드해 AI가 어휘 후보를 추출하도록 할 수 있습니다.
이 기능은 AI가 DB 저장까지 자동으로 수행하지 않습니다.

처리 흐름:

1. 관리자 화면에서 이미지 업로드
2. 백엔드가 Gemini API로 `word`, `meaning`, `category`, `needsReview` 후보 추출
3. 추출 결과를 DB에 저장하지 않고 프론트 검수 화면에 표시
4. 관리자가 저장 여부, 단어, 뜻, 카테고리를 확인 및 수정
5. `검수 완료 및 저장` 버튼 클릭
6. 선택된 최종 데이터만 `/api/vocabularies/batch`로 저장

Supported image formats:

```text
jpg
jpeg
png
webp
```

Limits:

- 한 번에 최대 5장까지 업로드할 수 있습니다.
- 이미지 파일 1개는 5MB 이하여야 합니다.
- 업로드 MIME type은 `image/jpeg`, `image/png`, `image/webp`만 허용합니다.

AI category classification:

- `NATIVE_KOREAN`: 고유어
- `SINO_KOREAN`: 한자어
- `LOANWORD`: 외래어
- `PROVERB`: 속담
- `IDIOM`: 관용어

AI가 분류를 확신하기 어려운 항목은 `needsReview: true`로 표시됩니다.
그래도 DB 저장 시점에는 관리자가 최종적으로 위 카테고리 중 하나를 선택해야 합니다.

Extract candidates from images:

```bash
curl -X POST http://localhost:8080/api/vocabularies/image/extract \
  -F "files=@page-1.jpg" \
  -F "files=@page-2.png"
```

Extraction response example:

```json
{
  "totalCount": 1,
  "items": [
    {
      "imageNumber": 1,
      "rowNumber": 1,
      "word": "가람",
      "meaning": "강을 뜻하는 옛말",
      "category": "NATIVE_KOREAN",
      "needsReview": false,
      "confidence": 0.91
    }
  ]
}
```

Save reviewed candidates:

```bash
curl -X POST http://localhost:8080/api/vocabularies/batch \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "word": "가람",
        "meaning": "강을 뜻하는 옛말",
        "category": "NATIVE_KOREAN"
      }
    ]
  }'
```

저장 결과는 CSV 대량 등록과 동일하게 `success`, `skipped`, `failed`로 집계됩니다.
중복 기준은 `word + meaning + category`입니다.
이미 DB에 같은 항목이 있으면 오류가 아니라 `skipped`로 처리됩니다.

Failure cases shown to users:

- 잘못된 파일 형식
- 파일 크기 초과
- AI API 호출 실패
- 이미지에서 어휘를 찾지 못함
- AI 응답 형식 오류
- category 분류 실패
- 저장 validation 실패

This feature can incur Gemini API costs when images are analyzed.
Local MVP에서는 관리자 기능 보호가 없으므로, 공개 배포 전 인증/인가나 네트워크 접근 제한으로 관리자 화면과 API를 보호해야 합니다.

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
- 생성된 각 문제는 서버 메모리에 30분 동안 저장되는 `questionId`로 식별합니다.
- `optionId`는 Vocabulary ID가 아니라 해당 문제의 선택지를 검증하기 위한 임의 식별자입니다.
- 생성 응답에는 정답 option 정보가 노출되지 않습니다.
- 정답 제출 시 서버는 `questionId`가 실제 생성된 문제인지, 선택한 `optionId`가 해당 문제의 options에 포함되는지 검증합니다.
- 정답 여부는 표시 text가 아니라 서버가 저장한 정답 `optionId` 기준으로 판정합니다.

Create quiz response example:

```json
[
  {
    "questionId": "b2e4c407-67e0-4203-bb01-1306a5a790d1",
    "vocabularyId": 1,
    "mode": "WORD_TO_MEANING",
    "questionText": "사과",
    "options": [
      {
        "optionId": "81db62b4-4f66-4287-94d0-5b0e7564e353",
        "text": "grape"
      },
      {
        "optionId": "8eb9e671-d53f-4afd-a2db-6b904195a513",
        "text": "apple"
      },
      {
        "optionId": "f96ced35-8ea0-4a8b-b20f-bb7757a029b8",
        "text": "banana"
      },
      {
        "optionId": "567cbffd-00f8-4c31-a236-15083cb295f1",
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
    "questionId": "b2e4c407-67e0-4203-bb01-1306a5a790d1",
    "selectedOptionId": "8eb9e671-d53f-4afd-a2db-6b904195a513",
    "wrongAnswerReview": false
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
- `questionCount`가 출제 가능한 어휘 수보다 크면 생성할 수 없습니다.
- 같은 category 안에 선택지로 사용할 서로 다른 text가 4개 미만이면 생성할 수 없습니다.
- 존재하지 않는 category 또는 mode는 400 응답으로 처리됩니다.
- 생성되지 않았거나 만료된 `questionId`로 제출하면 400 응답으로 처리됩니다.
- 해당 문제의 options에 없는 `selectedOptionId`로 제출하면 400 응답으로 처리됩니다.
- 정답 제출 시 클라이언트의 정답 여부는 신뢰하지 않고 서버가 저장한 문제 세션을 기준으로 판정합니다.

## Wrong Answer Review

일반 퀴즈에서 오답을 제출하면 서버가 정답 판정 결과를 기준으로 해당 Vocabulary를 오답 목록에 자동 저장합니다.
클라이언트가 보낸 정답 여부 값은 사용하지 않습니다.

Wrong answer storage rules:

- 오답은 `vocabularyId + quizMode` 조합으로 관리합니다.
- 같은 `vocabularyId + quizMode` 오답이 이미 있으면 새 row를 만들지 않고 `wrongCount`를 1 증가시키고 `lastWrongAt`을 갱신합니다.
- 일반 퀴즈에서 정답을 맞힌 경우 기존 오답 목록은 변경하지 않습니다.
- 오답 복습에서 정답을 맞히면 해당 `vocabularyId + quizMode` 오답 데이터를 삭제합니다.
- 오답 복습에서 다시 틀리면 일반 오답과 동일하게 `wrongCount`를 증가시키고 `lastWrongAt`을 갱신합니다.
- 현재 정책은 오답 복습에서 1회 정답 시 제거입니다.

List wrong answers:

```bash
curl http://localhost:8080/api/wrong-answers
```

Wrong answer response example:

```json
[
  {
    "id": 1,
    "vocabularyId": 10,
    "word": "가람",
    "meaning": "강",
    "category": "NATIVE_KOREAN",
    "quizMode": "WORD_TO_MEANING",
    "wrongCount": 2,
    "lastWrongAt": "2026-08-21T10:30:00"
  }
]
```

Create wrong answer review quiz:

```bash
curl -X POST http://localhost:8080/api/wrong-answers/quizzes \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "WORD_TO_MEANING",
    "questionCount": 4
  }'
```

오답 복습 퀴즈는 실제 문제로 출제되는 Vocabulary만 오답 목록에서 가져옵니다.
오답 선택지 distractor는 해당 Vocabulary와 같은 category의 전체 Vocabulary에서 가져오기 때문에,
오답이 1개뿐이어도 같은 category에 충분한 어휘와 고유한 선택지 text가 있으면 복습할 수 있습니다.

Submit a review answer:

```bash
curl -X POST http://localhost:8080/api/quizzes/submit \
  -H "Content-Type: application/json" \
  -d '{
    "questionId": "63cf230f-2099-4be0-b27a-6b27d808ac1e",
    "selectedOptionId": "33333095-3f48-49f1-bc47-31ae23698a0b",
    "wrongAnswerReview": true
  }'
```

Delete wrong answers:

```bash
curl -X DELETE http://localhost:8080/api/wrong-answers/1
curl -X DELETE http://localhost:8080/api/wrong-answers
```

Wrong answer review error cases:

- 오답 목록이 없으면 복습 퀴즈를 생성할 수 없습니다.
- 오답 Vocabulary와 같은 category 전체에서도 4개의 고유한 선택지 text를 만들 수 없으면 생성할 수 없습니다.
- `questionCount`가 출제 가능한 오답 어휘 수보다 크면 생성할 수 없습니다.

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
- Frontend quiz UI
- Wrong-answer storage and review

Not implemented yet:

- User accounts
- Authentication or authorization
- AWS resource creation or deployment
