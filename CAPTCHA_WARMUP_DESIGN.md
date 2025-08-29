# HCaptcha Local Timeout Cache Design

## Overview

This branch implements a local timeout cache optimization for HCaptcha passive challenges in the Stripe Android SDK. The system reduces latency and improves user experience by pre-warming captcha tokens through a dedicated activity and caching successful results.

## Problem Statement

Previously, HCaptcha passive challenges were performed synchronously during payment confirmation flows, leading to:
- Increased payment confirmation latency
- Poor user experience due to loading delays
- Repeated captcha challenges for similar operations

## Solution Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────────┐
│                        PaymentSheet                             │
├─────────────────────────────────────────────────────────────────┤
│ PaymentSheetActivity                                            │
│ └─ onCreate() → registerFromActivity()                         │
│                                                                 │
│ PaymentSheetViewModel                                           │
│ ├─ passiveChallengeWarmer: PassiveChallengeWarmer              │
│ ├─ registerFromActivity(activityResultCaller, lifecycleOwner)  │
│ └─ warmUpCaptcha() with hardcoded params (TODO: use metadata)  │
└─────────────────────────────────────────────┼─────────────────┘
                                               │
                                               v
┌─────────────────────────────────────────────────────────────────┐
│              PassiveChallenge Warmer System                     │
├─────────────────────────────────────────────────────────────────┤
│ PassiveChallengeWarmer (Interface)                              │
│ ├─ register(activityResultCaller, lifecycleOwner)              │
│ └─ start(passiveCaptchaParams)                                 │
│                                                                 │
│ DefaultPassiveChallengeWarmer                                   │
│ ├─ launcher: ActivityResultLauncher                            │
│ ├─ register() - sets up launcher & lifecycle observer         │
│ └─ start() - launches PassiveChallengeWarmerActivity          │
│                                                                 │
│ PassiveChallengeWarmerActivityContract                          │
│ ├─ createIntent() - creates warmup activity intent             │
│ └─ parseResult() - handles activity completion                 │
└─────────────────────────────────────────────┼─────────────────┘
                                               │
                                               v
┌─────────────────────────────────────────────────────────────────┐
│            Dedicated Warmup Activity System                     │
├─────────────────────────────────────────────────────────────────┤
│ PassiveChallengeWarmerActivity                                  │
│ ├─ onStart() - triggers warmup in lifecycle scope             │
│ ├─ viewModel: PassiveChallengeWarmerViewModel                  │
│ └─ finish() after warmup completion                           │
│                                                                 │
│ PassiveChallengeWarmerViewModel                                 │
│ ├─ passiveCaptchaParams from Dagger component                 │
│ ├─ hCaptchaService injection                                   │
│ └─ warmUp(activity) delegates to service                      │
│                                                                 │
│ PassiveChallengeWarmerComponent (Dagger)                       │
│ ├─ HCaptchaModule integration                                  │
│ └─ PassiveCaptchaParams binding                               │
└─────────────────────────────────────────────┼─────────────────┘
                                               │
                                               v
┌─────────────────────────────────────────────────────────────────┐
│                    HCaptcha Service Layer                       │
├─────────────────────────────────────────────────────────────────┤
│ HCaptchaService (Interface)                                     │
│ ├─ warmUp(activity, siteKey, rqData)                           │
│ └─ performPassiveHCaptcha(activity, siteKey, rqData)           │
│                                                                 │
│ DefaultHCaptchaService (Singleton)                              │
│ ├─ cachedResult: MutableStateFlow<CachedResult>                │
│ ├─ warmUp() - background token generation & caching           │
│ ├─ performPassiveHCaptcha() - cache lookup with timeout       │
│ └─ transformCachedResult() - reactive cache state management   │
└─────────────────────────────────────────────────────────────────┘
```

### Cache State Machine

```
┌─────────────────────────────────────────────────────────────────┐
│                    CachedResult State Flow                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│    ┌─────────┐    warmUp()     ┌──────────┐                    │
│    │  Idle   │ ──────────────▶ │ Loading  │                    │
│    └─────────┘                 └──────────┘                    │
│         ▲                           │                           │
│         │                           │                           │
│         │ new session               ▼                           │
│    ┌─────────┐   ◄───────────  ┌──────────┐                    │
│    │ Failure │                 │ Success  │                    │
│    └─────────┘   ──────────▶   └──────────┘                    │
│         ▲           retry            │                          │
│         │                           │                          │
│         └───────────────────────────┘                          │
│              token expires                                      │
│                                                                 │
│  canWarmUp Logic:                                               │
│  • Idle, Failure: true (can start new warmup)                 │
│  • Loading, Success: false (operation in progress/cached)      │
└─────────────────────────────────────────────────────────────────┘
```

### Sequence Diagram: Warmup Flow

```
PaymentSheet      WarmerSystem      WarmerActivity    HCaptchaService    HCaptcha SDK
     │                  │                 │                 │                │
     │ onCreate()       │                 │                 │                │
     │ registerFromActivity()            │                 │                │
     │─────────────────▶│                 │                 │                │
     │                  │ register()      │                 │                │
     │                  │ (setup launcher)│                 │                │
     │                  │                 │                 │                │
     │ warmUpCaptcha()  │                 │                 │                │
     │─────────────────▶│                 │                 │                │
     │                  │ start(params)   │                 │                │
     │                  │─────────────────│                 │                │
     │                  │ launcher.launch()               │                │
     │                  │─────────────────▶│                 │                │
     │                  │                 │ onStart()       │                │
     │                  │                 │ viewModel.warmUp()              │
     │                  │                 │─────────────────▶│                │
     │                  │                 │                 │ warmUp()       │
     │                  │                 │                 │ cachedResult   │
     │                  │                 │                 │ = Loading      │
     │                  │                 │                 │                │
     │                  │                 │                 │ performPassive()│
     │                  │                 │                 │ Helper()       │
     │                  │                 │                 │──────────────── ▶│
     │                  │                 │                 │                │ setup()
     │                  │                 │                 │                │ verify()
     │                  │                 │                 │                │
     │                  │                 │                 │ onSuccess()    │
     │                  │                 │                 │◄────────────────│
     │                  │                 │                 │ cachedResult   │
     │                  │                 │                 │ = Success(token)│
     │                  │                 │ finish()        │                │
     │                  │                 │◄────────────────│                │
     │                  │ onResult        │                 │                │
     │                  │◄────────────────│                 │                │
     │                  │                 │                 │                │
     │  [Later: Payment Confirmation]     │                 │                │
     │                  │                 │                 │                │
     │ confirmPayment   │                 │                 │                │
     │─────────────────────────────────────────────────────▶│                │
     │                  │                 │                 │ performPassive()│
     │                  │                 │                 │ -> returns     │
     │                  │                 │                 │    cached token│
     │                  │                 │                 │ (no SDK call)  │
```

## Key Features

### 1. Local Timeout Cache
- **Cache Duration**: 6-second timeout for HCaptcha operations
- **State Management**: Uses `MutableStateFlow<CachedResult>` for reactive state handling
- **Cache States**: Idle, Loading, Success(token), Failure(error)

### 2. Pre-warming System
- **Trigger**: Initiated during PaymentSheet lifecycle (`onCreate`)
- **Background Processing**: Captcha tokens generated proactively
- **Lifecycle Integration**: Tied to Android activity lifecycle for proper resource management

### 3. Smart Cache Logic
- **canWarmUp**: Prevents redundant warmup operations
  - `true`: Idle or Failure states (can start new warmup)
  - `false`: Loading or Success states (operation in progress or cached)
- **Automatic Invalidation**: Cache cleared on failures or timeouts

### 4. Integration Points

#### PaymentSheet Integration
```kotlin
// PaymentSheetActivity.kt - onCreate()
override fun registerFromActivity(
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
) {
    confirmationHandler.register(activityResultCaller, lifecycleOwner)
    warmUpCaptcha(activityResultCaller, lifecycleOwner)
}

// PaymentSheetViewModel.kt:723
private fun warmUpCaptcha(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
    passiveChallengeWarmer.register(activityResultCaller, lifecycleOwner)
    // TODO: Wait for passive captcha params to be available instead of hardcoded params
    val params = PassiveCaptchaParams("143aadb6-fb60-4ab6-b128-f7fe53426d4a", null)
    passiveChallengeWarmer.start(params)
}
```

#### Warmer System Implementation
```kotlin
// DefaultPassiveChallengeWarmer.kt
override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
    val contract = PassiveChallengeWarmerActivityContract()
    launcher = activityResultCaller.registerForActivityResult(contract) {}
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            launcher?.unregister()
            launcher = null
        }
    })
}

override fun start(passiveCaptchaParams: PassiveCaptchaParams) {
    launcher?.launch(PassiveChallengeWarmerActivityContract.Args(passiveCaptchaParams))
}

// PassiveChallengeWarmerActivity.kt:22
override fun onStart() {
    super.onStart()
    lifecycleScope.launch {
        runCatching { viewModel.warmUp(this@PassiveChallengeWarmerActivity) }
        setResult(Activity.RESULT_OK, Intent())
        finish()
    }
}
```

#### Service Layer Enhancement  
```kotlin
// DefaultHCaptchaService.kt - Singleton with cache
override suspend fun warmUp(activity: FragmentActivity, siteKey: String, rqData: String?) {
    if (cachedResult.value.canWarmUp.not()) return
    cachedResult.emit(CachedResult.Loading)
    val update = when (val result = performPassiveHCaptchaHelper(activity, siteKey, rqData)) {
        is HCaptchaService.Result.Failure -> CachedResult.Failure(result.error)
        is HCaptchaService.Result.Success -> CachedResult.Success(result.token)
    }
    cachedResult.emit(update)
}

// HCaptchaModule.kt - Singleton provision
@Provides
fun provideHCaptchaService(hCaptchaProvider: HCaptchaProvider): HCaptchaService {
    return hCaptchaService ?: synchronized(this) {
        hCaptchaService ?: DefaultHCaptchaService(hCaptchaProvider).also {
            this.hCaptchaService = it
        }
    }
}
```

## Benefits

### Performance Improvements
- **Reduced Latency**: Pre-warmed tokens eliminate captcha delay during payment confirmation
- **Better UX**: Smoother payment flows without blocking captcha challenges
- **Resource Optimization**: Prevents duplicate captcha calls for same session

### Reliability Enhancements
- **Timeout Handling**: 6-second timeout prevents indefinite hangs
- **Failure Recovery**: Automatic retry mechanism on failed warmups
- **State Consistency**: Reactive state management ensures UI consistency

### Maintainability
- **Clean Separation**: Warmer system isolated from core payment logic  
- **Testable Design**: Injectable dependencies and clear interfaces
- **Lifecycle Awareness**: Proper Android lifecycle integration

## Testing Strategy

The implementation includes comprehensive test coverage:

### Unit Tests
- **DefaultHCaptchaServiceTest**: Cache state transitions, timeout handling, error scenarios
- **PassiveChallengeWarmerViewModelTest**: Warmup coordination logic
- **PassiveChallengeWarmerActivityTest**: Activity lifecycle integration

### Integration Tests  
- **PaymentSheetViewModelTest**: End-to-end warmup flow validation
- **HCaptcha Service Integration**: Real SDK interaction testing

### Test Patterns Used
- **Fakes over Mocks**: `FakeHCaptchaService` for predictable test behavior
- **Flow Testing**: Turbine library for reactive stream validation
- **Truth Assertions**: Clear, readable test assertions

## Configuration

### Dependency Injection
```kotlin
// PassiveChallengeWarmerComponent.kt - Dedicated Dagger component
@Singleton
@Component(modules = [HCaptchaModule::class])
internal interface PassiveChallengeWarmerComponent {
    val passiveChallengeWarmerViewModel: PassiveChallengeWarmerViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun passiveCaptchaParams(passiveCaptchaParams: PassiveCaptchaParams): Builder
        fun build(): PassiveChallengeWarmerComponent
    }
}

// PassiveChallengeWarmerModule.kt - Warmer provider
@Module
interface PassiveChallengeWarmerModule {
    @Provides
    fun providePassiveChallengeWarmer(): PassiveChallengeWarmer {
        return DefaultPassiveChallengeWarmer()
    }
}

// PaymentSheetViewModel injection - Constructor parameter
internal class PaymentSheetViewModel @Inject internal constructor(
    // ... other parameters
    private val passiveChallengeWarmer: PassiveChallengeWarmer,
    // ...
)
```

### Android Manifest
```xml
<!-- payments-core/AndroidManifest.xml -->
<activity
    android:name="com.stripe.android.challenge.warmer.PassiveChallengeWarmerActivity"
    android:exported="false"
    android:theme="@style/StripeTransparentTheme"
    android:launchMode="singleInstance"/>
```

## Backward Compatibility

- **Interface Preservation**: Existing `HCaptchaService.performPassiveHCaptcha()` unchanged
- **Additive Changes**: New `warmUp()` method added without breaking existing clients
- **Graceful Degradation**: System works without warmup if integration is incomplete

## Current Implementation Status

### Completed Features
- ✅ **Cache State Management**: Reactive cache with timeout handling
- ✅ **Dedicated Warmup Activity**: Isolated activity for background warmup
- ✅ **Activity Result Integration**: Proper launcher lifecycle management
- ✅ **Singleton Service**: Shared cache across warmup and actual usage
- ✅ **Comprehensive Testing**: Unit and integration tests for all components

### Known Limitations & TODOs
- 🔄 **Hardcoded Parameters**: Currently using test site key instead of dynamic paymentMethodMetadata
  ```kotlin
  // TODO: Wait for passive captcha params to be available instead of hardcoded params
  val params = PassiveCaptchaParams("143aadb6-fb60-4ab6-b128-f7fe53426d4a", null)
  ```
- 🔄 **Metadata Integration**: Need to wire up actual PassiveCaptchaParams from payment metadata
- 🔄 **Launch Mode Validation**: singleInstance launch mode needs production testing

## Future Enhancements

### Immediate Next Steps
- **Dynamic Parameters**: Replace hardcoded site key with paymentMethodMetadata flow
- **Conditional Warmup**: Only warm up when PassiveCaptchaParams are available
- **Performance Validation**: Measure actual latency improvements in production

### Potential Optimizations
- **Cross-Session Caching**: Persist tokens across app sessions
- **Adaptive Timeout**: Dynamic timeout based on network conditions
- **Batch Warmup**: Pre-warm multiple captcha variations simultaneously

### Monitoring & Analytics  
- **Cache Hit Rate**: Track warmup effectiveness
- **Performance Metrics**: Measure latency improvements
- **Error Tracking**: Monitor warmup failure patterns

---

## Files Modified

**Core Implementation:**
- `payments-core/src/main/java/com/stripe/android/hcaptcha/DefaultHCaptchaService.kt` - Singleton cache logic with timeout
- `payments-core/src/main/java/com/stripe/android/hcaptcha/HCaptchaService.kt` - Extended interface with warmUp() method
- `payments-core/src/main/java/com/stripe/android/hcaptcha/HCaptchaModule.kt` - Singleton service provision

**Warmer System (7 new files):**
- `DefaultPassiveChallengeWarmer.kt` - Activity launcher management with lifecycle cleanup
- `PassiveChallengeWarmer.kt` - Two-phase interface: register() + start()
- `PassiveChallengeWarmerActivity.kt` - Dedicated warmup activity with singleInstance launch mode
- `PassiveChallengeWarmerActivityContract.kt` - ActivityResultContract implementation
- `PassiveChallengeWarmerArgs.kt` - Parcelable data class for activity arguments
- `PassiveChallengeWarmerComponent.kt` - Dagger component with PassiveCaptchaParams binding
- `PassiveChallengeWarmerViewModel.kt` - Coordinates warmup through HCaptchaService
- `PassiveChallengeWarmerModule.kt` - Dagger module for warmer provision

**Integration Points:**
- `paymentsheet/src/main/java/com/stripe/android/paymentsheet/PaymentSheetViewModel.kt` - Warmer registration and hardcoded params (TODO: metadata)
- `paymentsheet/src/main/java/com/stripe/android/paymentsheet/injection/PaymentSheetLauncherComponent.kt` - DI integration

**Testing (10 enhanced/new files):**
- Enhanced `DefaultHCaptchaServiceTest.kt` with cache state validation
- Updated `FakeHCaptchaService.kt` with separate warmUp tracking
- Enhanced existing challenge tests to use `ChallengeFactory`
- Added complete test suites for warmer system: activity, viewmodel, and contract tests

**Configuration:**
- `payments-core/AndroidManifest.xml` - Warmer activity with singleInstance launch mode
- Dagger component architecture for isolated warmup dependency injection