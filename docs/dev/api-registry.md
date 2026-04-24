# API Cumulative Registry

> Initialized on Story 1.2 completion.
> Updated by Dev Agent after each story completion.

## Registry Metadata

**Last Updated**: 2026-04-24
**Total Stories Tracked**: 2 (1.1, 1.2)
**Total Endpoints**: 3 (1 health probe from 1.1 + 2 auth endpoints from 1.2)
**Repository**: edu-ai-community
**Mode**: monolith

## API Endpoints Registry

| Method | Path | Story | File | Auth | Request Schema | Response Schema |
|--------|------|-------|------|------|----------------|-----------------|
| GET | `/api/health` | 1.1 | edu-gateway actuator forward | none | — | actuator health json |
| POST | `/api/auth/send-sms` | 1.2 | `backend/edu-core/.../AuthController.java` | none | `SendSmsRequest` | `R<Void>` |
| POST | `/api/auth/register` | 1.2 | `backend/edu-core/.../AuthController.java` | none | `RegisterRequest` | `R<UserDto>` |

## API Design Patterns

- URL convention: RESTful, kebab-case, `/api/<domain>/<action-or-resource>`
- Response envelope: `R<T>` = `{ code: int, message: string, data: T | null }` (1.1)
- Success: `code=0, message="OK"`
- HTTP status contract: **always 200** for any business error (4000/400/409/429/503); only HTTP 500 for unhandled exceptions returning `R(5000)`
- Validation errors: `code=4000` (GlobalExceptionHandler.CODE_VALIDATION)

## Request/Response Schemas

| Schema | Type | Story | File |
|--------|------|-------|------|
| `SendSmsRequest` | Java DTO | 1.2 | `backend/edu-core/.../dto/SendSmsRequest.java` |
| `RegisterRequest` | Java DTO | 1.2 | `backend/edu-core/.../dto/RegisterRequest.java` |
| `UserDto` | Java DTO (response) | 1.2 | `backend/edu-core/.../dto/UserDto.java` |
| `R<T>` | Shared envelope | 1.1 | `backend/edu-common/.../response/R.java` |
| `BizException` | Shared exception | 1.1 | `backend/edu-common/.../exception/BizException.java` |

## Endpoints by Story

- **1.1**: `/api/health` (gateway actuator)
- **1.2**: `/api/auth/send-sms`, `/api/auth/register`
