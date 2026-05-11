#!/usr/bin/env ruby

abort <<~EOS
`scripts/deploy/deploy.rb` no longer runs the release end-to-end.

Use:
  `./scripts/deploy/propose_release.rb`
  `./scripts/deploy/deploy_release.rb`

`propose release` creates the version bump branch, signed tag, and PR handoff.
`deploy release` publishes from the existing signed tag after the version bump PR is merged.
EOS
