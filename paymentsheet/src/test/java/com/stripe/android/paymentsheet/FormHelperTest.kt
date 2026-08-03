package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.isInstanceOf
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParams.Companion.getNameFromParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.ServerDrivenFormRenderException
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BacsDebitBankAccountSpec
import com.stripe.android.ui.core.elements.BacsDebitConfirmSpec
import com.stripe.android.ui.core.elements.BlikElement
import com.stripe.android.ui.core.elements.BoletoTaxIdSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionElement
import com.stripe.android.ui.core.elements.KonbiniConfirmationNumberSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PaymentMethodMessageHeaderElement
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.CheckboxFieldElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith
import com.stripe.android.paymentsheet.forms.generated.FormElementSpecV1 as FormElementSpec
import com.stripe.android.paymentsheet.forms.generated.PaymentMethodFormSpecV1 as PaymentMethodFormSpec

@RunWith(RobolectricTestRunner::class)
internal class FormHelperTest {

    @get:Rule
    val enableKlarnaFormRemovalRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableKlarnaFormRemoval,
        isEnabled = false
    )

    @get:Rule
    val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    @Test
    fun `formElementsForCode with unknown code returns empty list`() = runTest {
        val formHelper = createFormHelper(
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.formElementsForCode("blah")).isEmpty()
    }

    @Test
    fun `server driven form rendering reports success`() = runScenario {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                serverDrivenFormSpecs = listOf(
                    PaymentMethodFormSpec(
                        type = "server_selected_type",
                        fields = listOf(
                            FormElementSpec(
                                type = "native_component",
                                component = "link_card_collection",
                            )
                        ),
                    )
                ),
            ),
            eventReporter = eventReporter,
            newPaymentSelectionProvider = { null },
        )

        assertThat(formHelper.formElementsForCode("server_selected_type")).isEmpty()
        val event = eventReporter.analyticsEventCalls.awaitItem()
        assertThat(event).isInstanceOf(PaymentSheetEvent.MobileSessionFormRender::class.java)
        assertThat((event as PaymentSheetEvent.MobileSessionFormRender).params)
            .containsEntry("mobile_session_render_outcome", "success")
    }

    @Test
    fun `server driven form rendering trusts Mint billing fields`() = runTest {
        val paymentMethodType = "server_selected_type"
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                ),
                sharedDataSpecs = listOf(
                    SharedDataSpec(
                        type = paymentMethodType,
                        fields = arrayListOf(NameSpec()),
                    )
                ),
                serverDrivenFormSpecs = listOf(
                    PaymentMethodFormSpec(
                        type = paymentMethodType,
                        fields = listOf(FormElementSpec(type = "name")),
                    )
                ),
            ),
            newPaymentSelectionProvider = { null },
        )

        assertThat(formHelper.formElementsForCode(paymentMethodType)).hasSize(1)
    }

    @Test
    fun `server driven BLIK component renders without a payment method definition lookup`() = runTest {
        val paymentMethodType = "server_selected_type"
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                serverDrivenFormSpecs = listOf(
                    PaymentMethodFormSpec(
                        type = paymentMethodType,
                        fields = listOf(
                            FormElementSpec(
                                type = "native_component",
                                component = "blik_confirmation",
                            )
                        ),
                    )
                ),
            ),
            newPaymentSelectionProvider = { null },
        )

        val section = formHelper.formElementsForCode(paymentMethodType).single()
        assertThat(section).isInstanceOf<SectionElement>()
        assertThat((section as SectionElement).fields.single())
            .isInstanceOf<BlikElement>()
    }

    @Test
    fun `server driven renderer preserves mixed native and declarative field order`() = runTest {
        val paymentMethodType = "server_selected_type"
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                sharedDataSpecs = listOf(
                    SharedDataSpec(
                        type = paymentMethodType,
                        fields = arrayListOf(NameSpec()),
                    )
                ),
                serverDrivenFormSpecs = listOf(
                    PaymentMethodFormSpec(
                        type = paymentMethodType,
                        fields = listOf(
                            FormElementSpec(type = "native_component", component = "card_details"),
                            FormElementSpec(type = "native_component", component = "card_billing_details"),
                            FormElementSpec(type = "name"),
                        ),
                    )
                ),
            ),
            newPaymentSelectionProvider = { null },
        )

        val elements = formHelper.formElementsForCode(paymentMethodType)

        assertThat(elements).hasSize(3)
        assertThat(elements[0]).isInstanceOf<CardDetailsSectionElement>()
        assertThat(elements[1]).isInstanceOf<SectionElement>()
        assertThat(elements[2]).isInstanceOf<SectionElement>()
    }

    @Test
    fun `server driven Bacs atoms render without a payment method definition lookup`() = runTest {
        val paymentMethodType = "server_selected_type"
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                sharedDataSpecs = listOf(
                    SharedDataSpec(
                        type = paymentMethodType,
                        fields = arrayListOf(
                            BacsDebitBankAccountSpec(),
                            BacsDebitConfirmSpec(),
                        ),
                    )
                ),
                serverDrivenFormSpecs = listOf(
                    PaymentMethodFormSpec(
                        type = paymentMethodType,
                        fields = listOf(
                            FormElementSpec(type = "bacs_debit_bank_account"),
                            FormElementSpec(type = "bacs_debit_mandate"),
                        ),
                    )
                ),
            ),
            newPaymentSelectionProvider = { null },
        )

        val elements = formHelper.formElementsForCode(paymentMethodType)
        assertThat(elements).hasSize(2)
        assertThat(elements[0]).isInstanceOf<SectionElement>()
        assertThat(elements[1]).isInstanceOf<CheckboxFieldElement>()
    }

    @Test
    fun `server driven Boleto atom renders without a payment method definition lookup`() = runTest {
        val paymentMethodType = "server_selected_type"
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                sharedDataSpecs = listOf(
                    SharedDataSpec(
                        type = paymentMethodType,
                        fields = arrayListOf(BoletoTaxIdSpec()),
                    )
                ),
                serverDrivenFormSpecs = listOf(
                    PaymentMethodFormSpec(
                        type = paymentMethodType,
                        fields = listOf(FormElementSpec(type = "boleto_tax_id")),
                    )
                ),
            ),
            newPaymentSelectionProvider = { null },
        )

        assertThat(formHelper.formElementsForCode(paymentMethodType).single()).isInstanceOf<SectionElement>()
    }

    @Test
    fun `server driven Konbini atom renders without a payment method definition lookup`() = runTest {
        val paymentMethodType = "server_selected_type"
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                sharedDataSpecs = listOf(
                    SharedDataSpec(
                        type = paymentMethodType,
                        fields = arrayListOf(KonbiniConfirmationNumberSpec()),
                    )
                ),
                serverDrivenFormSpecs = listOf(
                    PaymentMethodFormSpec(
                        type = paymentMethodType,
                        fields = listOf(FormElementSpec(type = "konbini_confirmation_number")),
                    )
                ),
            ),
            newPaymentSelectionProvider = { null },
        )

        assertThat(formHelper.formElementsForCode(paymentMethodType).single()).isInstanceOf<SectionElement>()
    }

    @Test
    fun `unknown server driven component reports failure and remains developer visible`() = runScenario {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                serverDrivenFormSpecs = listOf(
                    PaymentMethodFormSpec(
                        type = "server_selected_type",
                        fields = listOf(
                            FormElementSpec(
                                type = "native_component",
                                component = "future_component",
                            )
                        ),
                    )
                ),
            ),
            eventReporter = eventReporter,
            newPaymentSelectionProvider = { null },
        )

        val error = assertFailsWith<ServerDrivenFormRenderException> {
            formHelper.formElementsForCode("server_selected_type")
        }
        assertThat(error.errorCode)
            .isEqualTo(ServerDrivenFormRenderException.ErrorCode.UnsupportedNativeComponent)
        val event = eventReporter.analyticsEventCalls.awaitItem()
        assertThat((event as PaymentSheetEvent.MobileSessionFormRender).params)
            .containsExactly(
                "mobile_session_render_outcome", "failure",
                "mobile_session_render_error_code", "unsupported_native_component",
            )
    }

    @Test
    fun `formElementsForCode returns klarna form elements`() = runTest {
        enableKlarnaFormRemovalRule.setEnabled(true)
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                )
            ),
            newPaymentSelectionProvider = { null },
        )
        val formElements = formHelper.formElementsForCode("klarna")
        assertThat(formElements).hasSize(0)
        enableKlarnaFormRemovalRule.setEnabled(false)
    }

    @Test
    fun `formElementsForCode returns klarna form elements without using current selection values`() = runTest {
        enableKlarnaFormRemovalRule.setEnabled(true)

        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
            newPaymentSelectionProvider = {
                NewPaymentOptionSelection.New(
                    PaymentSelection.New.GenericPaymentMethod(
                        label = "Cash App".resolvableString,
                        iconResource = 0,
                        iconResourceNight = null,
                        lightThemeIconUrl = null,
                        darkThemeIconUrl = null,
                        paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(
                            billingDetails = PaymentMethod.BillingDetails(email = "example@email.com")
                        ),
                        customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                        paymentMethodOptionsParams = null,
                        paymentMethodExtraParams = null,
                    )
                )
            },
        )
        val formElements = formHelper.formElementsForCode("klarna")
        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].getFormFieldValueFlow().value[0].first.v1).isEqualTo("billing_details[email]")
        assertThat(formElements[0].getFormFieldValueFlow().value[0].second.value).isEqualTo("")

        enableKlarnaFormRemovalRule.setEnabled(false)
    }

    @Test
    fun `formElementsForCode returns klarna form elements using current selection values`() = runTest {
        enableKlarnaFormRemovalRule.setEnabled(true)
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
            newPaymentSelectionProvider = {
                NewPaymentOptionSelection.New(
                    PaymentSelection.New.GenericPaymentMethod(
                        label = "Klarna".resolvableString,
                        iconResource = 0,
                        iconResourceNight = null,
                        lightThemeIconUrl = null,
                        darkThemeIconUrl = null,
                        paymentMethodCreateParams = PaymentMethodCreateParams.createKlarna(
                            billingDetails = PaymentMethod.BillingDetails(email = "example@email.com")
                        ),
                        customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                        paymentMethodOptionsParams = null,
                        paymentMethodExtraParams = null,
                    )
                )
            },
        )
        val formElements = formHelper.formElementsForCode("klarna")
        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].getFormFieldValueFlow().value[0].first.v1).isEqualTo("billing_details[email]")
        assertThat(formElements[0].getFormFieldValueFlow().value[0].second.value).isEqualTo("example@email.com")
        enableKlarnaFormRemovalRule.setEnabled(false)
    }

    @Test
    fun `createFormArguments produces the correct form arguments when payment intent is off-session`() = runTest {
        val observedArgs = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                hasCustomerConfiguration = true,
                stripeIntent = PaymentIntentFixtures.PI_OFF_SESSION
            )
        ).createFormArguments(
            paymentMethodCode = LpmRepositoryTestHelpers.card.code,
        )

        assertThat(observedArgs).isEqualTo(
            PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = CardDefinition.type.code,
                amount = Amount(
                    value = 1099,
                    currencyCode = "usd",
                ),
                hasIntentToSetup = true,
                billingDetails = PaymentSheet.BillingDetails(),
            )
        )
    }

    @Test
    fun `onFormFieldValuesChanged calls selection updater with transformed card`() = runTest {
        val cardBrand = "visa"
        val name = "Joe"
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.CardBrand to FormFieldEntry(cardBrand, true),
                IdentifierSpec.Name to FormFieldEntry(name, true),
            ),
            userRequestedReuse = customerRequestedSave,
        )
        var hasCalledSelectionUpdater = false
        createFormHelper(
            newPaymentSelectionProvider = { null },
            selectionUpdater = { paymentSelection ->
                val cardPaymentSelection = paymentSelection as PaymentSelection.New.Card
                assertThat(cardPaymentSelection.brand.code).isEqualTo(cardBrand)
                assertThat(cardPaymentSelection.customerRequestedSave).isEqualTo(customerRequestedSave)
                assertThat(getNameFromParams(cardPaymentSelection.paymentMethodCreateParams)).isEqualTo(name)
                hasCalledSelectionUpdater = true
            }
        ).onFormFieldValuesChanged(formFieldValues, "card")
        assertThat(hasCalledSelectionUpdater).isTrue()
    }

    @Test
    fun `onPaymentMethodFormCompleted event emitted when form is filled`() = runScenario {
        enableKlarnaFormRemovalRule.setEnabled(true)
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.Country to FormFieldEntry("US", true),
                IdentifierSpec.Email to FormFieldEntry("Joe@stripe.com", true),
            ),
            userRequestedReuse = customerRequestedSave,
        )
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
            eventReporter = eventReporter,
            newPaymentSelectionProvider = { null },
            selectionUpdater = {},
        )
        formHelper.onFormFieldValuesChanged(formFieldValues, "klarna")
        val event = eventReporter.formCompletedCalls.awaitItem()
        assertThat(event.code).isEqualTo("klarna")
        enableKlarnaFormRemovalRule.setEnabled(false)
    }

    @Test
    fun `onPaymentMethodFormCompleted event should not be emitted when form is filled twice`() = runScenario {
        enableKlarnaFormRemovalRule.setEnabled(true)
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
            eventReporter = eventReporter,
            newPaymentSelectionProvider = { null },
            selectionUpdater = {},
        )

        val firstFormFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.Country to FormFieldEntry("US", true),
                IdentifierSpec.Email to FormFieldEntry("Joe@stripe.com", true),
            ),
            userRequestedReuse = customerRequestedSave,
        )
        formHelper.onFormFieldValuesChanged(firstFormFieldValues, "klarna")
        val event = eventReporter.formCompletedCalls.awaitItem()
        assertThat(event.code).isEqualTo("klarna")

        val secondFormFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.Country to FormFieldEntry("UK", true),
                IdentifierSpec.Email to FormFieldEntry("Joey@stripe.com", true),
            ),
            userRequestedReuse = customerRequestedSave,
        )
        formHelper.onFormFieldValuesChanged(secondFormFieldValues, "klarna")
        enableKlarnaFormRemovalRule.setEnabled(false)
    }

    @Test
    fun `onPaymentMethodFormCompleted event emitted when different forms are filled`() = runScenario {
        enableKlarnaFormRemovalRule.setEnabled(true)
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
            eventReporter = eventReporter,
            newPaymentSelectionProvider = { null },
            selectionUpdater = {},
        )

        val klarnaFormFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.Country to FormFieldEntry("US", true),
                IdentifierSpec.Email to FormFieldEntry("Joe@stripe.com", true),
            ),
            userRequestedReuse = customerRequestedSave,
        )
        formHelper.onFormFieldValuesChanged(klarnaFormFieldValues, "klarna")
        val klarnaEvent = eventReporter.formCompletedCalls.awaitItem()
        assertThat(klarnaEvent.code).isEqualTo("klarna")

        val cardFormFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                IdentifierSpec.Name to FormFieldEntry("joe", true),
            ),
            userRequestedReuse = customerRequestedSave,
        )
        formHelper.onFormFieldValuesChanged(cardFormFieldValues, "card")
        val cardEvent = eventReporter.formCompletedCalls.awaitItem()
        assertThat(cardEvent.code).isEqualTo("card")
        enableKlarnaFormRemovalRule.setEnabled(false)
    }

    @Test
    fun `onFormFieldValuesChanged & onLinkStateChanged calls create Link Inline selection when card`() = runTest {
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                IdentifierSpec.Name to FormFieldEntry("Joe", true),
            ),
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
        )

        val userInput = UserInput.SignUp(
            email = "email@email.com",
            phone = "1234567890",
            country = "CA",
            name = "John Doe",
            consentAction = SignUpConsentAction.Checkbox,
        )

        runLinkInlineTest(
            formFieldValues = formFieldValues,
            paymentMethodCode = "card",
            inlineSignupViewState = InlineSignupViewState(
                merchantName = "Merchant Inc.",
                signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
                fields = emptyList(),
                prefillEligibleFields = emptySet(),
                userInput = userInput,
                allowsDefaultOptIn = false,
                linkSignUpOptInFeatureEnabled = false,
                linkBrand = LinkBrand.Link,
            ),
        ) {
            assertThat(expectMostRecentItem()).isEqualTo(
                PaymentSelection.New.Card(
                    paymentMethodCreateParams = formFieldValues.transformToPaymentMethodCreateParams(
                        paymentMethodCode = "card",
                        paymentMethodMetadata = PaymentMethodMetadataFactory.create()
                    ),
                    paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank,
                    ),
                    paymentMethodExtraParams = PaymentMethodExtraParams.Card(
                        setAsDefault = null
                    ),
                    brand = CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
                    linkInput = userInput,
                )
            )
        }
    }

    @Test
    fun `Skips Link if not being used`() = runTest {
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                IdentifierSpec.Name to FormFieldEntry("Joe", true),
            ),
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
        )

        runLinkInlineTest(
            formFieldValues = formFieldValues,
            paymentMethodCode = "card",
            inlineSignupViewState = InlineSignupViewState(
                merchantName = "Merchant Inc.",
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                fields = emptyList(),
                prefillEligibleFields = emptySet(),
                userInput = UserInput.SignUp(
                    email = "email@email.com",
                    phone = "1234567890",
                    country = "CA",
                    name = "John Doe",
                    consentAction = SignUpConsentAction.Checkbox,
                ),
                isExpanded = false,
                allowsDefaultOptIn = false,
                linkSignUpOptInFeatureEnabled = false,
                linkBrand = LinkBrand.Link,
            )
        ) {
            assertThat(expectMostRecentItem()).isEqualTo(
                PaymentSelection.New.Card(
                    paymentMethodCreateParams = formFieldValues.transformToPaymentMethodCreateParams(
                        paymentMethodCode = "card",
                        paymentMethodMetadata = PaymentMethodMetadataFactory.create()
                    ),
                    paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank,
                    ),
                    paymentMethodExtraParams = PaymentMethodExtraParams.Card(
                        setAsDefault = null
                    ),
                    brand = CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
                )
            )
        }
    }

    @Test
    fun `onFormFieldValuesChanged & onLinkStateChanged calls create generic selection when not card`() = runTest {
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.Name to FormFieldEntry("Joe", true),
            ),
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
        )

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("card", "bancontact")
            )
        )

        runLinkInlineTest(
            formFieldValues = formFieldValues,
            paymentMethodCode = "bancontact",
            paymentMethodMetadata = paymentMethodMetadata,
            inlineSignupViewState = InlineSignupViewState(
                merchantName = "Merchant Inc.",
                signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
                fields = emptyList(),
                prefillEligibleFields = emptySet(),
                userInput = UserInput.SignUp(
                    email = "email@email.com",
                    phone = "1234567890",
                    country = "CA",
                    name = "John Doe",
                    consentAction = SignUpConsentAction.Checkbox,
                ),
                allowsDefaultOptIn = false,
                linkSignUpOptInFeatureEnabled = false,
                linkBrand = LinkBrand.Link,
            )
        ) {
            assertThat(expectMostRecentItem()).isEqualTo(
                PaymentSelection.New.GenericPaymentMethod(
                    paymentMethodCreateParams = formFieldValues.transformToPaymentMethodCreateParams(
                        paymentMethodCode = "bancontact",
                        paymentMethodMetadata = paymentMethodMetadata,
                    ),
                    paymentMethodOptionsParams = null,
                    paymentMethodExtraParams = null,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
                    label = resolvableString(R.string.stripe_paymentsheet_payment_method_bancontact),
                    iconResource = R.drawable.stripe_ic_paymentsheet_pm_bancontact,
                    iconResourceNight = null,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                )
            )
        }
    }

    @Test
    fun `Creates null selection if Link input is null when expanded`() = runTest {
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                IdentifierSpec.Name to FormFieldEntry("Joe", true),
            ),
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
        )

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("card", "bancontact")
            )
        )

        val selection = MutableStateFlow<PaymentSelection?>(null)

        selection.test {
            assertThat(awaitItem()).isNull()

            val linkInlineHandler = LinkInlineHandler.create()
            val formHelper = createFormHelper(
                paymentMethodMetadata = paymentMethodMetadata,
                linkInlineHandler = linkInlineHandler,
                selectionUpdater = { paymentSelection ->
                    selection.value = paymentSelection
                },
                newPaymentSelectionProvider = { null }
            )

            formHelper.onFormFieldValuesChanged(formFieldValues, "card")

            assertThat(awaitItem()).isEqualTo(
                PaymentSelection.New.Card(
                    paymentMethodCreateParams = formFieldValues.transformToPaymentMethodCreateParams(
                        paymentMethodCode = "card",
                        paymentMethodMetadata = PaymentMethodMetadataFactory.create()
                    ),
                    paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank,
                    ),
                    paymentMethodExtraParams = PaymentMethodExtraParams.Card(
                        setAsDefault = null
                    ),
                    brand = CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
                )
            )

            linkInlineHandler.onStateUpdated(
                InlineSignupViewState(
                    merchantName = "Merchant Inc.",
                    signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                    fields = emptyList(),
                    prefillEligibleFields = emptySet(),
                    userInput = null,
                    isExpanded = true,
                    allowsDefaultOptIn = false,
                    linkSignUpOptInFeatureEnabled = false,
                    linkBrand = LinkBrand.Link,
                )
            )

            assertThat(awaitItem()).isNull()

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `getPaymentMethodParams returns correct payment method params`() = runTest {
        val cardBrand = "visa"
        val name = "Joe"
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.CardBrand to FormFieldEntry(cardBrand, true),
                IdentifierSpec.Name to FormFieldEntry(name, true),
            ),
            userRequestedReuse = customerRequestedSave,
        )

        val formHelper = createFormHelper { }
        val params = formHelper.getPaymentMethodParams(formFieldValues, "card")

        assertThat(params?.let { getNameFromParams(it) }).isEqualTo(name)
        assertThat(params?.typeCode).isEqualTo("card")
    }

    @Test
    fun `formTypeForCode returns Empty for an LPM with no fields`() = runTest {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                )
            ),
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.Empty)
    }

    @Test
    fun `formTypeForCode returns MandateOnly for an LPM with no fields, but a mandate`() = runTest {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                )
            ),
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.formTypeForCode("cashapp")).isInstanceOf<FormHelper.FormType.MandateOnly>()
    }

    @Test
    fun `formTypeForCode returns UserInteractionRequired for an LPM with no fields, but requires name`() = runTest {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.formTypeForCode("cashapp")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
    }

    @Test
    fun `formTypeForCode returns UserInteractionRequired for an LPM with fields`() = runTest {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "afterpay_clearpay"),
                ),
            ),
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.formTypeForCode("afterpay_clearpay"))
            .isEqualTo(FormHelper.FormType.UserInteractionRequired)
    }

    @Test
    fun `formTypeForCode returns UserInteractionRequired for non form field based LPM us_bank_account`() = runTest {
        val formHelper = createFormHelper(
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.formTypeForCode("us_bank_account")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
        assertThat(formHelper.formTypeForCode("link")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
    }

    @Test
    fun `server driven form spec can require a form screen for an arbitrary code`() = runTest {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                serverDrivenFormSpecs = listOf(
                    PaymentMethodFormSpec(
                        type = "server_selected_type",
                        fields = listOf(
                            FormElementSpec(
                                type = "native_component",
                                component = "link_card_collection",
                            )
                        ),
                        requiresFormScreen = true,
                    ),
                ),
            ),
            newPaymentSelectionProvider = { null },
        )

        assertThat(formHelper.formTypeForCode("server_selected_type"))
            .isEqualTo(FormHelper.FormType.UserInteractionRequired)
    }

    @Test
    fun `server driven form spec does not infer form screen behavior from link code`() = runTest {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                serverDrivenFormSpecs = listOf(
                    PaymentMethodFormSpec(
                        type = PaymentMethod.Type.Link.code,
                        fields = listOf(
                            FormElementSpec(
                                type = "native_component",
                                component = "link_card_collection",
                            )
                        ),
                        requiresFormScreen = false,
                    ),
                ),
            ),
            newPaymentSelectionProvider = { null },
        )

        assertThat(formHelper.formTypeForCode(PaymentMethod.Type.Link.code))
            .isEqualTo(FormHelper.FormType.Empty)
    }

    @Test
    fun `formElementsForCode returns PMM promotion header if available`() = runTest {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "afterpay_clearpay"),
                )
            ),
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(
                promotions = listOf(
                    PaymentMethodMessagePromotion(
                        paymentMethodType = "AFTERPAY_CLEARPAY",
                        message = "This is a message",
                        learnMore = PaymentMethodMessageLearnMore(
                            message = "Click me",
                            url = "https://test.com"
                        )
                    )
                )
            ),
            newPaymentSelectionProvider = { null }
        )

        val elements = formHelper.formElementsForCode(PaymentMethod.Type.AfterpayClearpay.code)
        val header = elements[0] as PaymentMethodMessageHeaderElement
        assertThat(header).isInstanceOf<PaymentMethodMessageHeaderElement>()
        assertThat(header.promotion).isNotNull()
    }

    private fun runLinkInlineTest(
        formFieldValues: FormFieldValues,
        inlineSignupViewState: InlineSignupViewState?,
        paymentMethodCode: String,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        test: suspend TurbineTestContext<PaymentSelection?>.() -> Unit,
    ) = runTest {
        val selection = MutableStateFlow<PaymentSelection?>(null)

        selection.test {
            assertThat(awaitItem()).isNull()

            val linkInlineHandler = LinkInlineHandler.create()
            val formHelper = createFormHelper(
                paymentMethodMetadata = paymentMethodMetadata,
                linkInlineHandler = linkInlineHandler,
                selectionUpdater = { paymentSelection ->
                    selection.value = paymentSelection
                },
                newPaymentSelectionProvider = { null },
            )

            formHelper.onFormFieldValuesChanged(formFieldValues, paymentMethodCode)
            linkInlineHandler.onStateUpdated(inlineSignupViewState)

            test()

            ensureAllEventsConsumed()
        }
    }

    private fun createFormHelper(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        linkInlineHandler: LinkInlineHandler = LinkInlineHandler.create(),
        eventReporter: FakeEventReporter = FakeEventReporter(),
        tapToAddHelper: TapToAddHelper? = null,
        paymentMethodMessagePromotionsHelper: FakePaymentMethodMessagePromotionsHelper =
            FakePaymentMethodMessagePromotionsHelper(),
        newPaymentSelectionProvider: (PaymentMethodCode) -> NewPaymentOptionSelection? =
            { throw AssertionError("Not implemented") },
        selectionUpdater: (PaymentSelection?) -> Unit = { throw AssertionError("Not implemented") },
    ): FormHelper {
        return DefaultFormHelper(
            coroutineScope = coroutineScopeCleanupRule.track(CoroutineScope(UnconfinedTestDispatcher())),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = newPaymentSelectionProvider,
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            linkInlineHandler = linkInlineHandler,
            selectionUpdater = selectionUpdater,
            setAsDefaultMatchesSaveForFutureUse = false,
            eventReporter = eventReporter,
            savedStateHandle = SavedStateHandle(),
            autocompleteAddressInteractorFactory = null,
            automaticallyLaunchedCardScanFormDataHelper = null,
            tapToAddHelper = tapToAddHelper,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
            isNfcScanningAvailable = null,
        )
    }

    private fun runScenario(
        eventReporter: FakeEventReporter = FakeEventReporter(),
        block: suspend Scenario.() -> Unit,
    ) {
        Scenario(
            eventReporter = eventReporter,
        ).apply {
            runTest {
                block()
            }
        }
        eventReporter.validate()
    }

    private data class Scenario(
        val eventReporter: FakeEventReporter,
    )
}
