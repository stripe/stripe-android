---
name: delegate-low-reasoning-work
description: Use when running routine Gradle tests, builds, lint, formatting, generated-output checks, or verbose output collection in stripe-android. Delegate independent low-judgment work to a low-cost subagent to preserve the main context.
---

# Delegate Routine Work

Delegate routine, verbose work when the client has a lower-cost subagent available. Keep responsibility for intent, implementation choices, diagnosis, and the final conclusion in the main conversation.

## What to delegate

- Every exact local `./gradlew` test, build, lint, formatting, or generated-output command.
- Mechanical log, test-artifact, and file-inventory collection.
- Repeating an explicit command across independently scoped modules.

## Select the executor

Use the least capable available agent that can execute the task reliably. Prefer the client's low-cost profile, such as Haiku or Luna when available. If no appropriate subagent or delegation slot is available, run the command in the main session and say why.

Do not delegate code changes, architectural decisions, failure diagnosis, security-sensitive review, or user-facing conclusions.

## Give a narrow contract

State the exact command, working directory, relevant timeout or device constraint, and that the subagent must not edit files. Ask for a compact result containing the exit status, elapsed time, failing test names if any, and no more than 80 relevant log lines.

```text
Run ./gradlew :paymentsheet:testDebugUnitTest from the repository root. Do not edit files.
Return the exit status, elapsed time, failing test names if any, and at most 80 relevant log lines.
```

## Verify and continue

Treat the returned output as evidence, not a conclusion. Triage failures and decide the next action in the main session. If the task grows beyond the original contract, delegate a newly bounded command or handle the reasoning directly.
