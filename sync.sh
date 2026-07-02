#!/bin/bash
#
# sync.sh — safely bring local branches in line with their remotes.
#
# Read-only scan first, then act. It refuses to change anything if you have
# local changes (uncommitted work, or a branch with commits not on any remote).
# When clean it fetches, fast-forwards every tracking branch to its remote,
# deletes local branches whose remote is gone *but only if they're fully pushed*,
# and switches you to the default branch. See INSTRUCTIONS.GIT.md.

set -euo pipefail

clear

RED=$'\033[0;31m'
GREEN=$'\033[0;32m'
YELLOW=$'\033[0;33m'
BLUE=$'\033[0;34m'
GRAY=$'\033[0;90m'
BOLD=$'\033[1m'
RESET=$'\033[0m'

log()     { printf '%s\n' "${BLUE}> $* ${RESET}"; }
step()    { printf '\n%s\n' "${BOLD}${BLUE}==> ${BOLD}$*${RESET}"; }
success() { printf '%s\n' "${GREEN}$* ${RESET}"; }
warn()    { printf '%s\n' "${YELLOW}$* ${RESET}"; }
fail()    { printf '%s\n' "${RED}$* ${RESET}" >&2; }

trap 'fail "Failed at line $LINENO. Aborting — nothing further will run."' ERR

run() {
  log "${GRAY}\$ $*${RESET}"
  "$@"
}

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { fail "Not a git repository."; exit 1; }
cd "$(git rev-parse --show-toplevel)"

step "Step 1/4 — Check for local changes"
if [ -n "$(git status --porcelain)" ]; then
  warn "You have uncommitted changes / untracked files:"
  git status --short
  fail "Local changes detected. Commit or stash them first, then re-run. Nothing was changed."
  exit 1
fi
success "working tree clean"

step "Step 2/4 — Fetch from remote"
run git fetch --prune origin
git remote set-head origin --auto >/dev/null 2>&1 || true
default="$(git symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null | sed 's#^origin/##' || true)"
[ -n "$default" ] || default="$(git remote show origin | sed -n 's/.*HEAD branch: //p')"
[ -n "$default" ] || { fail "Could not determine the remote default branch."; exit 1; }
log "default branch is ${BOLD}${default}${RESET}"
success "fetched"

step "Step 3/4 — Scan local branches (read-only)"
diverged=""            # tracking branches with commits not on their remote -> abort
stranded=""            # orphaned branches with unpushed commits             -> abort
behind=()              # tracking branches purely behind their remote        -> fast-forward
delete=()              # orphaned but fully pushed                           -> delete

while IFS= read -r b; do
  up="$(git for-each-ref --format='%(upstream:short)' "refs/heads/${b}")"
  if [ -n "$up" ] && git show-ref --verify --quiet "refs/remotes/${up}"; then
    local_sha="$(git rev-parse "$b")"
    remote_sha="$(git rev-parse "$up")"
    base="$(git merge-base "$b" "$up")"
    if [ "$local_sha" = "$remote_sha" ]; then
      :                                                  # already up to date
    elif [ "$local_sha" = "$base" ]; then
      behind+=("$b")                                     # behind -> fast-forwardable
    else
      diverged+="  ${b}  (local commits not on ${up})\n"
    fi
  else
    # no upstream, or the remote branch was renamed/deleted (pruned)
    if [ -n "$(git branch -r --contains "$(git rev-parse "$b")" 2>/dev/null)" ]; then
      delete+=("$b")                                     # fully pushed somewhere -> safe to drop
    else
      stranded+="  ${b}  (no remote, and has commits not pushed anywhere)\n"
    fi
  fi
done < <(git for-each-ref --format='%(refname:short)' refs/heads/)

if [ -n "$diverged" ] || [ -n "$stranded" ]; then
  [ -n "$diverged" ] && { warn "Branches with local commits not on their remote:"; printf '%b' "$diverged"; }
  [ -n "$stranded" ] && { warn "Orphaned branches with unpushed commits:";        printf '%b' "$stranded"; }
  fail "Push, reset, or delete these first. Aborting; nothing was changed."
  exit 1
fi
success "safe to sync"

step "Step 4/4 — Sync and switch to default (${default})"
run git switch "$default"
run git merge --ff-only "origin/${default}"

if [ "${#behind[@]}" -gt 0 ]; then
  for b in "${behind[@]}"; do
    [ "$b" = "$default" ] && continue
    up="$(git for-each-ref --format='%(upstream:short)' "refs/heads/${b}")"
    run git branch -f "$b" "$up"
  done
fi

if [ "${#delete[@]}" -gt 0 ]; then
  for b in "${delete[@]}"; do
    [ "$b" = "$default" ] && continue
    warn "removing orphaned local branch '${b}' (fully pushed; its remote is gone)"
    run git branch -D "$b"
  done
fi
success "local branches match their remotes"

printf '\n%s\n' "${GREEN}${BOLD}Done. You are on '${default}'; everything is in sync.${RESET}"
printf '%s\n' "${GRAY}Tip: check out another branch anytime with 'git switch <name>' (auto-tracks the remote).${RESET}"
