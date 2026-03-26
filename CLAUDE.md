# CLAUDE.md

## Commands

- `./gradlew build` - Build all modules
- `./gradlew test` - Run all unit tests
- `./gradlew testDebugUnitTest` - Debug unit tests only
- `./gradlew :MODULE:testDebugUnitTest` - Single module (e.g. `:payments-core`, `:paymentsheet`)
- `./gradlew connectedAndroidTest` - Instrumentation tests (requires device)
- `./gradlew detekt` - Static analysis
- `./gradlew :dokkaGenerate` - API docs (outputs to docs/)

**GitHub Issues** — use `gh` CLI with `GH_HOST=github.com` prefix (`export` for reads, inline for writes). Always use `--state all` when searching. Check existing issues before investigating user reports.

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

**Testing** — MUST invoke the relevant skill before writing any test code:
- `write-tests` — unit test structure, runScenario pattern, Turbine Flow testing, Truth assertions
- `create-fake` — fake implementations with Turbine call tracking
- `compose-tests` — Compose UI tests with composeRule, Robolectric, node assertions
