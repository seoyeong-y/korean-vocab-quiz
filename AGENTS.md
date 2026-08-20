# Project Guidelines

## Branch Strategy

- `main`: 실제 배포 가능한 안정 버전입니다.
- `develop`: 개발 내용을 통합하는 브랜치입니다.
- `feat/#<issue-number>`: 기능 개발 브랜치입니다.
- `fix/#<issue-number>`: 버그 수정 브랜치입니다.

The initial project setup can exist on both `main` and `develop` as the shared repository foundation.
After this setup, do not develop feature code directly on `main` or `develop`.

## Issue And PR Workflow

For meaningful feature work, project configuration work after the initial setup, and bug fixes:

1. Create a GitHub Issue before starting work.
2. Create a working branch from `develop` using the Issue number.
3. Use `feat/#<issue-number>` for feature work.
4. Use `fix/#<issue-number>` for bug fixes.
5. Commit only on the working branch.
6. Push the working branch to origin.
7. Open a Pull Request with `develop` as the base branch.
8. Link the PR to the Issue in the PR body.
9. Do not merge the PR; the repository owner reviews and merges.

Do not append a feature name or description to Issue based branch names.

Only create a `develop` to `main` Pull Request when preparing an actual deployment.
Very small typo fixes may be handled without a separate Issue.

## Commit Messages

Use Conventional Commits for every commit in this repository.

Format:

```text
<type>: <description>
```

Allowed types:

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `refactor`: 기능 변경 없는 코드 구조 개선
- `test`: 테스트 추가 또는 수정
- `docs`: 문서 변경
- `chore`: 프로젝트 설정, 빌드, 의존성, 개발환경 등 기타 작업

Descriptions should be concise Korean text.

Examples:

- `chore: 프로젝트 초기 구조 설정`
- `feat: 어휘 CRUD API 추가`
- `feat: 퀴즈 생성 기능 추가`
- `fix: 어휘 중복 등록 오류 수정`
- `test: 어휘 서비스 테스트 추가`
- `docs: 로컬 실행 방법 문서화`
