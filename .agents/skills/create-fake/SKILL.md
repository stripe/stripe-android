---
name: create-fake
description: Use when creating or changing a fake test implementation in stripe-android — mandates Turbine for all interaction tracking and covers FakeClassName, ViewActionRecorder, and ensureAllEventsConsumed validation
---

# Creating Fakes for Testing

This skill describes how to create fake implementations for testing in the Stripe Android SDK. The codebase **strongly prefers fakes over mocks** for better test reliability and clarity.

## Core Principles

1. **Prefer fakes over mocks** - Create `FakeClassName` implementations that provide controllable, inspectable behavior
2. **Use Turbine for every recorded interaction** - Track calls, callbacks, and lifecycle events with Turbine channels
3. **Provide default parameters** - Make fakes easy to instantiate with sensible defaults
4. **Require validation** - Implement `ensureAllEventsConsumed()` and call it after every test scenario

## Interaction Tracking Is Always Turbine

Use `Turbine<Call>()` for all history that a test observes. This includes method calls with parameters, parameterless lifecycle calls such as `unregister()`, and callback registration.

Never use these to record interactions:

- `MutableList`, `mutableListOf`, or `list += call`
- `callCount` integer properties
- `wasCalled` or `invoked` boolean properties
- A callback field that bypasses recording the registration call

Mutable properties are appropriate only for configuring fake behavior, such as the value a method returns or an error it should throw. They are not appropriate for recording what the system under test did.

```kotlin
internal class FakeLauncher(
    var launchResult: Boolean = false, // Configuration is mutable.
) : Launcher {
    val registerCalls = Turbine<RegisterCall>()
    val launchCalls = Turbine<LaunchCall>()
    val unregisterCalls = Turbine<Unit>()

    override fun register(callback: () -> Unit) {
        registerCalls.add(RegisterCall(callback))
    }

    override fun launch(id: String): Boolean {
        launchCalls.add(LaunchCall(id))
        return launchResult
    }

    override fun unregister() {
        unregisterCalls.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        registerCalls.ensureAllEventsConsumed()
        launchCalls.ensureAllEventsConsumed()
        unregisterCalls.ensureAllEventsConsumed()
    }

    data class RegisterCall(val callback: () -> Unit)
    data class LaunchCall(val id: String)
}
```

Retrieve registered callbacks from the recorded call with `registerCalls.awaitItem()` and invoke that callback in the test. Do not add a separate callback shortcut that hides whether registration occurred.

## Basic Fake Structure

### File Naming and Location
- Place fakes in the test source directory: `src/test/java/com/stripe/android/.../FakeClassName.kt`
- Name pattern: `Fake` + interface/class name (e.g., `FakeEventReporter`, `FakeCustomerRepository`)
- Mark as `internal` to scope to the test module

### Constructor Pattern
Always use **default parameters** to make instantiation easy:

```kotlin
internal class FakeCustomerRepository(
    private val paymentMethods: List<PaymentMethod> = emptyList(),
    private val customer: Customer? = null
) : CustomerRepository {
    // Implementation
}
```

For complex setup, provide a **companion object factory method**:

```kotlin
internal class FakePaymentMethodVerticalLayoutInteractor(
    initialState: PaymentMethodVerticalLayoutInteractor.State,
    initialShowsWalletsHeader: Boolean = false,
    private val viewActionRecorder: ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction>
) : PaymentMethodVerticalLayoutInteractor {

    companion object {
        fun create(
            paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            initialShowsWalletsHeader: Boolean = true,
            viewActionRecorder: ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction> = ViewActionRecorder()
        ): FakePaymentMethodVerticalLayoutInteractor {
            // Complex initialization logic
            val initialState = /* construct complex state */
            return FakePaymentMethodVerticalLayoutInteractor(
                initialState = initialState,
                initialShowsWalletsHeader = initialShowsWalletsHeader,
                viewActionRecorder = viewActionRecorder
            )
        }
    }
}
```

## Tracking Method Calls with Turbine

### Basic Turbine Pattern

Directly expose Turbines for test verification:

```kotlin
internal class FakeEventReporter : EventReporter {
    val paymentFailureCalls = Turbine<PaymentFailureCall>()
    val paymentSuccessCalls = Turbine<PaymentSuccessCall>()

    override fun onPaymentFailure(error: Throwable, source: PaymentEventSource) {
        paymentFailureCalls.add(PaymentFailureCall(error, source))
    }

    override fun onPaymentSuccess(paymentMethod: PaymentMethod) {
        paymentSuccessCalls.add(PaymentSuccessCall(paymentMethod))
    }
}
```

### Data Classes for Call Capture

Define **data classes** to capture method call parameters:

```kotlin
data class PaymentFailureCall(val error: Throwable, val source: PaymentEventSource)
data class PaymentSuccessCall(val paymentMethod: PaymentMethod)
data class DetachRequest(val paymentMethodId: String, val customerId: String)
```

### Validation with ensureAllEventsConsumed

Implement a **validation method** that includes every Turbine and ensures all events were consumed:

```kotlin
fun ensureAllEventsConsumed() {
    paymentFailureCalls.ensureAllEventsConsumed()
    paymentSuccessCalls.ensureAllEventsConsumed()
    detachRequests.ensureAllEventsConsumed()
    updateRequests.ensureAllEventsConsumed()
    // ... validate all turbines
}
```

Every scenario should call this method after verification, including scenarios where no calls are expected:

```kotlin
@Test
fun `test payment flow`() = runTest {
    val fake = FakeEventReporter()

    // Perform operations
    fake.onPaymentSuccess(paymentMethod)

    // Verify calls
    assertThat(fake.paymentSuccessCalls.awaitItem()).isEqualTo(
        PaymentSuccessCall(paymentMethod)
    )

    // Validate all events consumed
    fake.ensureAllEventsConsumed()
}
```

## ViewActionRecorder Pattern

For classes that handle view actions, use **ViewActionRecorder**:

```kotlin
internal class FakePaymentMethodVerticalLayoutInteractor(
    initialState: PaymentMethodVerticalLayoutInteractor.State,
    private val viewActionRecorder: ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction>
) : PaymentMethodVerticalLayoutInteractor {

    override fun handleViewAction(viewAction: PaymentMethodVerticalLayoutInteractor.ViewAction) {
        viewActionRecorder.record(viewAction)
        // Optional: implement state changes based on action
    }
}
```

**Include ViewActionRecorder in factory with default**:

```kotlin
companion object {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        viewActionRecorder: ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction> = ViewActionRecorder()
    ): FakePaymentMethodVerticalLayoutInteractor {
        return FakePaymentMethodVerticalLayoutInteractor(
            initialState = /* ... */,
            viewActionRecorder = viewActionRecorder
        )
    }
}
```

## Excellent Real-World Examples

Reference these fakes from the codebase as gold standards:

### **FakeEventReporter**
`paymentsheet/src/test/java/com/stripe/android/paymentsheet/analytics/FakeEventReporter.kt`
- 16 different Turbine channels for comprehensive event tracking
- Clean `validate()` method checking all turbines
- Data classes for each event type
- Gold standard for Turbine usage

### **FakeCustomerRepository**
`paymentsheet/src/test/java/com/stripe/android/utils/FakeCustomerRepository.kt`
- Excellent use of default parameters
- Multiple Turbines for tracking different operations (detach, update, setDefault)
- Data classes for request tracking

### **FakePaymentMethodVerticalLayoutInteractor**
`paymentsheet/src/test/java/com/stripe/android/paymentsheet/verticalmode/FakePaymentMethodVerticalLayoutInteractor.kt`
- ViewActionRecorder integration
- Companion object factory method with sophisticated defaults

## Quick Reference

- **Need to track method calls?** → Use Turbine with data classes
- **Need to track a parameterless call?** → Use `Turbine<Unit>`
- **Need to invoke a registered callback?** → Capture it in a Turbine call and retrieve it with `awaitItem()`
- **Tracking view actions?** → Use ViewActionRecorder
- **Need to verify all events consumed?** → Implement `ensureAllEventsConsumed()` that calls it on all Turbines
- **Mutable list, counter, or invoked flag?** → Replace it with a Turbine; mutable fields are only for fake configuration
- **Complex initialization?** → Add companion object `create()` factory method
- **Always** → Provide default parameters for easy instantiation
