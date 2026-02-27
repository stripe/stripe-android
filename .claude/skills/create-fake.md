# Creating Fakes for Testing

This skill describes how to create fake implementations for testing in the Stripe Android SDK. The codebase **strongly prefers fakes over mocks** for better test reliability and clarity.

## Core Principles

1. **Prefer fakes over mocks** - Create `FakeClassName` implementations that provide controllable, inspectable behavior
2. **Use Turbine for call tracking** - Track method invocations with Turbine channels for verification
3. **Provide default parameters** - Make fakes easy to instantiate with sensible defaults
4. **Enable validation** - Implement `ensureAllEventsConsumed()` validation method when using Turbines

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

Implement a **validation method** that ensures all turbine events were consumed:

```kotlin
fun ensureAllEventsConsumed() {
    paymentFailureCalls.ensureAllEventsConsumed()
    paymentSuccessCalls.ensureAllEventsConsumed()
    detachRequests.ensureAllEventsConsumed()
    updateRequests.ensureAllEventsConsumed()
    // ... validate all turbines
}
```

Tests should call this method after verification:

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
- **Tracking view actions?** → Use ViewActionRecorder
- **Need to verify all events consumed?** → Implement `ensureAllEventsConsumed()` that calls it on all Turbines
- **Complex initialization?** → Add companion object `create()` factory method
- **Always** → Provide default parameters for easy instantiation
