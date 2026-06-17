# Data Model: Android Role-Based UI Enforcement

**Feature**: 003-android-role-based-ui
**Date**: 2026-06-12

## Conceptual Entities

This feature introduces no new database tables or persistent storage. It operates on existing data already loaded into `SessionManager`.

### RoleGate

A composable gate that wraps UI content and conditionally renders it based on the current user's role.

| Attribute | Type | Description |
|-----------|------|-------------|
| allowedRoles | Set\<String\> | Set of role strings ("admin", "dentist", "receptionist") permitted to see the content |
| content | @Composable () -> Unit | The UI to render if the current user's role is in `allowedRoles` |
| fallbackContent | @Composable () -> Unit | Optional UI to render if the role is not allowed (default: empty/blank) |

**Resolution**: On each composition, `RoleGate` reads `SessionManager.role` via `collectAsState()`. If the current role is in `allowedRoles`, `content` is rendered; otherwise `fallbackContent` is rendered.

### Role-Based Visibility Matrix

Mapping of role → feature visibility. This is derived from the Role × Feature Access Matrix in the product constitution and refined by the KIT-07 specification.

| Feature | Admin | Dentist | Receptionist |
|---------|-------|---------|-------------|
| Analytics tab | ✓ Visible | ✗ Hidden | ✗ Hidden |
| Schedule - book appointment FAB | ✓ Visible | ✗ Hidden | ✓ Visible |
| Schedule - edit/cancel appointment | ✓ Visible | ✗ Hidden | ✓ Visible |
| Patients - medical history edit | ✓ Editable | ✓ Editable | ✗ Read-only |
| Patients - medical files | ✓ Visible | ✓ Visible | ✗ Hidden |
| Patients - create procedure card | ✓ Visible | ✓ Visible | ✗ Hidden |
| Clinics - add/edit/delete | ✓ Visible | ✗ Hidden | ✗ Hidden |
| Procedures Library - create/edit/delete | ✓ Visible | ✗ Hidden | ✗ Hidden |
| Settings - Users panel | ✓ Visible | ✗ Hidden | ✗ Hidden |
| Payments - register payment | ✓ Visible | ✓ Visible | ✓ Visible |
| Payments - analytics summary | ✓ Visible | ✗ Hidden | ✗ Hidden |

### Role Display Modes

For any given UI element, the role check produces one of three outcomes:

| Mode | Description | When |
|------|-------------|------|
| **Visible & Active** | Element is shown and fully interactive | User's role is in the allowed set |
| **Visible & Read-Only** | Element is shown but cannot be edited | User's role is partially allowed (e.g., receptionist seeing medical history) |
| **Hidden** | Element is not rendered at all | User's role is not in the allowed set, and no read-only fallback is appropriate |

### Relationships

```
SessionManager.role (StateFlow<String?>)
    └── RoleGate (reads role, decides visibility)
            ├── content: rendered if role ∈ allowedRoles
            └── fallbackContent: rendered if role ∉ allowedRoles
```

No new entity relationships to the Supabase schema. The role value originates from the `profiles.role` column, loaded during authentication (implemented in KIT-02 through KIT-06).
