package com.stripe.android.paymentelement.embedded.form

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

internal class FormActivityScreenShotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        PaymentSheetAppearance.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    @Test
    fun testFormActivity_enabled() {
        paparazziRule.snapshot {
            TestFormActivityUi()
        }
    }

    @Test
    fun testFormActivity_disabled() {
        paparazziRule.snapshot {
            TestFormActivityUi(
                isEnabled = false
            )
        }
    }

    @Test
    fun testFormActivity_processing() {
        paparazziRule.snapshot {
            TestFormActivityUi(
                isEnabled = false,
                isProcessing = true,
                processingState = PrimaryButtonProcessingState.Processing
            )
        }
    }

    @Test
    fun testFormActivity_complete() {
        paparazziRule.snapshot {
            TestFormActivityUi(
                isEnabled = false,
                processingState = PrimaryButtonProcessingState.Completed
            )
        }
    }

    @Test
    fun testFormActivity_error() {
        paparazziRule.snapshot {
            TestFormActivityUi(
                isEnabled = true,
                processingState = PrimaryButtonProcessingState.Idle(null),
                error = "Something went wrong"
            )
        }
    }

    @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
    @Composable
    private fun TestFormActivityUi(
        isEnabled: Boolean = true,
        processingState: PrimaryButtonProcessingState = PrimaryButtonProcessingState.Idle(null),
        isProcessing: Boolean = false,
        error: String? = null
    ) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())
        val stateHolder = DefaultFormActivityStateHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration,
            coroutineScope = TestScope(UnconfinedTestDispatcher())
        )
        val formHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = selectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory
        )
        val interactor = EmbeddedFormInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            paymentMethodCode = "card",
            hasSavedPaymentMethods = false,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = formHelperFactory,
            viewModelScope = TestScope(UnconfinedTestDispatcher()),
            formActivityStateHelper = stateHolder,
        ).create()

        val state by stateHolder.state.collectAsState()

        ViewModelStoreOwnerContext {
            FormActivityUI(
                interactor = interactor,
                eventReporter = FakeEventReporter(),
                onClick = {},
                onProcessingCompleted = {},
                state = state.copy(
                    isEnabled = isEnabled,
                    isProcessing = isProcessing,
                    error = error?.resolvableString,
                    processingState = processingState
                ),
                onDismissed = {}
            )
        }
    }
}
