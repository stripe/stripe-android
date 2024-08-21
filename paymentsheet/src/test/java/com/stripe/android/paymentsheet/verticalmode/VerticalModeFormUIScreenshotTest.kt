package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

internal class VerticalModeFormUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @Test
    fun cardFormIsDisplayed() {
        paparazziRule.snapshot {
            val metadata = PaymentMethodMetadataFactory.create()
            CreateTestScenario(paymentMethodCode = "card", metadata = metadata)
        }
    }

    @Test
    fun cardFieldsAreDisabledWhenProcessing() {
        paparazziRule.snapshot {
            val metadata = PaymentMethodMetadataFactory.create()
            CreateTestScenario(paymentMethodCode = "card", metadata = metadata, isProcessing = true)
        }
    }

    @Test
    fun cashappShowsBillingFields() {
        paparazziRule.snapshot {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                )
            )
            CreateTestScenario(paymentMethodCode = "cashapp", metadata = metadata)
        }
    }

    @Composable
    private fun CreateTestScenario(
        paymentMethodCode: PaymentMethodCode,
        metadata: PaymentMethodMetadata,
        isProcessing: Boolean = false,
    ) {
        val formArguments = FormArgumentsFactory.create(
            paymentMethodCode = paymentMethodCode,
            metadata = metadata,
        )
        val uiDefinitionArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            linkConfigurationCoordinator = null,
            onLinkInlineSignupStateChanged = { throw AssertionError("Not expected") },
        )

        CompositionLocalProvider(
            LocalViewModelStoreOwner provides object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = ViewModelStore()
            }
        ) {
            VerticalModeFormUI(
                interactor = ScreenshotVerticalModeFormInteractor(
                    initialState = VerticalModeFormInteractor.State(
                        selectedPaymentMethodCode = paymentMethodCode,
                        isProcessing = isProcessing,
                        usBankAccountFormArguments = mock(),
                        formArguments = formArguments,
                        formElements = metadata.formElementsForCode(
                            code = paymentMethodCode,
                            uiDefinitionFactoryArgumentsFactory = uiDefinitionArgumentsFactory,
                        )!!,
                        headerInformation = metadata.formHeaderInformationForCode(
                            code = paymentMethodCode,
                            customerHasSavedPaymentMethods = false,
                        ),
                    ),
                ),
            )
        }
    }

    private class ScreenshotVerticalModeFormInteractor(
        initialState: VerticalModeFormInteractor.State,
        override val isLiveMode: Boolean = true,
        private val canGoBack: Boolean = true,
    ) : VerticalModeFormInteractor {
        override val state: StateFlow<VerticalModeFormInteractor.State> = stateFlowOf(initialState)

        override fun handleViewAction(viewAction: VerticalModeFormInteractor.ViewAction) {
            throw AssertionError("Not expected")
        }

        override fun canGoBack(): Boolean {
            return canGoBack
        }

        override fun close() {
            throw AssertionError("Not expected")
        }
    }
}
