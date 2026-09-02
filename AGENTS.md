# AGENTS.md

## Commands

- `./gradlew build` - Build all modules
- `./gradlew test` - Run all unit tests
- `./gradlew testDebugUnitTest` - Debug unit tests only
- `./gradlew :MODULE:testDebugUnitTest` - Single module (e.g. `:payments-core`, `:paymentsheet`)
- `./gradlew connectedAndroidTest` - Instrumentation tests (requires device)
- `./gradlew detekt` - Static analysis
- `./gradlew :dokkaGenerate` - API docs (outputs to docs/)

**GitHub Issues** — use `gh` CLI with `GH_HOST=github.com` prefix (`export` for reads, inline for writes). Always use `--state all` when searching. Check existing issues before investigating user reports.

**Pull requests** — always open pull requests as drafts. Create the PR description from `.github/PULL_REQUEST_TEMPLATE.md`, preserving its headings and section order; fill in the existing sections instead of replacing them with custom headings. In `# Testing`, check `Added tests` when the change adds tests and `Modified tests` when it changes existing tests (check both when both apply). Never check `Manually verified`: commands run from the CLI, including tests also run by CI, are automated validation rather than manual verification. Leave all inapplicable testing checkboxes unchecked. Include `Committed and created by <agent>.` in the PR description, where agent is the type of agent (e.g., Claude or Codex).

**Internal Tools** — Jira: MOBILESDK, RUN_MOBILESDK | Trailhead space: mobile-sdk

## Architecture

Multi-module Android library for payment processing and financial services.

**Core**: `payments-core` (API models, Stripe client) → `payments` (high-level APIs) → `paymentsheet` (pre-built UI)
**Shared**: `stripe-core` (utilities), `payments-model` (data models), `payments-ui-core` (shared UI)
**Specialized**: `financial-connections`, `identity`, `connect`, `3ds2sdk`
**Infra**: `example`, `paymentsheet-example`, `payments-core-testing`, `lint`, `screenshot-testing`

**Key Patterns**
- Kotlin coroutines for async; Jetpack Compose + traditional Views
- Dagger/Hilt DI in some modules; binary-compatibility-validator for API compat
- Gradle with shared deps (dependencies.gradle), AGP 8.13.x, Kotlin 2.3.x
- Detekt for static analysis, Paparazzi for screenshot testing
- No defaults for internal code: public APIs give parameters defaults (`= null`, `= false`) for ergonomic construction; non-public code (`internal` or `@RestrictTo`) omits defaults on both model fields and function parameters to force explicit decisions at each call site

**Testing** — MUST invoke the relevant skill (in `.agents/skills/`) before writing any test code:
- `write-unit-tests` — unit test structure, fake implementations, runScenario pattern, Turbine testing, Truth assertions
- `compose-tests` — Compose UI tests with composeRule, Robolectric, node assertions
- `network-tests` — NetworkRule integration tests with testBodyFromFile and fixture patterns

**Build validation** — invoke `delegate-low-reasoning-work` before running routine Gradle checks, formatting, static analysis, or generated-output validation. Delegate when a low-cost subagent is available.
