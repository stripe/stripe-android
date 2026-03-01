---
name: github-issues
description: Use when working with GitHub issues or pull requests for the stripe/stripe-android repository
---

# GitHub Issues

## Read-Only Operations

Use `export GH_HOST=github.com &&` prefix to avoid permission prompts:

- `export GH_HOST=github.com && gh issue list --repo stripe/stripe-android --limit 20` — List recent issues
- `export GH_HOST=github.com && gh issue view <issue_number> --repo stripe/stripe-android` — View specific issue
- `export GH_HOST=github.com && gh issue view <issue_number> --repo stripe/stripe-android --comments` — View issue with comments
- `export GH_HOST=github.com && gh issue list --repo stripe/stripe-android --state all --search "keyword" --limit 30` — Search ALL issues (open/closed) by keyword

## Write Operations

Use `GH_HOST=github.com` prefix (keep permission prompts for safety):

- `GH_HOST=github.com gh issue create --repo stripe/stripe-android` — Create new issue
- `GH_HOST=github.com gh issue edit <issue_number> --repo stripe/stripe-android` — Edit issue
- `GH_HOST=github.com gh pr create --repo stripe/stripe-android` — Create pull request

## Best Practices

- Always use `--state all` when searching to include closed/resolved issues
- Always check GitHub issues for similar problems before investigating user reports
- Use GitHub CLI to distinguish between SDK bugs vs integration issues
