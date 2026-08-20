# Project Guidelines

## Branch Strategy

- `main`: 실제 배포 가능한 안정 버전입니다.
- `develop`: 개발 내용을 통합하는 브랜치입니다.
- `feat/#<issue-number>`: 기능 개발 브랜치입니다.
- `fix/#<issue-number>`: 버그 수정 브랜치입니다.
- `refactor/#<issue-number>`: 리팩토링 브랜치입니다.
- `design/#<issue-number>`: 디자인 또는 문장 수정 브랜치입니다.
- `chore/#<issue-number>`: 빌드, 패키지, 환경변수 등 운영 설정 브랜치입니다.

The initial project setup can exist on both `main` and `develop` as the shared repository foundation.
After this setup, do not develop feature code directly on `main` or `develop`.

Do not append a feature name or description to Issue based branch names.
When using branch names in shell commands, quote them when needed, for example `git checkout 'feat/#1'`, so `#` is not treated as a shell comment.

## Issue Templates

Use the GitHub Issue Templates in `.github/ISSUE_TEMPLATE/`.
Choose the template that matches the work type.

Allowed Issue types:

- `feat`: 새로운 기능 추가 또는 기능 업데이트
- `fix`: 버그 또는 에러 수정
- `refactor`: 코드 리팩토링
- `design`: 디자인 또는 문장 수정
- `chore`: 빌드 수정, 패키지 추가, 환경변수 설정 등

Issue titles must use this format:

```text
[type] 한국어 제목
```

The `type` must be lowercase.

Examples:

- `[feat] 어휘 CRUD API 구현`
- `[fix] Docker 실행 오류 수정`
- `[refactor] 퀴즈 생성 로직 개선`
- `[design] 퀴즈 화면 UI 수정`
- `[chore] 배포 환경 설정`

Issue body rules:

- `feat`: Write the implementation checklist under `✨ 구현 할 기능`.
- `fix`: Write symptoms and error logs, when needed, under `🤔 오류 내용`.
- `refactor`: Write the refactoring scope under `✨ 리팩토링 할 부분`.
- `design`: Write the publishing/design checklist under `퍼블리싱 할 내용`.
- `chore`: Write environment or setup changes under `✨ 세팅할 환경`.
- `📕 레퍼런스` is optional. Include it only when real references exist.
- Do not invent references.
- Replace template placeholders, empty checkboxes, and example text with the actual task details, or remove them if they are not needed.

## Issue And PR Workflow

For meaningful `feat`, `fix`, `refactor`, `design`, and `chore` work:

1. Create a GitHub Issue using the matching Issue type and template.
2. Confirm the actual Issue number.
3. Update `develop` from `origin/develop`.
4. Create a working branch from `develop` using `<type>/#<issue-number>`.
5. Implement the change.
6. Run tests and verification.
7. If tests fail, fix the cause and verify again.
8. Commit using Conventional Commits with a Korean description.
9. Push the working branch to origin.
10. Open a Pull Request with `develop` as the base branch.
11. Use `.github/pull_request_template.md` for the PR body.
12. Connect the Issue in the PR body with `closed #<issue-number>`.
13. Check required CI and test results.
14. If all verification is normal, mark the PR Ready for review.
15. Squash merge the PR into `develop`.
16. Confirm merge success.
17. Confirm the Issue is closed. If GitHub does not close it automatically, close it after confirming the merge.
18. Check out local `develop`.
19. Pull the latest `origin/develop`.
20. Delete the merged working branch locally and remotely.

If Issue creation fails, do not create a temporary Issue number or temporary branch. Stop the work and report the failure.

Very small typo fixes may be handled without a separate Issue.

## Pull Request Rules

All Pull Requests must use `.github/pull_request_template.md`.

PR bodies must keep this structure:

```markdown
## 📑 PR 요약

## 🔗 관련 이슈

## ☑ 작업 내용

## 📣 공유사항
```

For regular feature or fix work, write the related Issue as:

```text
closed #<issue-number>
```

Replace template examples such as `작업 1`, `작업 2`, `공유 사항 1`, and `공유 사항 2` with real content.
If there is nothing special to share, write `없음`.

## Automatic Merge Rules

For `feat`, `fix`, `refactor`, `design`, and `chore` Pull Requests targeting `develop`, Codex may automatically mark the PR Ready for review and squash merge it without additional user approval when all conditions are satisfied.

Stop and report to the user instead of automatically merging in these cases:

- Tests fail and the failure cannot be resolved.
- A merge conflict occurs.
- An unexpected existing behavior change is found.
- Sensitive information or a security issue is found.
- A destructive migration or data loss risk is found.
- The work affects the `main` branch.
- Requirements are unclear and an important implementation decision is needed.
- Any other serious issue makes automatic merge unsafe.

## Main Branch Rules

`main` manages the stable deployable version.

Do not commit, push, or merge directly to `main` during regular feature development.
Create a `develop` to `main` Pull Request only when the user explicitly requests a deployment.
Never automatically merge a `develop` to `main` Pull Request.

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
- `design`: 디자인 또는 문장 수정
- `test`: 테스트 추가 또는 수정
- `docs`: 문서 변경
- `chore`: 프로젝트 설정, 빌드, 의존성, 개발환경 등 기타 작업

Descriptions should be concise Korean text.
Use a commit type that matches the Issue type and work character.

Examples:

- `chore: 프로젝트 초기 구조 설정`
- `feat: 어휘 CRUD API 추가`
- `feat: 퀴즈 생성 기능 추가`
- `fix: Docker 실행 환경 설정 수정`
- `refactor: 퀴즈 생성 로직 개선`
- `design: 퀴즈 화면 스타일 수정`
- `test: 어휘 서비스 테스트 추가`
- `docs: 로컬 실행 방법 문서화`
