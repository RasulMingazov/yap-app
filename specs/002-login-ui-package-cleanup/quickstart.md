# Quickstart: Verifying the Login UI Package and Comment Cleanup

Both halves of this change are behavior-neutral, so verification is about proving that *nothing*
changed except structure and comments. Run the checks in order; each is cheap and localises a failure
before the next one runs.

## Prerequisites

- JDK and Android SDK as configured for the repo; `./gradlew` from the repository root.
- `git config core.hooksPath .githooks` already run in this clone (pre-push Detekt gate).
- Work on a `tech/...` branch — this change alters no behavior.

## 1. The move compiles and the login suite still passes

```bash
./gradlew :apps:mobile:feature-auth:impl:detekt
./gradlew :apps:mobile:feature-auth:impl:build
```

**Expected**: green. Every login test that existed before runs and passes; none is skipped, renamed in
intent, or weakened (SC-003).

## 2. The move changed nothing but packages and imports

```bash
git diff -M --stat -- 'apps/mobile/feature-auth/impl/**'
git diff -M -- 'apps/mobile/feature-auth/impl/**' | grep -E '^[-+]' | grep -vE '^[-+]{3}' \
  | grep -vE '^[-+]\s*(package|import)\b'
```

**Expected**: the first command shows the five production files and six test files as renames; the
second prints only comment-line removals (or nothing, if the cleanup is committed separately). Any
other line is a regression against FR-006/FR-009.

Element vocabulary must be identical — the tags are string constants that tests and future screenshot
baselines match on:

```bash
git show HEAD~1:apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/LoginTestTags.kt \
  | grep -E 'const val' | sort > /tmp/tags-before.txt
grep -E 'const val' apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/ui/LoginTestTags.kt \
  | sort | diff /tmp/tags-before.txt -
```

**Expected**: no diff. (Adjust `HEAD~1` to the commit before the move.)

## 3. The package boundary holds

```bash
# Drawing code must not have been left behind:
grep -rl 'androidx.compose' \
  apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/*.kt

# State code must not have drifted into ui/:
grep -L 'androidx.compose\|LoginTestTags' \
  apps/mobile/feature-auth/impl/src/commonMain/kotlin/app/yap/feature/auth/presentation/login/ui/*.kt
```

**Expected**: both print nothing (SC-002).

## 4. The cleanup removed comments and only comments

```bash
# Remaining comment lines in Kotlin sources — baseline was 893 across 116 of 170 files:
find apps services shared convention-plugins -path '*/build/*' -prune -o -type f -name '*.kt' -print0 \
  | xargs -0 grep -cE '^\s*(//|/\*|\*)' | awk -F: '{s+=$NF} END {print "comment lines:", s}'

# Nothing machine-read was ever present, and nothing may be introduced:
find apps services shared convention-plugins -path '*/build/*' -prune -o -type f -name '*.kt' -print0 \
  | xargs -0 grep -nE '//\s*(noinspection|ktlint|detekt|TODO|FIXME|region|@formatter|SPDX)'
```

**Expected**: `comment lines: 0` and no matches (SC-001).

Then confirm no code line moved with the comments:

```bash
git diff -U0 -- '*.kt' | grep -E '^-' | grep -vE '^---' \
  | grep -vE '^-\s*(//|/\*|\*|\*/)' | grep -vE '^-\s*$'
```

**Expected**: empty. A non-empty result means a non-comment line was deleted — most likely a string
literal containing `//` (the `jdbc:postgresql://…` and `https://…` cases from research R6).

## 5. Full repository verification

```bash
./gradlew build
./gradlew :apps:mobile:shared-app:compileKotlinIosSimulatorArm64
```

**Expected**: both green. The first is required for any repository-wide change; the second because the
moved files are `commonMain` and the KMP boundary is touched.

## 6. Documentation is consistent

```bash
grep -rn 'presentation/login' docs/ CLAUDE.md README.md
```

**Expected**: `docs/mobile/001-feature-boundaries.md` now describes the nested `ui` package; no other
guide contradicts the new layout (FR-011). `specs/001-login-screen/*` is a historical record and is
intentionally left as written.

## Done when

Steps 1–6 pass and the change is reported with the exact commands run. If any step fails or is
skipped, say so plainly rather than omitting it.
