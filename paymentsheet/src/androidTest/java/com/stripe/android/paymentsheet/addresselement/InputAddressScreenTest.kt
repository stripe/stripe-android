package com.stripe.android.paymentsheet.addresselement

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import com.stripe.android.uicore.DefaultStripeTheme
import kotlinx.coroutines.flow.Flow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

@RunWith(AndroidJUnit4::class)
class InputAddressScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clicking_primary_button_triggers_callback_when_enabled() {
        var counter = 0
        setContent(primaryButtonEnabled = true, primaryButtonCallback = { counter++ })
        composeTestRule.onNodeWithText("Save Address").performClick()
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun clicking_primary_button_does_not_trigger_callback_when_not_enabled() {
        var counter = 0
        setContent(primaryButtonEnabled = false, primaryButtonCallback = { counter++ })
        composeTestRule.onNodeWithText("Save Address").performClick()
        assertThat(counter).isEqualTo(0)
    }

    @Test
    fun clicking_close_button_triggers_callback() {
        var counter = 0
        setContent(onCloseCallback = { counter++ })
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun clicking_close_button_publishes_cancellation_in_stateful_screen() {
        val resultStateHolder = AddressElementResultStateHolder()
        val viewModel = InputAddressViewModel(
            args = AddressElementActivityContract.Args(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration(),
            ),
            navigator = TestNavigator,
            resultStateHolder = resultStateHolder,
            eventReporter = TestEventReporter,
            placesClient = null,
        )

        setContent(viewModel)

        composeTestRule.onNodeWithContentDescription("Close").performClick()

        assertThat(resultStateHolder.result.value)
            .isEqualTo(AddressLauncherResult.Canceled())
    }

    private fun setContent(
        primaryButtonEnabled: Boolean = true,
        primaryButtonCallback: () -> Unit = {},
        onCloseCallback: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            DefaultStripeTheme {
                InputAddressScreen(
                    primaryButtonEnabled = primaryButtonEnabled,
                    primaryButtonText = "Save Address",
                    title = "Address",
                    onPrimaryButtonClick = primaryButtonCallback,
                    onDisabledButtonClick = {},
                    onCloseClick = onCloseCallback,
                    topContent = {},
                    formContent = {},
                    bottomContent = {}
                )
            }
        }
    }

    private fun setContent(viewModel: InputAddressViewModel) {
        val subcomponentFactory = object : InputAddressViewModelSubcomponent.Factory {
            override fun create(): InputAddressViewModelSubcomponent =
                object : InputAddressViewModelSubcomponent {
                    override val inputAddressViewModel = viewModel
                }
        }

        composeTestRule.setContent {
            DefaultStripeTheme {
                InputAddressScreen(
                    inputAddressViewModelSubcomponentFactoryProvider = object :
                        Provider<InputAddressViewModelSubcomponent.Factory> {
                        override fun get(): InputAddressViewModelSubcomponent.Factory =
                            subcomponentFactory
                    }
                )
            }
        }
    }

    private object TestNavigator : AddressElementNavigator {
        override fun navigateTo(target: AddressElementScreen) = Unit

        override fun setResult(key: String, value: Any?) = Unit

        override fun <T : Any?> getResultFlow(key: String): Flow<T>? = null

        override fun onBack() = false
    }

    private object TestEventReporter : AddressLauncherEventReporter {
        override fun onShow(country: String) = Unit

        override fun onCompleted(
            country: String,
            autocompleteResultSelected: Boolean,
            editDistance: Int?,
        ) = Unit

        override fun onAutocompleteSessionStarted(sessionToken: String) = Unit

        override fun onAutocompleteFetchStarted() = Unit

        override fun onAutocompleteSuggestionsReturned(
            sessionToken: String,
            resultCount: Int,
            source: String?,
        ) = Unit

        override fun onAutocompleteDetailsFetchStarted() = Unit

        override fun onAutocompleteSelected(
            sessionToken: String,
            queryLength: Int,
            placeId: String?,
            source: String?,
        ) = Unit

        override fun onAutocompleteError(sessionToken: String, error: Throwable) = Unit
    }
}
