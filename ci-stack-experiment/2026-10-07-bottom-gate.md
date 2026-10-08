# Bottom-only Bitrise trigger experiment

Initial topology: master -> layer 1 -> layer 2 -> layer 3.
Layer 1 supplies this note, layer 2 restricts the Bitrise PR trigger to master,
and layer 3 supplies an observation checklist.

Expected initial scheduling: layer 1 starts Bitrise; layers 2 and 3 do not.
Then detach layer 1 and rebase layer 2 onto master, leaving layer 3 above it.
Expected scheduling after promotion: layer 2 starts Bitrise; layer 3 does not.

No PR or commit skip markers are used. GitHub Actions retains its existing triggers.
Observe scheduling for exact PR heads; successful build completion is a separate result.
