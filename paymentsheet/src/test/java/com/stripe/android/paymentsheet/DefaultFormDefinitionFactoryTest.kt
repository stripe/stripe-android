package com.stripe.android.paymentsheet

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultFormDefinitionFactoryTest {
    @Test
    fun `classification constructs elements once per code`() = runScenario {
        repeat(3) {
            assertThat(factory.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.Empty)
            assertThat(factory.formTypeForCode("card")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
        }
        assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp")
        selectionProvider.calls.expectNoEvents()
    }

    @Test
    fun `real form seeds classification cache`() = runScenario(
        metadata = metadataWithEmail(),
    ) {
        val elements = factory.formElementsForCode("cashapp")
        assertThat(elements).isNotEmpty()
        assertThat(selectionProvider.calls.awaitItem()).isEqualTo("cashapp")

        repeat(3) {
            assertThat(factory.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
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

    private fun runScenario(
        metadata: PaymentMethodMetadata = defaultMetadata(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionProvider = FakeFormSelectionProvider()
        Scenario(backgroundScope, metadata, selectionProvider).apply { block() }
        selectionProvider.calls.ensureAllEventsConsumed()
    }

    private class Scenario(
        private val coroutineScope: CoroutineScope,
        val metadata: PaymentMethodMetadata,
        val selectionProvider: FakeFormSelectionProvider,
    ) {
        val factory = createFactory(metadata)

        fun createFactory(metadata: PaymentMethodMetadata) = DefaultFormDefinitionFactory(
            coroutineScope = coroutineScope,
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

    override fun invoke(code: PaymentMethodCode): NewPaymentOptionSelection? {
        calls.add(code)
        return selection
    }
}
