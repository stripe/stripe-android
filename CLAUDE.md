# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
- Android Gradle Plugin 8.13.x with Kotlin 2.3.x
- Build configurations in build-configuration/ directory
- Detekt for static analysis
- Paparazzi for screenshot testing
- Custom deployment and versioning scripts in scripts/

**Testing**
- Fakes over mocks, Turbine for Flow testing, `runScenario` pattern for test setup

**Internal Tools**
- Jira boards: MOBILESDK and RUN_MOBILESDK
- Trailhead space: mobile-sdk
