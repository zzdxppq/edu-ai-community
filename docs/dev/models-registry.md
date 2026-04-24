# Models & Types Cumulative Registry

> Initialized on Story 1.2 completion.
> Updated by Dev Agent after each story completion.

## Registry Metadata

**Last Updated**: 2026-04-24
**Total Stories Tracked**: 2 (1.1, 1.2)
**Repository**: edu-ai-community
**Mode**: monolith

## Enums & Constants (Backend Java)

| Name | Values | Story | File |
|------|--------|-------|------|
| `UserRole` | REGION_ADMIN, CONSORTIUM_LEAD, MEMBER_URBAN, MEMBER_RURAL, RESEARCHER, OTHER (each with Chinese displayName) | 1.2 | `backend/edu-common/.../constant/UserRole.java` |
| `UserType` | USER, ADMIN | 1.2 | `backend/edu-common/.../constant/UserType.java` |

## Classes (Backend Java)

| Name | Category | Story | File |
|------|----------|-------|------|
| `R<T>` | response envelope | 1.1 | `backend/edu-common/.../response/R.java` |
| `BizException` | exception | 1.1 | `backend/edu-common/.../exception/BizException.java` |
| `GlobalExceptionHandler` | @RestControllerAdvice | 1.1 | `backend/edu-common/.../exception/GlobalExceptionHandler.java` |
| `User` | JPA entity | 1.2 | `backend/edu-core/.../auth/entity/User.java` |
| `UserRepository` | Spring Data JPA | 1.2 | `backend/edu-core/.../auth/repository/UserRepository.java` |
| `SmsService` | service interface | 1.2 | `backend/edu-core/.../auth/service/SmsService.java` |
| `LogSmsService` | service impl (@Profile dev/test) | 1.2 | `backend/edu-core/.../auth/service/impl/LogSmsService.java` |
| `AuthService` | service | 1.2 | `backend/edu-core/.../auth/service/AuthService.java` |
| `AuthController` | @RestController | 1.2 | `backend/edu-core/.../auth/controller/AuthController.java` |

## DTOs (Backend Java)

| Name | Purpose | Story | File |
|------|---------|-------|------|
| `SendSmsRequest` | request | 1.2 | `backend/edu-core/.../auth/dto/SendSmsRequest.java` |
| `RegisterRequest` | request | 1.2 | `backend/edu-core/.../auth/dto/RegisterRequest.java` |
| `UserDto` | response (whitelist 6 fields) | 1.2 | `backend/edu-core/.../auth/dto/UserDto.java` |

## TypeScript Types (Frontend)

| Name | Kind | Story | File |
|------|------|-------|------|
| `AuthUser` / `AuthState` | interface | 1.1 | `frontend/src/stores/auth.ts` |
| `UserRole` | literal union | 1.2 | `frontend/src/types/auth.ts` |
| `RoleOption` | interface | 1.2 | `frontend/src/types/auth.ts` |
| `ROLE_OPTIONS` | const array | 1.2 | `frontend/src/types/auth.ts` |
| `SendSmsRequest` / `RegisterRequest` / `UserDto` / `ApiEnvelope<T>` | interface | 1.2 | `frontend/src/types/auth.ts` |

## Composables (Frontend)

| Name | Story | File |
|------|-------|------|
| `useCountdown(initialSeconds)` | 1.2 | `frontend/src/composables/useCountdown.ts` |

## Naming Patterns

- Java class: PascalCase
- Java package: lowercase, dot-separated by layer (`com.edu.ai.core.auth.{controller,service,repository,entity,dto}`)
- Java enum: PascalCase (SCREAMING_SNAKE_CASE values)
- Java DTO: PascalCase with `Request` / `Dto` suffix by purpose
- TypeScript interface: PascalCase
- TypeScript const: SCREAMING_SNAKE_CASE
- Vue component: PascalCase.vue
- Vue composable: `use{Noun}.ts`

## Models by Story

- **1.1**: `R<T>`, `BizException`, `GlobalExceptionHandler`, `AuthUser`, `AuthState`
- **1.2**: `UserRole`, `UserType`, `User`, `UserRepository`, `SmsService`, `LogSmsService`, `AuthService`, `AuthController`, `SendSmsRequest`, `RegisterRequest`, `UserDto`, frontend types `UserRole`/`RoleOption`/`ROLE_OPTIONS`/`SendSmsRequest`/`RegisterRequest`/`UserDto`/`ApiEnvelope`, `useCountdown` composable.
