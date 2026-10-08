# Scheduling observation checklist

For each phase, record the PR number, head SHA, base branch, stack position,
Bitrise check or build URL, and observation time.

1. Before promotion, confirm layer 1 starts and layers 2 and 3 have no Bitrise build.
2. Detach layer 1 and rebase layer 2 onto master; preserve layer 3's parent relationship.
3. After promotion, confirm layer 2 starts and layer 3 has no Bitrise build.
4. Distinguish the base-change event from subsequent synchronize events.
5. Verify each child diff contains only its layer's change and all PRs stay draft/open.

Absence of a check immediately after an event is not sufficient proof of suppression.
Cross-check Bitrise build history and allow time for webhook processing.
