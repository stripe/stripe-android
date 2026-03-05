#!/usr/bin/env bash
set -o pipefail
set -e

REPO="stripe/stripe-android"
BRANCH_PREFIX="jaynewstrom/"

# Use a known-good pyenv Python version so the `llm` CLI works regardless of
# the repo's .python-version.
export PYENV_VERSION=3.11.10

LLM_SYSTEM_PROMPT="Output ONLY the requested text. No explanations, no markdown, no code blocks, no quotes, no surrounding formatting of any kind."

# ── Helpers ──────────────────────────────────────────────────────────────────

current_branch() {
  git rev-parse --abbrev-ref HEAD
}

maybe_update_master() {
  if [ "$(current_branch)" = "master" ]; then
    local stashed=false
    if ! git diff --quiet || ! git diff --cached --quiet || [ -n "$(git ls-files --others --exclude-standard)" ]; then
      echo "Stashing uncommitted changes..." >&2
      git stash push --include-untracked -m "create-pr: auto-stash before pull"
      stashed=true
    fi

    echo "Pulling latest master..." >&2
    git pull origin master

    if $stashed; then
      echo "Restoring stashed changes..." >&2
      git stash pop
    fi

    # Create a temporary branch; ensure_branch_name will rename it via LLM.
    git checkout -b temp-pr-branch
    return
  fi
}

# ── Rebase ───────────────────────────────────────────────────────────────────

maybe_rebase() {
  # Skip rebase prompt if we just branched off an up-to-date master.
  if [ "$(git rev-parse HEAD)" = "$(git rev-parse origin/master 2>/dev/null)" ]; then
    return
  fi

  read -rp "Rebase onto latest master? [y/n] " answer
  if [[ "$answer" != "y" ]]; then
    return
  fi

  local stashed=false
  if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Stashing uncommitted changes..." >&2
    git stash push -m "create-pr: auto-stash before rebase"
    stashed=true
  fi

  echo "Fetching latest master..." >&2
  git fetch origin master

  echo "Rebasing onto origin/master..." >&2
  if ! git rebase origin/master; then
    echo "Rebase failed. Aborting rebase..." >&2
    git rebase --abort
    if $stashed; then
      echo "Restoring stashed changes..." >&2
      git stash pop
    fi
    echo "Error: Rebase failed. Please resolve conflicts manually." >&2
    exit 1
  fi

  if $stashed; then
    echo "Restoring stashed changes..." >&2
    git stash pop
  fi

  echo "Rebase complete." >&2
}

# ── Branch naming ────────────────────────────────────────────────────────────

ensure_branch_name() {
  local branch
  branch=$(current_branch)

  if [[ "$branch" == "${BRANCH_PREFIX}"* ]]; then
    return
  fi

  # Generate a branch name from the current diff using LLM.
  echo "Generating branch name with LLM..." >&2
  local diff
  diff=$(git diff origin/master...HEAD 2>/dev/null || git diff HEAD)
  local suggested_suffix
  suggested_suffix=$(echo "$diff" | llm -s "$LLM_SYSTEM_PROMPT" "Suggest a short git branch name suffix for these changes. Rules: lowercase, hyphen-separated, 2-4 words, no prefix, no slashes. Example: allow-promotion-codes")

  local suggested_branch="${BRANCH_PREFIX}${suggested_suffix}"
  read -rp "Branch name [$suggested_branch]: " custom_branch
  local new_branch="${custom_branch:-$suggested_branch}"

  if [ -z "$new_branch" ]; then
    echo "Error: Branch name cannot be empty." >&2
    exit 1
  fi

  # Ensure the prefix is present even if the user typed a custom name.
  if [[ "$new_branch" != "${BRANCH_PREFIX}"* ]]; then
    new_branch="${BRANCH_PREFIX}${new_branch}"
  fi

  echo "Renaming branch '$branch' -> '$new_branch'..." >&2
  git branch -m "$new_branch"

  # Clean up old remote tracking branch if it exists.
  if git ls-remote --exit-code --heads origin "$branch" &>/dev/null; then
    echo "Deleting old remote branch 'origin/$branch'..." >&2
    git push origin --delete "$branch"
  fi
}

# ── Commit uncommitted changes ───────────────────────────────────────────────

maybe_commit() {
  if git diff --quiet && git diff --cached --quiet && [ -z "$(git ls-files --others --exclude-standard)" ]; then
    return
  fi

  echo "" >&2
  echo "You have uncommitted changes:" >&2
  git status --short >&2
  echo "" >&2

  read -rp "Stage all and commit before pushing? [y/n] " answer
  if [[ "$answer" != "y" ]]; then
    echo "Skipping commit. Uncommitted changes will not be included in the PR." >&2
    return
  fi

  git add -A

  echo "Generating commit message with LLM..." >&2
  local diff
  diff=$(git diff --cached)
  local suggested_msg
  suggested_msg=$(echo "$diff" | llm -s "$LLM_SYSTEM_PROMPT" "Suggest a concise commit message for this diff. One line, under 72 characters, no period at the end.")

  read -rp "Commit message [$suggested_msg]: " msg
  msg="${msg:-$suggested_msg}"
  if [ -z "$msg" ]; then
    echo "Error: Commit message cannot be empty." >&2
    exit 1
  fi
  git commit -m "$msg"
}

# ── Push ─────────────────────────────────────────────────────────────────────

push_branch() {
  local branch
  branch=$(current_branch)
  echo "Pushing '$branch' to origin..." >&2
  git push --force-with-lease --set-upstream origin "$branch"
}

# ── Label selection ──────────────────────────────────────────────────────────

SELECTED_LABELS=()

select_labels() {
  echo "" >&2
  echo "── Label selection (press Enter with no search term to finish) ──" >&2

  while true; do
    if [ ${#SELECTED_LABELS[@]} -gt 0 ]; then
      echo "Selected so far: ${SELECTED_LABELS[*]}" >&2
    fi

    read -rp "Search labels: " hint
    if [ -z "$hint" ]; then
      break
    fi

    local labels
    labels=$(export GH_HOST=github.com && gh label list --repo "$REPO" --limit 100 --json name --jq '.[].name' | grep -i "$hint" || true)

    if [ -z "$labels" ]; then
      echo "No labels matched '$hint'." >&2
      continue
    fi

    # Display numbered list.
    local i=1
    local label_array=()
    while IFS= read -r label; do
      label_array+=("$label")
      echo "  $i) $label" >&2
      ((i++))
    done <<< "$labels"

    read -rp "Pick a number (or press Enter to skip): " pick
    if [ -z "$pick" ]; then
      continue
    fi

    if [[ "$pick" =~ ^[0-9]+$ ]] && [ "$pick" -ge 1 ] && [ "$pick" -le "${#label_array[@]}" ]; then
      local chosen="${label_array[$((pick - 1))]}"
      SELECTED_LABELS+=("$chosen")
      echo "Added label: $chosen" >&2
    else
      echo "Invalid selection." >&2
    fi
  done
}

# ── PR creation ──────────────────────────────────────────────────────────────

create_pr() {
  echo "Generating PR title with LLM..." >&2
  local diff
  diff=$(git log --oneline origin/master..HEAD && echo "---" && git diff origin/master...HEAD)
  local suggested_title
  suggested_title=$(echo "$diff" | llm -s "$LLM_SYSTEM_PROMPT" "Suggest a concise PR title for these changes to the Stripe Android SDK. Under 70 characters, end with a period.")

  read -rp "PR title [$suggested_title]: " title
  title="${title:-$suggested_title}"
  if [ -z "$title" ]; then
    echo "Error: PR title cannot be empty." >&2
    exit 1
  fi

  local label_args=()
  for label in "${SELECTED_LABELS[@]}"; do
    label_args+=(--label "$label")
  done

  echo "Creating draft PR..." >&2
  local pr_url
  pr_url=$(GH_HOST=github.com gh pr create \
    --repo "$REPO" \
    --draft \
    --title "$title" \
    "${label_args[@]}")

  echo "" >&2
  echo "PR created: $pr_url" >&2
  open "$pr_url"
}

# ── Main ─────────────────────────────────────────────────────────────────────

maybe_update_master
maybe_rebase
maybe_commit
ensure_branch_name
push_branch
select_labels
create_pr
