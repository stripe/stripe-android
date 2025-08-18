# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

**Build and Test**
- `./gradlew build` - Build all modules
- `./gradlew test` - Run unit tests for all modules
- `./gradlew testDebugUnitTest` - Run debug unit tests only
- `./gradlew connectedAndroidTest` - Run instrumentation tests (requires device)
- `./gradlew :payments:test` - Run tests for a specific module
- `./gradlew :example:assembleDebug` - Build example app
- `./gradlew :paymentsheet-example:assembleDebug` - Build PaymentSheet example app

**Code Quality**
- `./gradlew detekt` - Run static analysis with Detekt
- `./gradlew ktlint` - Run Android lint

**Documentation**
- `./gradlew dokkaHtmlMultiModule` - Generate API documentation (outputs to docs/)

**Testing Individual Modules**
- Use module names from settings.gradle: `:payments-core`, `:paymentsheet`, `:financial-connections`, `:identity`, `:connect`, etc.
- Example: `./gradlew :payments-core:testDebugUnitTest`

**GitHub Issue Management**
- **Read-only operations**: Use `export GH_HOST=github.com &&` prefix to avoid permission prompts
  - `export GH_HOST=github.com && gh issue list --repo stripe/stripe-android --limit 20` - List recent issues
  - `export GH_HOST=github.com && gh issue view <issue_number> --repo stripe/stripe-android` - View specific issue
  - `export GH_HOST=github.com && gh issue view <issue_number> --repo stripe/stripe-android --comments` - View issue with comments
  - `export GH_HOST=github.com && gh issue list --repo stripe/stripe-android --state all --search "keyword" --limit 30` - Search ALL issues (open/closed) by keyword
- **Write operations**: Use `GH_HOST=github.com` prefix (keep permission prompts for safety)
  - `GH_HOST=github.com gh issue create --repo stripe/stripe-android` - Create new issue
  - `GH_HOST=github.com gh issue edit <issue_number> --repo stripe/stripe-android` - Edit issue
  - `GH_HOST=github.com gh pr create --repo stripe/stripe-android` - Create pull request
- Always use `--state all` when searching to include closed/resolved issues
- Always check GitHub issues for similar problems before investigating user reports
- Use GitHub CLI to distinguish between SDK bugs vs integration issues

## Architecture

This is the **Stripe Android SDK**, a multi-module Android library for payment processing and financial services.

**Core Architecture**
- **payments-core**: Core payment functionality, API models, and Stripe API client
- **payments**: High-level payment APIs and utilities (depends on payments-core)
- **paymentsheet**: Pre-built payment UI components (PaymentSheet, FlowController)
- **payments-ui-core**: Shared UI components and styling
- **stripe-core**: Fundamental utilities shared across all modules
- **payments-model**: Data models for payment objects

**Specialized Modules**
- **financial-connections**: Bank account linking and financial data
- **identity**: Identity verification features
- **connect**: Stripe Connect marketplace functionality
- **3ds2sdk**: 3D Secure 2.0 authentication
- **stripecardscan**: Credit card OCR scanning

**Development Infrastructure**
- **example**: Main example/demo application
- **paymentsheet-example**: PaymentSheet-specific examples
- **payments-core-testing**: Testing utilities for payments-core
- **lint**: Custom Android lint rules
- **screenshot-testing**: UI screenshot test utilities

**Key Patterns**
- Modules follow Android library conventions with consumer ProGuard rules
- Heavy use of Kotlin coroutines for async operations
- Jetpack Compose UI components alongside traditional Android Views
- Dependency injection with Dagger/Hilt in some modules
- API compatibility validation with binary-compatibility-validator

**Build System**
- Multi-module Gradle project with shared dependency management (dependencies.gradle)
- Android Gradle Plugin 8.8.x with Kotlin 2.1.x
- Build configurations in build-configuration/ directory
- Detekt for static analysis, ktlint for formatting
- Paparazzi for screenshot testing
- Custom deployment and versioning scripts in scripts/

**Testing Strategy**
- Unit tests with JUnit, Mockito, and Truth assertions
- Instrumentation tests using Espresso and AndroidX Test
- Robolectric for Android unit tests
- Screenshot testing with Paparazzi and custom screenshot-testing module