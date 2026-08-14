# Feature Specification: Login UI Package and Comment Cleanup

**Feature Branch**: `tech/002-login-ui-package-cleanup`

**Created**: 2026-08-14

**Status**: Draft

**Input**: User description: "сделай presentation.login.ui пакет и положи туда композ штуки, а также убери миллион комментариев в коде, можешь в целом смело все убирать"

## User Scenarios & Testing *(mandatory)*

The people served here are the contributors and agents working in this repository; the shipped
application behaves exactly as before.

### User Story 1 - Compose code lives in its own package (Priority: P1)

A contributor opening the login slice sees rendering code separated from state code: everything that
draws the screen sits in one place, and the state-producing code that a plain unit test can drive
sits beside it, not mixed into the same directory listing.

**Why this priority**: This is the structural change requested; the cleanup in Story 2 is easier to
review once files have already moved.

**Independent Test**: Move the rendering code into the new package, leave comments untouched, and
confirm the module still builds and every existing login test still passes unchanged in behavior.

**Acceptance Scenarios**:

1. **Given** the login slice, **When** a contributor lists the rendering package, **Then** it
   contains every declaration that draws or styles the screen and nothing that produces state.
2. **Given** the login slice, **When** a contributor lists the package one level above, **Then** it
   contains the state-producing declarations and no drawing code.
3. **Given** the moved code, **When** the full build and the existing login tests run, **Then** they
   pass and no rendered output, semantics, or test tag changes.

---

### User Story 2 - Source reads without commentary (Priority: P2)

A contributor reads a file and sees only code. Explanatory prose that restated the code, justified a
past decision, or duplicated a document does not appear between declarations.

**Why this priority**: Independent of Story 1 and valuable on its own, but it changes many more files
and is safest once the structure is settled.

**Independent Test**: Strip commentary from the Kotlin sources without moving anything, then confirm
the build, tests, and Detekt all pass and no non-comment line differs.

**Acceptance Scenarios**:

1. **Given** any Kotlin source in the repository, **When** it is opened, **Then** it contains no
   explanatory comment or documentation block.
2. **Given** a comment that the toolchain acts on rather than a reader, **When** cleanup runs,
   **Then** that comment is kept.
3. **Given** the cleanup, **When** the full build runs, **Then** compilation, tests, and static
   analysis all pass and no behavior changes.

---

### Edge Cases

- A comment carries a fact that exists nowhere else (a lifted design value, a workaround reason). It
  is dropped from code; if the fact must survive, it belongs in the owning document under `docs/` or
  in the feature's own artefacts, not in a comment.
- A comment is machine-read — a suppression directive, a tool marker, a generated-file header, or a
  license header. It stays.
- A comment is the only thing separating two declarations or holds a string that looks like a
  comment. Removal must not change the parsed program.
- A declaration name only made sense together with its comment. The name is improved rather than the
  comment restored — in its own commit, so the removal stays a pure deletion (FR-009).
- Moving a declaration would widen its visibility beyond the module. Visibility stays as narrow as
  before; if a move would force widening, the declaration does not move.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The login slice MUST expose a dedicated rendering package nested under its existing
  presentation package for the login screen.
- **FR-002**: Every declaration whose purpose is to draw or style the login screen MUST live in that
  rendering package, including the screen itself, its provider sheet, its legal line, its colour
  roles, and the tags that name its rendered elements.
- **FR-003**: Declarations that produce or describe state without drawing — the view model, the state
  mapper, and the provider declarations — MUST remain outside the rendering package.
- **FR-004**: Tests that exercise rendering MUST follow their subjects into the matching test
  package; tests that exercise state MUST stay where they are.
- **FR-005**: The move MUST NOT widen any declaration's visibility beyond the module.
- **FR-006**: The move MUST NOT change any rendered output, semantics, string resource, or test tag
  value.
- **FR-007**: All explanatory comments and documentation blocks MUST be removed from every Kotlin
  source in the repository — mobile, server, shared contracts, and the Gradle build-logic tree —
  including test sources.
- **FR-008**: Comments the toolchain acts on MUST be preserved: static-analysis suppression
  directives, generated-file markers, license headers, and any comment whose removal changes build or
  analysis behavior.
- **FR-009**: Comment removal MUST NOT change any non-comment line; reordering and logic edits are
  out of scope. The one rename allowed by Edge Cases — a name that only made sense together with its
  comment — MUST be a separate, separately reviewable commit, never part of the removal itself.
- **FR-010**: Comments in non-Kotlin files — documents under `docs/`, build scripts, configuration,
  resource files, and platform project files — MUST be left untouched.
- **FR-011**: Any statement that a project guide makes about where login presentation code lives MUST
  be reconciled with the new structure in the same change.
- **FR-012**: The repository MUST pass its full verification command after the change, plus the iOS
  target compilation, since the touched code is shared across platforms.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero explanatory comment lines remain in any Kotlin source; the 893 comment lines
  present across 116 of the repository's 170 Kotlin files before the change drop to only the
  machine-read ones allowed by FR-008.
- **SC-002**: 100% of login declarations that draw or style the screen sit in the rendering package,
  and 0% of state-producing declarations do.
- **SC-003**: Every test that passed before the change passes after it, with no test skipped, renamed
  in intent, or weakened.
- **SC-004**: The change introduces no difference in application behavior: no rendered element,
  accessibility label, or tag value differs from before.
- **SC-005**: A contributor can find the code that draws the login screen from the package name alone,
  without opening a file.

## Assumptions

- "Композ штуки" means every declaration that draws or styles the login screen, including the
  element tags naming those drawn elements. The view model, its state mapper, and the provider
  declarations are state, not drawing, and stay put.
- "Смело все убирать" is read at repository scope: all Kotlin sources, production and test, mobile
  and server and shared — not only the login slice.
- Documentation under `docs/` and the feature's spec artefacts remain the place where decisions are
  recorded, so nothing of value is lost by deleting code comments.
- This is a structural and hygiene change with no behavior change, so it belongs on a `tech/...`
  branch and requires no new tests; the existing suite is the safety net.
- No other feature slice exists with a comparable presentation package yet, so the new layout sets a
  precedent for future slices rather than requiring a parallel migration.
