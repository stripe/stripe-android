package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.ImmediateVerticalPaymentSelectionHandler
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class DefaultFormDefinitionFactoryTest {
    @Test
    fun `classification constructs elements once per code`() = runScenario {
        repeat(3) {
            assertThat(factory.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.Empty)
            assertThat(factory.formTypeForCode("card")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
        }
        assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp")
        assertThat(selectionProvider.calls.awaitItem()).isEqualTo("card")
    }

    @Test
    fun `real form seeds classification cache`() = runScenario {
        val elements = factory.formElementsForCode("card")
        assertThat(elements).isNotEmpty()
        assertThat(selectionProvider.calls.awaitItem()).isEqualTo("card")

        repeat(3) {
            assertThat(factory.formTypeForCode("card")).isEqualTo(formTypeForCode("card", elements))
        }
        selectionProvider.calls.expectNoEvents()
    }

    @Test
    fun `real forms use fresh controllers and latest selection after classification`() = runScenario(
        metadata = metadataWithEmail(),
    ) {
        assertThat(factory.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
        selectionProvider.calls.awaitItem()
        selectionProvider.selection = cashAppSelection("first@example.com")
        val first = factory.formElementsForCode("cashapp")
        assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp")
        assertThat(first.email()).isEqualTo("first@example.com")

        selectionProvider.selection = cashAppSelection("second@example.com")
        val second = factory.formElementsForCode("cashapp")
        assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp")
        assertThat(second.first()).isNotSameInstanceAs(first.first())
        val firstController = (first.first() as SectionElement).fields.first().sectionFieldErrorController()
        val secondController = (second.first() as SectionElement).fields.first().sectionFieldErrorController()
        assertThat(secondController).isNotSameInstanceAs(firstController)
        assertThat(second.email()).isEqualTo("second@example.com")
        assertThat(first.email()).isEqualTo("first@example.com")
        assertThat(factory.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
        selectionProvider.calls.expectNoEvents()
    }

    @Test
    fun `factories independently classify billing requirements`() = runScenario {
        val requiresEmail = createFactory(metadataWithEmail())
        assertThat(factory.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.Empty)
        assertThat(requiresEmail.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
        assertThat(factory.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.Empty)
        repeat(2) { assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp") }
    }

    @Test
    fun `factories independently retain mandate text`() = runScenario(
        metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(paymentMethodTypes = listOf("cashapp")),
        ),
    ) {
        val other = createFactory(metadata.copy(merchantName = "Another merchant"))
        val first = factory.formTypeForCode("cashapp") as FormHelper.FormType.MandateOnly
        val second = other.formTypeForCode("cashapp") as FormHelper.FormType.MandateOnly
        assertThat(first.mandate).isNotEqualTo(second.mandate)
        assertThat(factory.formTypeForCode("cashapp")).isEqualTo(first)
        assertThat(other.formTypeForCode("cashapp")).isEqualTo(second)
        repeat(2) { assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp") }
    }

    @Test
    fun `concurrent cold classifications share construction`() = runScenario {
        val gate = ConstructionGate()
        selectionProvider.gate = gate
        val first = async(Dispatchers.Default) { factory.formTypeForCode("cashapp") }
        withContext(Dispatchers.IO) { assertThat(gate.entered.await(5, TimeUnit.SECONDS)).isTrue() }
        val secondStarted = CountDownLatch(1)
        val second = async(Dispatchers.Default) {
            secondStarted.countDown()
            factory.formTypeForCode("cashapp")
        }
        try {
            withContext(Dispatchers.IO) { assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue() }
            assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp")
            selectionProvider.calls.expectNoEvents()
        } finally {
            gate.release.countDown()
        }
        assertThat(first.await()).isEqualTo(FormHelper.FormType.Empty)
        assertThat(second.await()).isEqualTo(FormHelper.FormType.Empty)
        selectionProvider.calls.expectNoEvents()
    }

    @Test
    fun `failed classification can be retried`() = runScenario {
        selectionProvider.failure = IllegalStateException("Construction failed")
        assertFailsWith<IllegalStateException> { factory.formTypeForCode("cashapp") }
        assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp")
        selectionProvider.failure = null
        assertThat(factory.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.Empty)
        assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp")
        assertThat(factory.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.Empty)
        selectionProvider.calls.expectNoEvents()
    }

    @Test
    fun `vertical state reads and form updates reuse classification and report completion once`() = runScenario(
        metadata = metadataWithEmail(),
    ) {
        val eventReporter = FakeEventReporter()
        val selections = Turbine<PaymentSelection?>()
        val selection = MutableStateFlow<PaymentSelection?>(cashAppSelection("initial@example.com").paymentSelection)
        val processing = MutableStateFlow(false)
        val helper = DefaultFormHelper(
            coroutineScope = backgroundScope,
            linkInlineHandler = LinkInlineHandler.create(),
            paymentMethodMetadata = metadata,
            selectionUpdater = {
                selection.value = it
                selections.add(it)
            },
            eventReporter = eventReporter,
            savedStateHandle = SavedStateHandle(),
            formDefinitionFactory = factory,
        )
        val interactor = createVerticalInteractor(selection, processing, helper)
        repeat(3) {
            val state = interactor.state.value
            val selected = state.selection as PaymentMethodVerticalLayoutInteractor.Selection.New
            assertThat(selected.code).isEqualTo("cashapp")
            assertThat(selected.canBeChanged).isTrue()
            assertThat(state.isProcessing).isFalse()
        }
        assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp")
        processing.value = true
        assertThat(interactor.state.value.isProcessing).isTrue()
        selectionProvider.calls.expectNoEvents()
        listOf("first@example.com", "second@example.com").forEach { email ->
            helper.onFormFieldValuesChanged(
                FormFieldValues(
                    fieldValuePairs = mapOf(FormFieldId.Email to FormFieldEntry(email, true)),
                    userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
                ),
                "cashapp",
            )
            val selection = selections.awaitItem() as PaymentSelection.New.GenericPaymentMethod
            assertThat(selection.paymentMethodCreateParams.billingDetails?.email).isEqualTo(email)
            repeat(3) {
                val selected = interactor.state.value.selection as PaymentMethodVerticalLayoutInteractor.Selection.New
                assertThat(selected.code).isEqualTo("cashapp")
                assertThat(selected.canBeChanged).isTrue()
            }
        }
        assertThat(eventReporter.formCompletedCalls.awaitItem().code).isEqualTo("cashapp")
        selectionProvider.calls.expectNoEvents()
        selections.ensureAllEventsConsumed()
        eventReporter.validate()
        interactor.close()
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = defaultMetadata(),
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val selectionProvider = FakeFormSelectionProvider()
        Scenario(this, metadata, selectionProvider).apply { block() }
        selectionProvider.calls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val scope: TestScope,
        val metadata: PaymentMethodMetadata,
        val selectionProvider: FakeFormSelectionProvider,
    ) : CoroutineScope by scope {
        val backgroundScope = scope.backgroundScope
        val factory = createFactory(metadata)

        fun createVerticalInteractor(
            selection: MutableStateFlow<PaymentSelection?>,
            processing: MutableStateFlow<Boolean>,
            helper: FormHelper,
        ) = DefaultPaymentMethodVerticalLayoutInteractor(
            paymentMethodMetadata = metadata,
            processing = processing,
            savedPaymentMethodSelectionState = MutableStateFlow(SavedPaymentMethodSelectionState.Idle),
            temporarySelection = MutableStateFlow(null),
            selection = selection,
            paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(null),
            formTypeForCode = helper::formTypeForCode,
            onFormFieldValuesChanged = helper::onFormFieldValuesChanged,
            transitionToManageScreen = {},
            transitionToFormScreen = {},
            paymentMethods = MutableStateFlow(emptyList()),
            mostRecentlySelectedSavedPaymentMethod = MutableStateFlow(null),
            canRemove = MutableStateFlow(true),
            walletsState = MutableStateFlow(null),
            canUpdateCardExpiryAndBillingDetails = MutableStateFlow(false),
            canChangeCbc = MutableStateFlow(false),
            updateSelection = { selection.value = it },
            verticalPaymentSelectionHandler = ImmediateVerticalPaymentSelectionHandler(
                updateSelection = { value, _ -> selection.value = value },
                completionAction = null,
            ),
            isCurrentScreen = MutableStateFlow(false),
            reportPaymentMethodTypeSelected = {},
            reportFormShown = {},
            onUpdatePaymentMethod = {},
            shouldUpdateVerticalModeSelection = { true },
            displaysMandatesInFormScreen = false,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { _, _ -> },
            updateMandateText = null,
            linkAccount = MutableStateFlow(LinkAccountUpdate.Value(null)),
            coroutineScope = backgroundScope,
            mainDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
            paymentMethodMessagePromotionsHelper = null,
        )

        fun createFactory(metadata: PaymentMethodMetadata) = DefaultFormDefinitionFactory(
            coroutineScope = backgroundScope,
            linkInlineHandler = LinkInlineHandler.create(),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            paymentMethodMetadata = metadata,
            newPaymentSelectionProvider = selectionProvider,
            linkConfigurationCoordinator = null,
            setAsDefaultMatchesSaveForFutureUse = false,
            autocompleteAddressInteractorFactory = null,
            isLinkUI = false,
            automaticallyLaunchedCardScanFormDataHelper = null,
            tapToAddHelper = null,
            paymentMethodMessagePromotionsHelper = null,
            isNfcScanningAvailable = null,
        )
    }

    private fun List<FormElement>.email() = flatMap { it.getFormFieldValueFlow().value }
        .first { it.first == FormFieldId.Email }.second.value

    private fun cashAppSelection(email: String) = NewPaymentOptionSelection.New(
        PaymentSelection.New.GenericPaymentMethod(
            label = "Cash App".resolvableString,
            iconResource = 0,
            iconResourceNight = null,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(
                billingDetails = PaymentMethod.BillingDetails(email = email),
            ),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            paymentMethodOptionsParams = null,
            paymentMethodExtraParams = null,
        ),
    )

    private fun metadataWithEmail() = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("cashapp"),
        ),
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        ),
    )

    private fun defaultMetadata() = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("cashapp", "card"),
        ),
    )
}

internal class FakeFormSelectionProvider : (PaymentMethodCode) -> NewPaymentOptionSelection? {
    val calls = Turbine<PaymentMethodCode>()
    var selection: NewPaymentOptionSelection? = null
    var failure: IllegalStateException? = null
    var gate: ConstructionGate? = null

    override fun invoke(code: PaymentMethodCode): NewPaymentOptionSelection? {
        calls.add(code)
        gate?.let {
            it.entered.countDown()
            check(it.release.await(5, TimeUnit.SECONDS))
        }
        failure?.let { throw it }
        return selection
    }
}

internal class ConstructionGate {
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
}
