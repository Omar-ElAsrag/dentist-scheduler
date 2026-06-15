# Research: Android Role-Based UI Enforcement

**Feature**: 003-android-role-based-ui
**Date**: 2026-06-12

## R1: RoleGate Composable Pattern

**Decision**: Use a simple composable function `RoleGate` that accepts `allowedRoles: List<String>`, reads `SessionManager.role` via `collectAsState()`, and conditionally renders `content` if the current role is in the allowed set. An optional `fallbackContent` composable can be provided for permission-denied messaging.

```kotlin
@Composable
fun RoleGate(
    allowedRoles: Set<String>,
    fallbackContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit
)
```

**Rationale**: A single composable wrapper prevents role-checking logic from being scattered across 6+ screens. It reads from `SessionManager.role` (the single source of truth) and reacts to role changes automatically via StateFlow collection. The pattern is minimal (no DI, no new dependencies) and follows existing Compose idioms used elsewhere in the project (e.g., `LanguageSwitcher`, `AppCard`).

**Alternatives considered**:
- **Inline `if (role == "admin")` checks in each screen**: Rejected — violates FR-002 (no reusable mechanism) and SC-004 (no hardcoded role strings in composables).
- **Navigation-level gating via `NavHost` route definitions**: Rejected — doesn't cover within-screen gating (buttons, fields). Would need more complex route logic.
- **ViewModel-level role checking**: Rejected — role is UI-layer state, not business logic. ViewModel shouldn't dictate UI visibility.

## R2: Analytics Tab Visibility

**Decision**: Filter the `navItems` list in `MainAppScreen.kt` based on role BEFORE passing to the `NavigationBar`. For non-admin roles, exclude `NavigationItem.Analytics` from the list. Also add a `RoleGate` guard on the analytics composable route so direct navigation is caught.

**Rationale**: Removing the item from the list naturally hides the tab from the navigation bar without needing to modify the `NavigationBar` composable itself. The route-level guard prevents deep-link access.

**Alternatives considered**:
- **Grey out the tab instead of hiding**: Rejected — spec requires hiding, not disabling. A greyed-out tab would still tease the feature to non-admin users.
- **Keep tab visible but show permission message on tap**: Rejected — poor UX. Better to not show the tab at all.

## R3: Read-Only Field Enforcement

**Decision**: Use Compose's existing `enabled` parameter on `OutlinedTextField` and other input controls, setting `enabled = role != "receptionist"` for medical history fields. Alternatively, use `RoleGate` to swap editable fields for read-only `Text` displays.

**Rationale**: The `enabled = false` approach keeps the field visible but non-interactive, which is clearer UX than hiding the field entirely. It uses standard Compose APIs with no custom logic.

**Alternatives considered**:
- **RoleGate wrapping content blocks**: Rejected — showing a "permission denied" message instead of the fields would hide patient information, which is counterproductive. Receptionists need to see (but not edit) medical history.
- **Custom `MedicalField` composable**: Adds unnecessary abstraction for a simple `enabled` parameter.

## R4: Permission Denied Message

**Decision**: Use a simple `AlertDialog` for navigation-level denials (e.g., deep-link to Analytics) and inline `Text` or `OutlinedButton` replacement for within-screen denials (e.g., "Create Procedure Card" button replaced with "Requires dentist or admin access" text).

**Rationale**: `AlertDialog` is appropriate for full-screen denials (unexpected navigation), while inline text is less disruptive for within-screen restrictions where the user is already on the right page.

**Alternatives considered**:
- **Snackbar**: Rejected — snackbars auto-dismiss and the user might miss the message.
- **Toast**: Rejected — Android Toast is deprecated for foreground notifications.

## R5: RoleGate Placement Convention

**Decision**: All `RoleGate` usages SHALL reference `SessionManager.role` via the `RoleGate` composable. No screen shall directly import or reference the role value without going through `RoleGate`. The `RoleGate` composable lives in `ui/components/RoleGate.kt`.

**Rationale**: Centralizing the gating mechanism ensures SC-004 compliance (no hardcoded role strings). If the role model changes (e.g., new role added), only `RoleGate.kt` and its allowed-role sets need updating.

**Alternatives considered**:
- **RoleGate as a modifier**: Rejected — modifiers cannot conditionally render content; they can only modify it. Compose's `@Composable` function pattern is the correct approach for conditional content.
