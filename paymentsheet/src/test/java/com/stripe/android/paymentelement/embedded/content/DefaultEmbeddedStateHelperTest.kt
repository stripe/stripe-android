@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class DefaultEmbeddedStateHelperTest {
    @Test
    fun `setting state correctly sets row style`() = testScenario {
        setState {
            appearance(
                PaymentSheet.Appearance(
                    embeddedAppearance = Embedded(
                        Embedded.RowStyle.FlatWithRadio.default
                    )
                )
            )
        }

        assertThat(embeddedContentHelper.dataLoadedTurbine.awaitItem().rowStyle)
            .isEqualTo(Embedded.RowStyle.FlatWithRadio.default)
    }

    @Test
    fun `setting state correctly parses appearance`() = testScenario {
        assertThat(StripeTheme.colorsLightMutable.componentBorder)
            .isEqualTo(
                StripeThemeDefaults.colorsLight.componentBorder
            )

        setState {
            appearance(
                PaymentSheet.Appearance(
                    colorsLight = PaymentSheetAppearance.CrazyAppearance.appearance.colorsLight,
                )
            )
        }

        assertThat(StripeTheme.colorsLightMutable.componentBorder)
            .isEqualTo(
                Color(
                    PaymentSheetAppearance.CrazyAppearance.appearance.colorsLight.componentBorder
                )
            )

        // Reset appearance
        PaymentSheet.Appearance().parseAppearance()
        embeddedContentHelper.dataLoadedTurbine.awaitItem()
    }

    @Test
    fun `setting state correctly sets state holders and null clears all state holders`() = testScenario {
        assertThat(confirmationStateHolder.state).isNull()
        assertThat(customerStateHolder.customer.value).isNull()
        assertThat(selectionHolder.selection.value).isNull()

        setState(
            selection = PaymentSelection.GooglePay,
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        )

        assertThat(stateHelper.state).isNotNull()
        assertThat(confirmationStateHolder.state).isNotNull()
        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.GooglePay)
        assertThat(embeddedContentHelper.dataLoadedTurbine.awaitItem()).isNotNull()

        stateHelper.state = null

        assertThat(stateHelper.state).isNull()
        assertThat(confirmationStateHolder.state).isNull()
        assertThat(customerStateHolder.customer.value).isNull()
        assertThat(selectionHolder.selection.value).isNull()
        assertThat(embeddedContentHelper.clearEmbeddedContentTurbine.awaitItem()).isEqualTo(Unit)
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val customerStateHolder = CustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA),
        )
        val confirmationStateHolder = EmbeddedConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )
        val embeddedContentHelper = FakeEmbeddedContentHelper()
        val stateHelper = DefaultEmbeddedStateHelper(
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            confirmationStateHolder = confirmationStateHolder,
            embeddedContentHelper = embeddedContentHelper,
        )

        Scenario(
            confirmationStateHolder = confirmationStateHolder,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            embeddedContentHelper = embeddedContentHelper,
            stateHelper = stateHelper,
        ).block()

        embeddedContentHelper.validate()
    }

    private class Scenario(
        val confirmationStateHolder: EmbeddedConfirmationStateHolder,
        val customerStateHolder: CustomerStateHolder,
        val selectionHolder: EmbeddedSelectionHolder,
        val embeddedContentHelper: FakeEmbeddedContentHelper,
        val stateHelper: EmbeddedStateHelper,
    ) {
        fun setState(
            paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            selection: PaymentSelection? = selectionHolder.selection.value,
            customer: CustomerState? = customerStateHolder.customer.value,
            configurationBuilder: EmbeddedPaymentElement.Configuration.Builder.() ->
            EmbeddedPaymentElement.Configuration.Builder = { this },
        ) {
            val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc")
                .configurationBuilder()
                .build()
            stateHelper.state = EmbeddedPaymentElement.State(
                confirmationState = EmbeddedConfirmationStateHolder.State(
                    paymentMethodMetadata = paymentMethodMetadata,
                    selection = selection,
                    initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                        intentConfiguration = PaymentSheet.IntentConfiguration(
                            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                                amount = 5050,
                                currency = "USD"
                            )
                        ),
                    ),
                    configuration = configuration,
                ),
                customer = customer,
            )
        }
    }
}
