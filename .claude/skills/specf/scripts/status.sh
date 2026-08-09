#!/usr/bin/env bash
#
# Emit a compact status summary for a spec-first feature as JSON, so commands
# (specf-batch, specf-verify) can reconcile task markers and repository state
# without a full read of tasks.md/spec.md/plan.md just for bookkeeping.
#
# Usage: status.sh <feature-slug-or-path>
#
# Output (JSON):
#   {
#     "feature": "001-authentication",
#     "feature_dir": "specs/001-authentication",
#     "docs": {"spec": true, "plan": true, "tasks": true},
#     "head": "9fb587a",
#     "branch": "feature/login",
#     "dirty": false,
#     "phases": [
#       {"phase": "Phase 4 — Server exposure and verification checkpoint",
#        "incomplete": ["T019", "T020"]},
#       ...
#     ]
#   }
#
# Only tasks.md is parsed for task/phase data. spec.md and plan.md are reported
# only as present/absent — reading their content is the caller's job, scoped to
# what it actually needs (see SKILL.md "Scoped reads").

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: status.sh <feature-slug-or-path>" >&2
    exit 1
fi

INPUT="$1"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || {
    echo "ERROR: not inside a git repository" >&2
    exit 1
}

if [ -d "$REPO_ROOT/specs/$INPUT" ]; then
    FEATURE_DIR="$REPO_ROOT/specs/$INPUT"
elif [ -d "$INPUT" ]; then
    FEATURE_DIR="$(cd "$INPUT" && pwd)"
else
    echo "ERROR: no feature directory for '$INPUT' (looked under specs/ and as a literal path)" >&2
    exit 1
fi

FEATURE_SLUG="$(basename "$FEATURE_DIR")"
SPEC="$FEATURE_DIR/spec.md"
PLAN="$FEATURE_DIR/plan.md"
TASKS="$FEATURE_DIR/tasks.md"

has_file() { [ -f "$1" ] && echo true || echo false; }

HEAD_SHA="$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo null)"
BRANCH="$(git -C "$REPO_ROOT" branch --show-current 2>/dev/null || echo null)"
if [ -n "$(git -C "$REPO_ROOT" status --porcelain 2>/dev/null)" ]; then DIRTY=true; else DIRTY=false; fi

# Group incomplete task IDs (- [ ] **T###**) under their nearest "## Phase" heading.
PHASES_TSV=""
if [ -f "$TASKS" ]; then
    PHASES_TSV="$(awk '
        /^## / {
            if (phase != "") print phase "\t" ids
            phase = $0
            sub(/^## /, "", phase)
            ids = ""
            next
        }
        /^- \[ \] \*\*T[0-9]+\*\*/ {
            match($0, /T[0-9]+/)
            id = substr($0, RSTART, RLENGTH)
            ids = ids (ids == "" ? "" : ",") id
        }
        END { if (phase != "") print phase "\t" ids }
    ' "$TASKS")"
fi

if command -v jq >/dev/null 2>&1; then
    PHASES_JSON="[]"
    if [ -n "$PHASES_TSV" ]; then
        PHASES_JSON="$(printf '%s\n' "$PHASES_TSV" | jq -Rn '
            [inputs
             | select(length > 0)
             | split("\t")
             | {phase: .[0], incomplete: ((.[1] // "") | split(",") | map(select(length > 0)))}
            ]')"
    fi
    jq -n \
        --arg feature "$FEATURE_SLUG" \
        --arg feature_dir "${FEATURE_DIR#"$REPO_ROOT"/}" \
        --argjson spec "$(has_file "$SPEC")" \
        --argjson plan "$(has_file "$PLAN")" \
        --argjson tasks "$(has_file "$TASKS")" \
        --arg head "$HEAD_SHA" \
        --arg branch "$BRANCH" \
        --argjson dirty "$DIRTY" \
        --argjson phases "$PHASES_JSON" \
        '{feature: $feature, feature_dir: $feature_dir,
          docs: {spec: $spec, plan: $plan, tasks: $tasks},
          head: $head, branch: $branch, dirty: $dirty, phases: $phases}'
else
    # jq missing: emit line-based fallback text instead of hand-rolled JSON.
    echo "FEATURE: $FEATURE_SLUG"
    echo "FEATURE_DIR: ${FEATURE_DIR#"$REPO_ROOT"/}"
    echo "DOCS: spec=$(has_file "$SPEC") plan=$(has_file "$PLAN") tasks=$(has_file "$TASKS")"
    echo "HEAD: $HEAD_SHA"
    echo "BRANCH: $BRANCH"
    echo "DIRTY: $DIRTY"
    echo "PHASES:"
    if [ -n "$PHASES_TSV" ]; then
        printf '%s\n' "$PHASES_TSV" | while IFS=$'\t' read -r phase ids; do
            echo "  - $phase | incomplete: ${ids:-none}"
        done
    fi
fi
