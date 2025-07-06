package com.stripe.android.paymentsheet.addresselement

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class DefaultAutocompleteLauncherTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val appearance = PaymentSheet.Appearance.Builder()
        .colorsLight(PaymentSheet.Colors.defaultDark)
        .build()

    private val addressDetails = AddressDetails(
        name = "John Doe",
        address = PaymentSheet.Address(
            line1 = "123 Main Street",
            line2 = "Apt 4B",
            city = "San Francisco",
            state = "CA",
            postalCode = "94105",
            country = "US"
        ),
        phoneNumber = "555-123-4567",
        isCheckboxSelected = true
    )

    @Test
    fun `register sets up activity result launcher`() = test {
        val launcher = createLauncher()

        launcher.register(activityResultCaller, TestLifecycleOwner())

        val registerCall = awaitRegisterCall()
        assertThat(registerCall.contract).isEqualTo(AutocompleteContract)

        assertThat(awaitNextRegisteredLauncher()).isNotNull()
    }

    @Test
    fun `launch does nothing when not registered`() = runTest {
        val launcher = createLauncher()

        launcher.launch(country = "US", googlePlacesApiKey = "test-api-key") {
            error("Should not be called!")
        }
    }

    @Test
    fun `launch calls activity launcher with correct arguments after register`() = test {
        val launcher = createLauncher()

        launcher.register(activityResultCaller, TestLifecycleOwner())

        assertThat(awaitRegisterCall()).isNotNull()
        assertThat(awaitNextRegisteredLauncher()).isNotNull()

        launcher.launch(country = "US", googlePlacesApiKey = "test-api-key") {
            error("Should not be called!")
        }

        val launchArgs = awaitLaunchCall()

        assertThat(launchArgs).isInstanceOf<AutocompleteContract.Args>()

        val autocompleteArgs = launchArgs as AutocompleteContract.Args

        assertThat(autocompleteArgs.country).isEqualTo("US")
        assertThat(autocompleteArgs.googlePlacesApiKey).isEqualTo("test-api-key")
        assertThat(autocompleteArgs.appearanceContext).isEqualTo(
            AutocompleteAppearanceContext.PaymentElement(appearance)
        )
    }

    @Test
    fun `launch creates unique IDs for multiple calls`() = test {
        val launcher = createLauncher()

        launcher.register(activityResultCaller, TestLifecycleOwner())

        assertThat(awaitRegisterCall()).isNotNull()
        assertThat(awaitNextRegisteredLauncher()).isNotNull()

        launcher.launch(country = "US", googlePlacesApiKey = "test-api-key") {
            error("Should not be called!")
        }

        launcher.launch(country = "CA", googlePlacesApiKey = "test-api-key") {
            error("Should not be called!")
        }

        val firstLaunchArguments = awaitLaunchCall()

        assertThat(firstLaunchArguments).isInstanceOf<AutocompleteContract.Args>()

        val firstAutocompleteArgs = firstLaunchArguments as AutocompleteContract.Args

        assertThat(firstAutocompleteArgs.id).isEqualTo(firstAutocompleteArgs.id)
        assertThat(firstAutocompleteArgs.country).isEqualTo("US")
        assertThat(firstAutocompleteArgs.googlePlacesApiKey).isEqualTo("test-api-key")

        val secondLaunchArguments = awaitLaunchCall()

        assertThat(secondLaunchArguments).isInstanceOf<AutocompleteContract.Args>()

        val secondAutocompleteArgs = secondLaunchArguments as AutocompleteContract.Args

        assertThat(secondAutocompleteArgs.id).isEqualTo(secondAutocompleteArgs.id)
        assertThat(secondAutocompleteArgs.country).isEqualTo("CA")
        assertThat(secondAutocompleteArgs.googlePlacesApiKey).isEqualTo("test-api-key")
    }

    @Test
    fun `launch passes PE context with custom appearance to contract args`() = test {
        val customAppearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.configureDefaultLight(
                primary = Color.Red
            )
        )
        val launcher = createLauncher(
            appearanceContext = AutocompleteAppearanceContext.PaymentElement(customAppearance)
        )

        launcher.register(activityResultCaller, TestLifecycleOwner())

        assertThat(awaitRegisterCall()).isNotNull()
        assertThat(awaitNextRegisteredLauncher()).isNotNull()

        launcher.launch(country = "US", googlePlacesApiKey = "test-api-key") {
            error("Should not be called!")
        }

        val launchArgs = awaitLaunchCall()

        assertThat(launchArgs).isInstanceOf<AutocompleteContract.Args>()

        val autocompleteArgs = launchArgs as AutocompleteContract.Args

        assertThat(autocompleteArgs.appearanceContext)
            .isEqualTo(AutocompleteAppearanceContext.PaymentElement(customAppearance))
    }

    @Test
    fun `launch passes Link context with custom appearance to contract args`() = test {
        val launcher = createLauncher(AutocompleteAppearanceContext.Link)

        launcher.register(activityResultCaller, TestLifecycleOwner())

        assertThat(awaitRegisterCall()).isNotNull()
        assertThat(awaitNextRegisteredLauncher()).isNotNull()

        launcher.launch(country = "US", googlePlacesApiKey = "test-api-key") {
            error("Should not be called!")
        }

        val launchArgs = awaitLaunchCall()

        assertThat(launchArgs).isInstanceOf<AutocompleteContract.Args>()

        val autocompleteArgs = launchArgs as AutocompleteContract.Args

        assertThat(autocompleteArgs.appearanceContext)
            .isEqualTo(AutocompleteAppearanceContext.Link)
    }

    @Test
    fun `activity result callback converts EnterManually result correctly`() = test {
        val launcher = createLauncher()

        var capturedResult: AutocompleteLauncher.Result? = null

        launcher.register(activityResultCaller, TestLifecycleOwner())

        val registerCall = awaitRegisterCall()

        assertThat(registerCall.contract).isEqualTo(AutocompleteContract)
        assertThat(awaitNextRegisteredLauncher()).isNotNull()

        launcher.launch(country = "US", googlePlacesApiKey = "test-api-key") {
            capturedResult = it
        }

        val launchArgs = awaitLaunchCall()

        assertThat(launchArgs).isInstanceOf<AutocompleteContract.Args>()

        val autocompleteArgs = launchArgs as AutocompleteContract.Args

        val result = AutocompleteContract.Result.EnterManually(
            id = autocompleteArgs.id,
            addressDetails = addressDetails
        )

        registerCall.callback.asCallbackFor<AutocompleteContract.Result>().onActivityResult(result)

        assertThat(capturedResult).isInstanceOf(AutocompleteLauncher.Result.EnterManually::class.java)

        val enterManuallyResult = capturedResult as AutocompleteLauncher.Result.EnterManually

        assertThat(enterManuallyResult.addressDetails).isEqualTo(addressDetails)
    }

    @Test
    fun `activity result callback converts Address result correctly`() = test {
        val launcher = createLauncher()

        var capturedResult: AutocompleteLauncher.Result? = null

        launcher.register(activityResultCaller, TestLifecycleOwner())

        val registerCall = awaitRegisterCall()

        assertThat(registerCall.contract).isEqualTo(AutocompleteContract)
        assertThat(awaitNextRegisteredLauncher()).isNotNull()

        launcher.launch(country = "US", googlePlacesApiKey = "test-api-key") {
            capturedResult = it
        }

        val launchArgs = awaitLaunchCall()

        assertThat(launchArgs).isInstanceOf<AutocompleteContract.Args>()

        val autocompleteArgs = launchArgs as AutocompleteContract.Args

        val result = AutocompleteContract.Result.Address(
            id = autocompleteArgs.id,
            addressDetails = addressDetails
        )

        registerCall.callback.asCallbackFor<AutocompleteContract.Result>().onActivityResult(result)

        assertThat(capturedResult).isInstanceOf<AutocompleteLauncher.Result.OnBack>()

        val onBackResult = capturedResult as AutocompleteLauncher.Result.OnBack

        assertThat(onBackResult.addressDetails).isEqualTo(addressDetails)
    }

    @Test
    fun `activity result callback removes listener after invocation`() = test {
        val launcher = createLauncher()
        var callbackCalledCount = 0

        launcher.register(activityResultCaller, TestLifecycleOwner())

        val registerCall = awaitRegisterCall()

        assertThat(registerCall.contract).isEqualTo(AutocompleteContract)
        assertThat(awaitNextRegisteredLauncher()).isNotNull()

        launcher.launch(country = "US", googlePlacesApiKey = "test-api-key") {
            callbackCalledCount++
        }

        val launchArgs = awaitLaunchCall()

        assertThat(launchArgs).isInstanceOf<AutocompleteContract.Args>()

        val autocompleteArgs = launchArgs as AutocompleteContract.Args

        val result = AutocompleteContract.Result.EnterManually(
            id = autocompleteArgs.id,
            addressDetails = addressDetails
        )

        val callback = registerCall.callback.asCallbackFor<AutocompleteContract.Result>()

        callback.onActivityResult(result)
        assertThat(callbackCalledCount).isEqualTo(1)

        callback.onActivityResult(result)
        assertThat(callbackCalledCount).isEqualTo(1)
    }

    @Test
    fun `lifecycle observer unregisters activity launcher on destroy`() = test {
        val launcher = createLauncher()
        val lifecycleOwner = TestLifecycleOwner()

        launcher.register(activityResultCaller, lifecycleOwner)

        assertThat(awaitRegisterCall()).isNotNull()

        val registeredLauncher = awaitNextRegisteredLauncher()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        val unregisteredLauncher = awaitNextUnregisteredLauncher()
        assertThat(unregisteredLauncher).isEqualTo(registeredLauncher)
    }

    @Test
    fun `multiple registrations replace previous launcher`() = test {
        val launcher = createLauncher()
        val lifecycleOwner1 = TestLifecycleOwner()
        val lifecycleOwner2 = TestLifecycleOwner()

        // First registration
        launcher.register(activityResultCaller, lifecycleOwner1)
        awaitRegisterCall()
        awaitNextRegisteredLauncher()

        // Second registration should work without issues
        launcher.register(activityResultCaller, lifecycleOwner2)
        awaitRegisterCall()
        awaitNextRegisteredLauncher()
    }

    @Test
    fun `launch with different result callbacks maintains separation`() = test {
        val launcher = createLauncher()

        var firstReceivedResult: AutocompleteLauncher.Result? = null
        var secondReceivedResult: AutocompleteLauncher.Result? = null

        launcher.register(activityResultCaller, TestLifecycleOwner())

        val registerCall = awaitRegisterCall()

        assertThat(registerCall.contract).isEqualTo(AutocompleteContract)
        assertThat(awaitNextRegisteredLauncher()).isNotNull()

        launcher.launch(country = "US", googlePlacesApiKey = "test-api-key") {
            firstReceivedResult = it
        }

        launcher.launch(country = "CA", googlePlacesApiKey = "test-api-key") {
            secondReceivedResult = it
        }

        val firstLaunchCall = awaitLaunchCall()

        assertThat(firstLaunchCall).isInstanceOf<AutocompleteContract.Args>()

        val firstAutocompleteArgs = firstLaunchCall as AutocompleteContract.Args

        val secondLaunchCall = awaitLaunchCall()

        assertThat(firstLaunchCall).isInstanceOf<AutocompleteContract.Args>()

        val secondAutocompleteArgs = secondLaunchCall as AutocompleteContract.Args

        val callback = registerCall.callback.asCallbackFor<AutocompleteContract.Result>()

        val firstAutocompleteResult = AutocompleteContract.Result.EnterManually(
            id = firstAutocompleteArgs.id,
            addressDetails = addressDetails
        )

        callback.onActivityResult(firstAutocompleteResult)

        val secondAutocompleteResult = AutocompleteContract.Result.Address(
            id = secondAutocompleteArgs.id,
            addressDetails = addressDetails
        )
        callback.onActivityResult(secondAutocompleteResult)

        assertThat(firstReceivedResult).isInstanceOf(AutocompleteLauncher.Result.EnterManually::class.java)
        assertThat(secondReceivedResult).isInstanceOf(AutocompleteLauncher.Result.OnBack::class.java)
    }

    private fun test(
        test: suspend DummyActivityResultCaller.Scenario.() -> Unit
    ) = runTest {
        DummyActivityResultCaller.test(test)
    }

    private fun createLauncher(
        appearanceContext: AutocompleteAppearanceContext =
            AutocompleteAppearanceContext.PaymentElement(appearance)
    ) = DefaultAutocompleteLauncher(appearanceContext)
}
