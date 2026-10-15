package com.stripe.android.paymentsheet.addresselement

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.ui.PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.paymentelement.AddressElementSameAsBillingPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressElementEventReporter
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
internal class InputAddressScreenTest {
    private val composeRule = createComposeRule()
    private val viewModelStoreRule = ViewModelStoreTestRule()

    @get:Rule
    val rules: RuleChain = RuleChain.emptyRuleChain()
        .around(viewModelStoreRule)
        .around(createComposeCleanupRule())
        .around(composeRule)
        .around(CoroutineTestRule())

    @Test
    fun `completed save hides loading while keeping controls disabled`() = runScenario {
        assertControlsEnabled()
        assertNotLoading()

        composeRule.runOnIdle {
            assertThat(resultStateHolder.tryStartSaving()).isTrue()
        }
        assertControlsDisabled()
        assertLoading()

        composeRule.runOnIdle {
            resultStateHolder.onSaveCompleted(
                AddressElementActivityContract.Result.StandaloneSucceeded(ADDRESS)
            )
        }
        assertControlsDisabled()
        assertNotLoading()
    }

    @Test
    fun `failed save hides loading and enables controls again`() = runScenario {
        assertControlsEnabled()
        assertNotLoading()

        composeRule.runOnIdle {
            assertThat(resultStateHolder.tryStartSaving()).isTrue()
        }
        assertControlsDisabled()
        assertLoading()

        composeRule.runOnIdle {
            resultStateHolder.onSaveFailed()
        }
        assertControlsEnabled()
        assertNotLoading()
    }

    @OptIn(AddressElementSameAsBillingPreview::class)
    private fun runScenario(block: Scenario.() -> Unit) = runTest {
        TestAddressElementNavigator.test {
            val resultStateHolder = AddressElementResultStateHolder()
            val eventReporter = FakeAddressElementEventReporter()
            val primaryButtonAction = FakeScreenPrimaryButtonAction()
            val viewModel = viewModelStoreRule.track(
                InputAddressViewModel(
                    args = AddressElementActivityContract.Args.Standalone(
                        apiConfiguration = DEFAULT_API_CONFIG,
                        config = AddressLauncher.Configuration.Builder()
                            .address(ADDRESS)
                            .billingAddress(
                                PaymentSheet.BillingDetails(name = ADDRESS.name, address = ADDRESS.address)
                            )
                            .additionalFields(
                                AddressLauncher.AdditionalFieldsConfiguration(checkboxLabel = CHECKBOX_LABEL)
                            )
                            .buttonTitle(SAVE_LABEL)
                            .build(),
                    ),
                    navigator = navigator,
                    resultStateHolder = resultStateHolder,
                    eventReporter = eventReporter,
                    placesClient = null,
                    primaryButtonAction = primaryButtonAction,
                )
            )
            val factory = FakeScreenViewModelSubcomponentFactory(viewModel)

            composeRule.setContent {
                InputAddressScreen(
                    inputAddressViewModelSubcomponentFactoryProvider = Provider { factory },
                )
            }
            composeRule.waitForIdle()
            factory.createCalls.awaitItem()
            assertThat(getResultFlowCalls.awaitItem().key).isEqualTo(AddressElementNavigator.AutocompleteEvent.KEY)
            assertThat(eventReporter.shownCalls.awaitItem().country).isEqualTo("US")
            composeRule.onNodeWithText(NAME).assertExists()

            Scenario(resultStateHolder).block()

            primaryButtonAction.calls.ensureAllEventsConsumed()
            factory.createCalls.ensureAllEventsConsumed()
            eventReporter.validate()
        }
    }

    private inner class Scenario(val resultStateHolder: AddressElementResultStateHolder) {
        fun assertControlsEnabled() {
            composeRule.onNodeWithText(NAME).assertIsEnabled()
            composeRule.onNodeWithText(SAVE_LABEL).assertIsEnabled()
            composeRule.onNodeWithContentDescription("Close").assertIsEnabled()
            composeRule.onNodeWithText(CHECKBOX_LABEL).assertIsEnabled()
            composeRule.onNodeWithText("Use billing address for shipping").assertIsEnabled()
            composeRule.onAllNodes(hasSetTextAction()).assertAll(isEnabled())
        }

        fun assertControlsDisabled() {
            composeRule.onNodeWithText(NAME).assertIsNotEnabled()
            composeRule.onNodeWithText(SAVE_LABEL).assertIsNotEnabled()
            composeRule.onNodeWithContentDescription("Close").assertIsNotEnabled()
            composeRule.onNodeWithText(CHECKBOX_LABEL).assertIsNotEnabled()
            composeRule.onNodeWithText("Use billing address for shipping").assertIsNotEnabled()
            composeRule.onAllNodes(hasSetTextAction()).assertAll(isNotEnabled())
        }

        fun assertLoading() {
            composeRule.onNodeWithTag(PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG).assertExists()
        }

        fun assertNotLoading() {
            composeRule.onNodeWithTag(PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
        }
    }

    private companion object {
        const val SAVE_LABEL = "Save address"
        const val CHECKBOX_LABEL = "Save this address for future use"
        const val NAME = "Jenny Rosen"
        val ADDRESS = AddressDetails(
            name = NAME,
            address = PaymentSheet.Address(
                line1 = "510 Townsend St",
                city = "San Francisco",
                state = "CA",
                postalCode = "94103",
                country = "US",
            ),
        )
    }
}

internal class FakeScreenPrimaryButtonAction : AddressElementPrimaryButtonAction {
    val calls = Turbine<AddressDetails>()

    override suspend fun invoke(addressDetails: AddressDetails): Result<AddressElementActivityContract.Result> {
        calls.add(addressDetails)
        return Result.success(AddressElementActivityContract.Result.StandaloneSucceeded(addressDetails))
    }
}

internal class FakeScreenViewModelSubcomponentFactory(
    private val viewModel: InputAddressViewModel,
) : InputAddressViewModelSubcomponent.Factory {
    val createCalls = Turbine<Unit>()

    override fun create(): InputAddressViewModelSubcomponent {
        createCalls.add(Unit)
        return object : InputAddressViewModelSubcomponent {
            override val inputAddressViewModel = viewModel
        }
    }
}
