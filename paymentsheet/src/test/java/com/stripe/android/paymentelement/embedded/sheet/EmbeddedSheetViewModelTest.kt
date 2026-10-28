package com.stripe.android.paymentelement.embedded.sheet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedSheetViewModelTest {
    @Test
    fun `first creation seeds all holders from launch arguments`() = runScenario {
        assertThat(customerStateHolder.customer.value).isEqualTo(args.customerState)
        assertThat(linkAccountHolder.linkAccountInfo.value).isEqualTo(args.linkAccountInfo)
        assertThat(selectionHolder.selection.value).isEqualTo(args.selection)
        assertThat(selectionHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(selectionHolder.getPreviousNewSelection("card"))
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `recreation preserves updated holder state instead of launch arguments`() = runScenario {
        val updatedSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION.copy(
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
        )
        selectionHolder.setSelection(updatedSelection)
        selectionHolder.previousNewSelections.remove("cashapp")
        val updatedCustomer = requireNotNull(args.customerState).copy(paymentMethods = emptyList())
        val updatedLink = LinkAccountUpdate.Value(null, null)
        customerStateHolder.setCustomerState(updatedCustomer)
        linkAccountHolder.set(updatedLink)

        recreate()

        selectionHolder.selection.test {
            assertThat(awaitItem()).isEqualTo(updatedSelection)
            expectNoEvents()
        }
        assertThat(selectionHolder.getPreviousNewSelection("card")).isEqualTo(updatedSelection)
        assertThat(selectionHolder.getPreviousNewSelection("cashapp")).isNull()
        assertThat(customerStateHolder.customer.value).isEqualTo(updatedCustomer)
        assertThat(linkAccountHolder.linkAccountInfo.value).isEqualTo(updatedLink)
    }

    @Test
    fun `recreation preserves cleared selection and customer state`() = runScenario {
        selectionHolder.setSelection(null)
        customerStateHolder.setCustomerState(null)

        recreate()

        assertThat(selectionHolder.selection.value).isNull()
        assertThat(customerStateHolder.customer.value).isNull()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val scenario = Scenario()
        try {
            scenario.block()
        } finally {
            scenario.close()
        }
    }

    private class Scenario {
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            productUsage = setOf("PaymentSheet.FlowController"),
            paymentElementCallbackIdentifier = "EmbeddedSheetViewModelTest",
            statusBarColor = null,
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            previousNewSelections = Bundle().apply {
                putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            },
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            ),
            linkAccountInfo = LinkAccountUpdate.Value(
                account = null,
                lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.LoggedOut,
            ),
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
        )
        private var controller = createController(null)
        private var savedStateHandle = createViewModel()

        val selectionHolder: DefaultEmbeddedSelectionHolder
            get() = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val linkAccountHolder: LinkAccountHolder
            get() = LinkAccountHolder(savedStateHandle)
        val customerStateHolder: DefaultCustomerStateHolder
            get() = DefaultCustomerStateHolder(
                customerMetadata = MutableStateFlow(args.paymentMethodMetadata.customerMetadata),
                paymentMethodMetadataFlow = MutableStateFlow(args.paymentMethodMetadata),
                savedStateHandle = savedStateHandle,
                selection = selectionHolder.selection,
            )

        fun recreate() {
            val savedState = Bundle()
            controller.saveInstanceState(savedState)
            close()
            controller = createController(savedState)
            savedStateHandle = createViewModel()
        }

        fun close() {
            controller.pause().stop().destroy()
            controller.get().viewModelStore.clear()
        }

        private fun createController(savedState: Bundle?): ActivityController<ComponentActivity> {
            return Robolectric.buildActivity(ComponentActivity::class.java)
                .create(savedState).start().resume()
        }

        private fun createViewModel(): SavedStateHandle {
            val activity = controller.get()
            val extras = MutableCreationExtras(activity.defaultViewModelCreationExtras).apply {
                set(ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY, "EmbeddedSheetViewModel")
            }
            val viewModel = EmbeddedSheetViewModel.Factory { args }
                .create(EmbeddedSheetViewModel::class.java, extras)
            activity.viewModelStore.put("EmbeddedSheetViewModel", viewModel)
            return extras.createSavedStateHandle()
        }
    }
}
