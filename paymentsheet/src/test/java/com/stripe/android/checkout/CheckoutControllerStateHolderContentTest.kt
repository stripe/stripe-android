package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentBuilder
import com.stripe.android.paymentelement.embedded.content.EmbeddedLinkHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncherHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.AnalyticEventCallbackRule
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.FakeSavedPaymentMethodRepository
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class, ExperimentalAnalyticEventCallbackApi::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutControllerStateHolderContentTest {

    @Test
    fun `embeddedContent emits content when a state is committed and null when cleared`() = runTest {
        val stateHolder = createStateHolder()

        stateHolder.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            stateHolder.state = committedState()
            assertThat(awaitItem()).isNotNull()
            stateHolder.state = null
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `walletButtonsContent emits content when a state is committed`() = runTest {
        val stateHolder = createStateHolder()

        stateHolder.walletButtonsContent.test {
            assertThat(awaitItem()).isNull()
            stateHolder.state = committedState()
            assertThat(awaitItem()).isNotNull()
        }
    }

    private fun createStateHolder(): CheckoutControllerStateHolder {
        // The builder's own selection holder is independent of the state holder here; content is
        // driven by the confirmation state the state holder derives from its committed state.
        val builderSavedStateHandle = SavedStateHandle()
        val builderSelectionHolder =
            com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder(builderSavedStateHandle)
        val contentBuilder = EmbeddedContentBuilder(
            eventReporter = FakeEventReporter(),
            workContext = Dispatchers.Unconfined,
            uiContext = Dispatchers.Unconfined,
            savedPaymentMethodRepository = FakeSavedPaymentMethodRepository(),
            selectionHolder = builderSelectionHolder,
            embeddedLinkHelper = object : EmbeddedLinkHelper {
                override val linkEmail: StateFlow<String?> = stateFlowOf(null)
            },
            embeddedWalletsHelper = { stateFlowOf(null) },
            customerStateHolder = DefaultCustomerStateHolder(
                savedStateHandle = builderSavedStateHandle,
                selection = builderSelectionHolder.selection,
                customerMetadata = stateFlowOf(PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA),
                paymentMethodMetadataFlow = stateFlowOf(null),
            ),
            embeddedFormHelperFactory = EmbeddedFormHelperFactory(
                linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                embeddedSelectionHolder = builderSelectionHolder,
                savedStateHandle = builderSavedStateHandle,
            ),
            confirmationHandler = FakeConfirmationHandler(),
            rowSelectionImmediateActionHandler = DefaultEmbeddedRowSelectionImmediateActionHandler(
                coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
                internalRowSelectionCallback = { null },
            ),
            errorReporter = FakeErrorReporter(),
            internalRowSelectionCallback = { null },
            linkPaymentLauncher = RecordingLinkPaymentLauncher.noOp(),
            analyticsCallbackProvider = { AnalyticEventCallbackRule() },
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
            sheetLauncherHolder = EmbeddedSheetLauncherHolder(),
        )
        return createTestCheckoutControllerStateHolder(
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            contentBuilder = Provider { contentBuilder },
        )
    }

    private fun committedState(
        paymentSelection: PaymentSelection? = null,
    ) = CheckoutControllerState(
        key = "test_key",
        configuration = CheckoutController.Configuration().build(),
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
        flagImages = null,
        collectedDetails = CheckoutCollectedDetails(),
        integrationLaunched = false,
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        paymentSelection = paymentSelection,
        temporarySelection = null,
        previousNewSelections = android.os.Bundle(),
    )
}
