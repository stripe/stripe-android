package com.stripe.android.paymentsheet

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
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
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParams.Companion.getNameFromParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.setupFutureUsage
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class FormHelperTest {

    @Test
    fun `formElementsForCode with unknown code returns empty list`() = runTest {
        val formHelper = createFormHelper(
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.formElementsForCode("blah")).isEmpty()
    }

    @Test
    fun `formElementsForCode returns klarna form elements`() = runTest {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                )
            ),
            newPaymentSelectionProvider = { null },
        )
        val formElements = formHelper.formElementsForCode("klarna")
        assertThat(formElements).hasSize(3)
        // Email field has an empty string for value
        assertThat(formElements[1].getFormFieldValueFlow().value[0].first.v1).isEqualTo("billing_details[email]")
        assertThat(formElements[1].getFormFieldValueFlow().value[0].second.value).isEqualTo("")
    }

    @Test
    fun `formElementsForCode returns klarna form elements without using current selection values`() = runTest {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                )
            ),
            newPaymentSelectionProvider = {
                NewOrExternalPaymentSelection.New(
                    PaymentSelection.New.GenericPaymentMethod(
                        label = "Cash App".resolvableString,
                        iconResource = 0,
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
        assertThat(formElements).hasSize(3)
        assertThat(formElements[1].getFormFieldValueFlow().value[0].first.v1).isEqualTo("billing_details[email]")
        assertThat(formElements[1].getFormFieldValueFlow().value[0].second.value).isEqualTo("")
    }

    @Test
    fun `formElementsForCode returns klarna form elements using current selection values`() = runTest {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                )
            ),
            newPaymentSelectionProvider = {
                NewOrExternalPaymentSelection.New(
                    PaymentSelection.New.GenericPaymentMethod(
                        label = "Klarna".resolvableString,
                        iconResource = 0,
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
        assertThat(formElements).hasSize(3)
        assertThat(formElements[1].getFormFieldValueFlow().value[0].first.v1).isEqualTo("billing_details[email]")
        assertThat(formElements[1].getFormFieldValueFlow().value[0].second.value).isEqualTo("example@email.com")
    }

    @Test
    fun `createFormArguments produces the correct form arguments when payment intent is off-session`() = runTest {
        val observedArgs = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
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
            ),
        ) {
            assertThat(expectMostRecentItem()).isEqualTo(
                PaymentSelection.New.LinkInline(
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
                    input = userInput,
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
                }
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
                    paymentMethodTypes = listOf("card", "klarna"),
                ),
            ),
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.formTypeForCode("klarna")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
    }

    @Test
    fun `formTypeForCode returns UserInteractionRequired for non form field based LPM us_bank_account`() = runTest {
        val formHelper = createFormHelper(
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.formTypeForCode("us_bank_account")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
        assertThat(formHelper.formTypeForCode("link")).isEqualTo(FormHelper.FormType.UserInteractionRequired)
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
                }
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
        newPaymentSelectionProvider: () -> NewOrExternalPaymentSelection? = { throw AssertionError("Not implemented") },
        selectionUpdater: (PaymentSelection?) -> Unit = { throw AssertionError("Not implemented") },
    ): FormHelper {
        return DefaultFormHelper(
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = newPaymentSelectionProvider,
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            linkInlineHandler = linkInlineHandler,
            selectionUpdater = selectionUpdater,
        )
    }
}
