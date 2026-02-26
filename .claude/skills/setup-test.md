# Setting Up Tests

This skill describes how to structure tests in the Stripe Android SDK using fakes, scenarios, and proper verification patterns.

## Core Principles

1. **Use fakes over mocks** - Leverage fake implementations for dependencies (see `create-fake.md`)
2. **Create test scenarios** - Use Scenario classes with `runScenario` functions to organize test setup
3. **Verify all events consumed** - Call `validate()` or `ensureAllEventsConsumed()` on fakes after test block
4. **Use Turbine for Flow testing** - Test Flow emissions with Turbine's `.test { }` syntax

## Basic Test Structure

Every test should follow this pattern:

```kotlin
@Test
fun `test description`() = runScenario(
    // Test-specific parameters
    config = testConfig
) {
    // 1. Configure: Set up fake behaviors (optional)
    fakeService.result = expectedResult

    // 2. Execute: Call the code under test
    val result = systemUnderTest.doSomething()

    // 3. Verify: Assert results and check fake calls
    assertThat(result).isEqualTo(expected)
    assertThat(fakeService.calls.awaitItem()).isEqualTo(expectedCall)
}
// 4. Validation: ensureAllEventsConsumed called automatically by runScenario
```

## Scenario Pattern with runScenario

### Basic Structure

Create a `runScenario` function and a `Scenario` class at the bottom of your test file:

```kotlin
class MyFeatureTest {
    @Test
    fun `test case`() = runScenario {
        // Test code using scenario fields
        assertThat(systemUnderTest.getValue()).isEqualTo(expectedValue)
    }

    private fun runScenario(
        config: Config = defaultConfig,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        // Setup fakes
        val fakeRepository = FakeRepository()
        val fakeAnalytics = FakeAnalytics()

        // Create system under test
        val systemUnderTest = MyFeature(
            repository = fakeRepository,
            analytics = fakeAnalytics,
            config = config
        )

        // Run test block with scenario context
        block(
            Scenario(
                systemUnderTest = systemUnderTest,
                fakeRepository = fakeRepository,
                fakeAnalytics = fakeAnalytics,
            )
        )

        // Validate all fakes
        fakeRepository.ensureAllEventsConsumed()
        fakeAnalytics.ensureAllEventsConsumed()
    }

    private class Scenario(
        val systemUnderTest: MyFeature,
        val fakeRepository: FakeRepository,
        val fakeAnalytics: FakeAnalytics,
    )
}
```

**Key Features:**
- `runScenario` replaces `runTest` as the test entry point
- Default parameters for all configuration make tests concise
- Trailing lambda provides DSL-like syntax with scenario fields
- `ensureAllEventsConsumed()` called automatically after test block
- Scenario class holds system under test and all fakes

### Using runScenario in Tests

```kotlin
@Test
fun `fetching data returns success when repository succeeds`() = runScenario {
    // Configure fake behavior
    fakeRepository.dataResult = Result.success(testData)

    // Execute
    val result = systemUnderTest.fetchData()

    // Verify
    assertThat(result.isSuccess).isTrue()
    assertThat(fakeRepository.fetchCalls.awaitItem()).isEqualTo(FetchCall(userId = "123"))
}
// ensureAllEventsConsumed called automatically
```

### runScenario with Custom Configuration

Override default parameters for specific test needs:

```kotlin
@Test
fun `uses custom timeout when configured`() = runScenario(
    config = Config(timeout = 5000),
    enableLogging = true
) {
    systemUnderTest.performAction()

    assertThat(fakeRepository.calls.awaitItem().timeout).isEqualTo(5000)
}
```

### Alternative: validate() Method on Fakes

Some fakes provide a `validate()` method instead of direct `ensureAllEventsConsumed()`:

```kotlin
private fun runScenario(
    block: suspend Scenario.() -> Unit,
) = runTest {
    val factory = FakeLinkFormElementFactory()
    val helper = DefaultHelper(factory)

    block(
        Scenario(
            helper = helper,
            factoryCalls = factory.calls,
        )
    )

    factory.validate() // Calls ensureAllEventsConsumed internally
}
```

## Verifying Fake Calls

### Using Turbine for Call Verification

```kotlin
@Test
fun `payment success triggers analytics event`() = runScenario {
    fakePaymentService.result = PaymentResult.Success(paymentMethod)

    systemUnderTest.processPayment(amount = 1000)

    // Verify analytics call
    val analyticsCall = fakeAnalytics.paymentSuccessCalls.awaitItem()
    assertThat(analyticsCall.amount).isEqualTo(1000)
    assertThat(analyticsCall.paymentMethod).isEqualTo(paymentMethod)
}
```

### Verifying Multiple Calls in Sequence

```kotlin
@Test
fun `flow makes multiple repository calls`() = runScenario {
    systemUnderTest.complexFlow()

    // Verify calls in order
    assertThat(fakeRepository.calls.awaitItem()).isEqualTo(Call.FetchUser)
    assertThat(fakeRepository.calls.awaitItem()).isEqualTo(Call.FetchPaymentMethods)
    assertThat(fakeRepository.calls.awaitItem()).isEqualTo(Call.UpdateCache)
}
```

### Verifying No Calls Were Made

```kotlin
@Test
fun `does not call repository when cache is valid`() = runScenario {
    systemUnderTest.getCachedData()

    // Verify no repository calls
    fakeRepository.calls.expectNoEvents()
}
```

## Flow Testing with Turbine

### Testing StateFlow/SharedFlow Emissions

```kotlin
@Test
fun `state updates when data is loaded`() = runScenario {
    systemUnderTest.state.test {
        // Verify initial state
        assertThat(awaitItem()).isEqualTo(State.Loading)

        // Trigger state change
        systemUnderTest.loadData()

        // Verify new state
        assertThat(awaitItem()).isEqualTo(State.Loaded(testData))

        // Verify no more emissions
        ensureAllEventsConsumed()
    }
}
```

### Testing Expected No Events

```kotlin
@Test
fun `state does not emit when already loaded`() = runScenario {
    systemUnderTest.state.test {
        skipItems(1) // Skip initial state

        // Call method that shouldn't emit
        systemUnderTest.refreshIfNeeded()

        // Verify no emission
        expectNoEvents()
    }
}
```

## Accessing Scenario Fields

The scenario provides access to the system under test and all fakes:

```kotlin
@Test
fun `scenario provides access to all components`() = runScenario(
    initialState = State.LoggedIn
) {
    // Access system under test
    val result = systemUnderTest.performAction()

    // Access fakes for verification
    assertThat(fakeRepository.calls.awaitItem()).isNotNull()
    assertThat(fakeAnalytics.events.awaitItem()).isNotNull()

    // Access other scenario fields
    assertThat(configuredTimeout).isEqualTo(5000)
}
```

## Excellent Real-World Example

### **DefaultSavedPaymentMethodLinkFormHelperTest**
`paymentsheet/src/test/java/com/stripe/android/common/spms/DefaultSavedPaymentMethodLinkFormHelperTest.kt`

This is the **gold standard** for the Scenario pattern:

```kotlin
@Test
fun `link form is unavailable when signup mode is not defined`() = runScenario(
    linkState = LinkState(
        configuration = TestFactory.LINK_CONFIGURATION,
        loginState = LinkState.LoginState.LoggedOut,
        signupMode = null,
    ),
) {
    helper.state.test {
        assertThat(awaitItem()).isEqualTo(SavedPaymentMethodLinkFormHelper.State.Unused)
    }

    assertThat(helper.formElement).isNull()
    formElementFactoryCreateCalls.expectNoEvents()
}

private fun runScenario(
    linkState: LinkState? = null,
    paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        linkState = linkState,
    ),
    handle: SavedStateHandle = SavedStateHandle(),
    linkConfigurationCoordinator: LinkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
    block: suspend Scenario.() -> Unit,
) = runTest {
    val factory = FakeLinkFormElementFactory()

    val helper = DefaultSavedPaymentMethodLinkFormHelper(
        paymentMethodMetadata = paymentMethodMetadata,
        linkConfigurationCoordinator = linkConfigurationCoordinator,
        savedStateHandle = handle,
        linkFormElementFactory = factory,
    )

    block(
        Scenario(
            helper = helper,
            handle = handle,
            formElementFactoryCreateCalls = factory.calls,
        )
    )

    factory.validate() // Calls ensureAllEventsConsumed
}

private class Scenario(
    val helper: SavedPaymentMethodLinkFormHelper,
    val handle: SavedStateHandle,
    val formElementFactoryCreateCalls: ReceiveTurbine<FakeLinkFormElementFactory.Call>,
)
```

**Best Practices Shown:**
- `runScenario` with comprehensive default parameters
- `runTest` called inside `runScenario`, not by tests
- Scenario class holds system under test and fake references
- `validate()` called on fakes after test block
- Tests use `runScenario` directly without `runTest`

## Quick Reference

- **Organizing tests?** → Create `runScenario` function with Scenario class
- **Running tests?** → Use `runScenario { }` instead of `runTest { }`
- **Testing Flows?** → Use `.test { }` from Turbine inside scenario block
- **Verifying calls?** → Use `awaitItem()` on Turbine channels in scenario
- **Multiple sequential calls?** → Call `awaitItem()` multiple times
- **Always** → Call `ensureAllEventsConsumed()` or `validate()` on fakes after scenario block

## Complete Example

```kotlin
class PaymentProcessorTest {
    @Test
    fun `processing payment triggers success analytics when payment succeeds`() = runScenario {
        // Configure fake behavior
        fakePaymentService.result = PaymentResult.Success(PAYMENT_METHOD)

        // Execute system under test
        val result = processor.processPayment(amount = 5000)

        // Verify result
        assertThat(result.isSuccess).isTrue()

        // Verify service was called correctly
        val serviceCall = fakePaymentService.processCalls.awaitItem()
        assertThat(serviceCall.amount).isEqualTo(5000)

        // Verify analytics event was fired
        val analyticsCall = fakeAnalytics.successCalls.awaitItem()
        assertThat(analyticsCall.amount).isEqualTo(5000)
        assertThat(analyticsCall.paymentMethod).isEqualTo(PAYMENT_METHOD)
    }

    @Test
    fun `processing with custom timeout uses correct timeout`() = runScenario(
        timeout = 10000
    ) {
        processor.processPayment(amount = 1000)

        val call = fakePaymentService.processCalls.awaitItem()
        assertThat(call.timeout).isEqualTo(10000)
    }

    private fun runScenario(
        timeout: Int = 5000,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakePaymentService = FakePaymentService()
        val fakeAnalytics = FakeAnalytics()

        val processor = PaymentProcessor(
            paymentService = fakePaymentService,
            analytics = fakeAnalytics,
            timeout = timeout
        )

        block(
            Scenario(
                processor = processor,
                fakePaymentService = fakePaymentService,
                fakeAnalytics = fakeAnalytics,
            )
        )

        // Validate all fakes after test
        fakePaymentService.ensureAllEventsConsumed()
        fakeAnalytics.ensureAllEventsConsumed()
    }

    private class Scenario(
        val processor: PaymentProcessor,
        val fakePaymentService: FakePaymentService,
        val fakeAnalytics: FakeAnalytics,
    )
}
```
