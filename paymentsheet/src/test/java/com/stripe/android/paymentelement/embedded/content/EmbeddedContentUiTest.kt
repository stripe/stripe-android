package com.stripe.android.paymentelement.embedded.content

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.FakeSavedPaymentMethodRepository
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@OptIn(WalletButtonsPreview::class)
internal class EmbeddedContentUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @get:Rule
    val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    @Test
    fun `rowStyle FlatWithDisclosure, dataLoaded emits embeddedContent event that passes validation`() =
        runScenario(internalRowSelectionCallback = {}) {
            embeddedContentHelper.embeddedContent.test {
                assertThat(awaitItem()).isNull()
                state.value = EmbeddedContentHelperStateHolder.State(
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                    appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default),
                    embeddedViewDisplaysMandateText = true,
                    configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
                )
                val content = awaitItem()
                assertThat(content).isNotNull()

                composeRule.setContent {
                    content?.Content()
                }

                composeRule.waitForIdle()
                composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertExists()
            }
        }

    @Test
    fun `rowStyle not FlatWithDisclosure, dataLoaded emits event that passes validation`() = runScenario(
        internalRowSelectionCallback = null
    ) {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            state.value = EmbeddedContentHelperStateHolder.State(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default),
                embeddedViewDisplaysMandateText = true,
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            )
            val content = awaitItem()
            assertThat(content).isNotNull()

            composeRule.setContent {
                content?.Content()
            }

            composeRule.waitForIdle()
            composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertExists()
        }
    }

    @Test
    fun `dataLoaded emits embeddedContent event that fails validation`() = runScenario(
        internalRowSelectionCallback = null
    ) {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            state.value = EmbeddedContentHelperStateHolder.State(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                appearance = Embedded(Embedded.RowStyle.FlatWithDisclosure.default),
                embeddedViewDisplaysMandateText = true,
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            )
            val content = awaitItem()
            assertThat(content).isNotNull()
            assertFailsWith<IllegalArgumentException>(
                message = "EmbeddedPaymentElement.Builder.rowSelectionBehavior() must be set to " +
                    "ImmediateAction when using FlatWithDisclosure RowStyle. " +
                    "Use a different style or enable ImmediateAction rowSelectionBehavior"
            ) {
                composeRule.setContent {
                    content?.Content()
                }
            }
        }
    }

    private class Scenario(
        val embeddedContentHelper: DefaultEmbeddedContentHelper,
        val state: MutableStateFlow<EmbeddedContentHelperStateHolder.State?>,
    )

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    @Suppress("LongMethod")
    private fun runScenario(
        internalRowSelectionCallback: InternalRowSelectionCallback? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val embeddedFormHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            embeddedSelectionHolder = selectionHolder,
            savedStateHandle = savedStateHandle,
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
        )
        val confirmationHandler = FakeConfirmationHandler()
        val eventReporter = FakeEventReporter()
        val errorReporter = FakeErrorReporter()
        val viewModelScope = coroutineScopeCleanupRule.track(CoroutineScope(Dispatchers.Unconfined))
        val immediateActionHandler =
            DefaultEmbeddedRowSelectionImmediateActionHandler(
                coroutineScope = coroutineScopeCleanupRule.track(CoroutineScope(UnconfinedTestDispatcher())),
                internalRowSelectionCallback = { null }
            )
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(
                PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA
            ),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val sheetStateHolder = SheetStateHolder(savedStateHandle)

        val state = MutableStateFlow<EmbeddedContentHelperStateHolder.State?>(null)
        val savedPaymentMethodMutatorFactory = EmbeddedContentSavedPaymentMethodMutatorFactory(
            eventReporter = eventReporter,
            workContext = Dispatchers.Unconfined,
            uiContext = Dispatchers.Unconfined,
            savedPaymentMethodRepository = FakeSavedPaymentMethodRepository(),
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            linkAccountHolder = linkAccountHolder,
            coroutineScope = viewModelScope,
            sheetStateHolder = sheetStateHolder,
        )
        val verticalLayoutInteractorFactory = DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactory(
            eventReporter = eventReporter,
            embeddedFormHelperFactory = embeddedFormHelperFactory,
            confirmationHandler = confirmationHandler,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
            rowSelectionImmediateActionHandler = immediateActionHandler,
            coroutineScope = viewModelScope,
            sheetStateHolder = sheetStateHolder,
            savedPaymentMethodMutatorFactory = savedPaymentMethodMutatorFactory,
        )

        val embeddedContentHelper =
            DefaultEmbeddedContentHelper(
                coroutineScope = viewModelScope,
                state = state,
                verticalLayoutInteractorFactory = verticalLayoutInteractorFactory,
                sheetStateHolder = sheetStateHolder,
                embeddedWalletsHelper = { stateFlowOf(null) },
                internalRowSelectionCallback = { internalRowSelectionCallback },
                customerStateHolder = customerStateHolder,
                selectionHolder = selectionHolder,
                errorReporter = errorReporter,
            )
        Scenario(
            embeddedContentHelper = embeddedContentHelper,
            state = state,
        ).block()
    }
}
