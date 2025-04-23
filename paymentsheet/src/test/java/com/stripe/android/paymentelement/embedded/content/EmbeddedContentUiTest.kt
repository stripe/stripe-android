package com.stripe.android.paymentelement.embedded.content

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedContentUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun `rowStyle FlatWithChevron, dataLoaded emits embeddedContent event that passes validation`() =
        runScenario(internalRowSelectionCallback = {}) {
            embeddedContentHelper.embeddedContent.test {
                assertThat(awaitItem()).isNull()
                embeddedContentHelper.dataLoaded(
                    PaymentMethodMetadataFactory.create(),
                    Embedded.RowStyle.FlatWithRadio.default,
                    embeddedViewDisplaysMandateText = true,
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
    fun `rowStyle not FlatWithChevron, dataLoaded emits embeddedContent event that passes validation`() = runScenario(
        internalRowSelectionCallback = null
    ) {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            embeddedContentHelper.dataLoaded(
                PaymentMethodMetadataFactory.create(),
                Embedded.RowStyle.FlatWithRadio.default,
                embeddedViewDisplaysMandateText = true,
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
            embeddedContentHelper.dataLoaded(
                PaymentMethodMetadataFactory.create(),
                Embedded.RowStyle.FlatWithChevron.default,
                embeddedViewDisplaysMandateText = true,
            )
            val content = awaitItem()
            assertThat(content).isNotNull()
            assertFailsWith<IllegalArgumentException>(
                message = "EmbeddedPaymentElement.Builder.rowSelectionBehavior() must be set to " +
                    "ImmediateAction when using FlatWithChevron RowStyle. " +
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
    )

    private fun runScenario(
        internalRowSelectionCallback: InternalRowSelectionCallback? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val embeddedFormHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            embeddedSelectionHolder = selectionHolder,
            savedStateHandle = savedStateHandle,
        )
        val confirmationHandler = FakeConfirmationHandler()
        val eventReporter = FakeEventReporter()
        val errorReporter = FakeErrorReporter()
        val immediateActionHandler =
            DefaultEmbeddedRowSelectionImmediateActionHandler(
                coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
                internalRowSelectionCallback = { null }
            )

        val embeddedContentHelper =
            DefaultEmbeddedContentHelper(
                coroutineScope = CoroutineScope(Dispatchers.Unconfined),
                savedStateHandle = savedStateHandle,
                eventReporter = eventReporter,
                workContext = Dispatchers.Unconfined,
                uiContext = Dispatchers.Unconfined,
                customerRepository = FakeCustomerRepository(),
                selectionHolder = selectionHolder,
                embeddedLinkHelper = object : EmbeddedLinkHelper {
                    override val linkEmail: StateFlow<String?> = stateFlowOf(null)
                },
                embeddedWalletsHelper = { stateFlowOf(null) },
                customerStateHolder = CustomerStateHolder(
                    savedStateHandle = savedStateHandle,
                    selection = selectionHolder.selection,
                    customerMetadataPermissions = stateFlowOf(
                        PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA.permissions
                    ),
                ),
                embeddedFormHelperFactory = embeddedFormHelperFactory,
                confirmationHandler = confirmationHandler,
                confirmationStateHolder = EmbeddedConfirmationStateHolder(
                    savedStateHandle = savedStateHandle,
                    selectionHolder = selectionHolder,
                    coroutineScope = CoroutineScope(Dispatchers.Unconfined),
                ),
                rowSelectionImmediateActionHandler = immediateActionHandler,
                errorReporter = errorReporter,
                internalRowSelectionCallback = { internalRowSelectionCallback }
            )
        Scenario(
            embeddedContentHelper = embeddedContentHelper,
        ).block()
    }
}
