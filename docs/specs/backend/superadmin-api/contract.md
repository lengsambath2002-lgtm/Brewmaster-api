# SuperAdmin Platform API

Cross-tenant management surface for platform operators. All endpoints require
a Bearer token whose user has `role="SuperAdmin"` (returns 403 otherwise).
Mounted under `/api/superadmin`.

## Identity model

- A reserved tenant `slug="__platform__"` (`name="Platform"`, `active=true`)
  holds the SuperAdmin user. The seeded SuperAdmin is
  `super@brewmaster.com` / `super1234` (rotate after first boot).
- `User.tenantId` is non-null. The SuperAdmin is bound to the platform tenant
  but is not subject to per-tenant suspension.
- Login is rejected when the user's tenant has `active=false`, except for
  the SuperAdmin role.

## Slug rules

- Lowercased, trimmed, `[a-z0-9-]+`.
- Reserved: `default`, `__platform__`.
- Globally unique (enforced by DB constraint).

## Endpoints

| Method | Path                                        | Notes                                            |
| ------ | ------------------------------------------- | ------------------------------------------------ |
| GET    | `/tenants`                                  | All tenants except `__platform__`, with counts.  |
| POST   | `/tenants`                                  | Create tenant + first owner in one transaction.  |
| GET    | `/tenants/{id}`                             | KHQR settings + owner list.                      |
| PATCH  | `/tenants/{id}`                             | Update `name` and KHQR settings.                 |
| POST   | `/tenants/{id}/suspend`                     | Sets `active=false`.                             |
| POST   | `/tenants/{id}/activate`                    | Sets `active=true`.                              |
| GET    | `/tenants/{id}/users`                       | Owners of a tenant.                              |
| POST   | `/tenants/{id}/users`                       | Add an owner; default role `Owner`.              |
| POST   | `/tenants/{id}/users/{userId}/reset-password` | Set a new hashed password. 204.                |
| GET    | `/stats`                                    | Platform totals + per-tenant breakdown.          |

## Validation & errors

- Bean Validation on request bodies (`@NotBlank`, `@Email`, `@Size`).
  Invalid payloads return `400 VALIDATION_ERROR`.
- Slug or email conflicts return `409 CONFLICT`.
- Unknown tenant or user IDs return `404 NOT_FOUND`. Requests targeting the
  platform tenant by ID also return `404` (the platform tenant is invisible
  to this API).
- Caller without SuperAdmin role returns `403 FORBIDDEN`.
- `passwordHash` is never serialized in any response.

## Cross-tenant data access

The `Tenant` and `User` entities are NOT `@TenantId`-filtered, so JPA reads
work cross-tenant directly. Tenant-scoped entities (`Order`, `Product`,
`Category`, `Transaction`) are filtered by Hibernate's discriminator. All
aggregates here use native SQL (in `SuperAdminRepository`) to bypass the
filter — the SuperAdmin's own `TenantContext` is irrelevant for these reads.

## Suspension enforcement

- `AuthService.login` rejects users whose tenant is suspended (`401`).
- `GuestTenantInterceptor` returns `404` for guest traffic against a
  suspended tenant.
- SuperAdmin and the platform tenant are exempt.
