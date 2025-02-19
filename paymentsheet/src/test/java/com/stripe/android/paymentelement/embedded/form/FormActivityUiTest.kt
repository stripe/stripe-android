package com.stripe.android.paymentelement.embedded.form

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.FormPage
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class FormActivityUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val formPage = FormPage(composeRule)

    @Test
    fun `primary button enables when form is complete`() = runScenario {
        composeRule.onNode(hasText("Pay $10.99")).assertIsNotEnabled()
        formPage.fillOutCardDetails()
        composeRule.onNode(hasText("Pay $10.99")).assertIsEnabled()
    }

    private fun runScenario(
        block: () -> Unit
    ) {
        val savedStateHandle = SavedStateHandle()
        val embeddedSelectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val embeddedFormHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = embeddedSelectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory
        )
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val testScope = TestScope(UnconfinedTestDispatcher())
        val stateHelper = DefaultFormActivityStateHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = embeddedSelectionHolder,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            onClickDelegate = OnClickDelegateOverrideImpl()
        )
        val eventReporter = FakeEventReporter()
        val interactor = EmbeddedFormInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            paymentMethodCode = "card",
            hasSavedPaymentMethods = false,
            embeddedSelectionHolder = embeddedSelectionHolder,
            embeddedFormHelperFactory = embeddedFormHelperFactory,
            viewModelScope = testScope,
            formActivityStateHelper = stateHelper,
            eventReporter = eventReporter
        ).create()

        composeRule.setContent {
            val state by stateHelper.state.collectAsState()
            FormActivityUI(
                interactor = interactor,
                eventReporter = eventReporter,
                onDismissed = {},
                onClick = {},
                onProcessingCompleted = {},
                state = state,
            )
        }

        block()
    }
}
