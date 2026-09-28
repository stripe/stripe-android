#!/usr/bin/env ruby

abort <<~EOS
`scripts/deploy/deploy.rb` no longer runs the release end-to-end.

Use:
  `./scripts/deploy/propose_release.rb`
  `./scripts/deploy/deploy_release.rb`

`propose release` creates the version bump branch and PR handoff.
`deploy release` creates the signed tag from the merged version bump PR and publishes it.
EOS
