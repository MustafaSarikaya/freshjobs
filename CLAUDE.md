# Fresh Jobs

Spring Boot backend (`backend/`) + React SPA (`frontend/`), with the API contract in `openapi/openapi.yaml`.

## TDD Workflow — Mandatory

This project follows strict Test-Driven Development. All contributors (human and AI) MUST follow these rules:

### Red-Green-Refactor
1. **Red**: Write a failing test first that describes the desired behavior
2. **Green**: Write the minimum code to make the test pass
3. **Refactor**: Clean up while keeping all tests green

### Rules
- Never write production code without a corresponding test
- Never break existing tests — if behavior changes intentionally, update the test in the same commit
- Run both test suites before every commit
- The frontend enforces 100% coverage — do not lower the Vitest thresholds without explicit justification
- Use `/* v8 ignore */` only with a comment explaining why

### Running Tests
```bash
# Backend (JDK 21 + Maven)
cd backend && mvn test

# Frontend
cd frontend && npm ci && npm run lint && npm run test
```

### API changes
- Edit `openapi/openapi.yaml` first; the backend generates its API interfaces from it at build time
- Regenerate frontend types with `cd frontend && npm run generate:types`
- Lint the spec with `npx -p @stoplight/spectral-cli spectral lint openapi/openapi.yaml`

### Test Layout
- Backend: JUnit 5 under `backend/src/test/java`, mirroring the main package structure
- Frontend: Vitest + Testing Library, `*.test.ts(x)` next to each source file
