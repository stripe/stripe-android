package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.getDefaultCustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.screenshottesting.LayoutDirection
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.utils.screenshots.PaymentSheetAppearance.DefaultAppearance
import org.junit.Rule
import org.junit.Test

class CardUiDefinitionFactoryTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @get:Rule
    val rightToLeftPaparazziRule = PaparazziRule(
        listOf(LayoutDirection.RightToLeft)
    )

    @get:Rule
    val customSpacingPaparazziRule = PaparazziRule(
        listOf(CustomSpacingAppearance)
    )

    @get:Rule
    val customTextFieldsPaparazziRule = PaparazziRule(
        listOf(CustomTextInsetsAppearance)
    )

    private val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card"),
        )
    )

    @Test
    fun testCard() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata
            )
        }
    }

    @Test
    fun testCardWithValidation() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                    )
                ),
                isValidating = true,
            )
        }
    }

    @Test
    fun testCardWithDefaultValues() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata,
                paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
                    code = "card",
                    billingDetails = null,
                    requiresMandate = false,
                    overrideParamMap = mapOf(
                        "type" to "card",
                        "card" to mapOf(
                            "number" to "4242424242424242",
                            "exp_month" to "07",
                            "exp_year" to "2050",
                            "cvc" to "123",
                        ),
                        "billing_details" to mapOf(
                            "country" to "US",
                            "postal_code" to "94080",
                        ),
                    ),
                    productUsage = emptySet(),
                )
            )
        }
    }

    @Test
    fun testCardWithBillingDetailsCollectionConfiguration() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    )
                )
            )
        }
    }

    @Test
    fun testCardWithBillingDetailsCollectionConfigurationAndValidation() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    )
                ),
                isValidating = true,
            )
        }
    }

    @Test
    fun testCardWithSaveForLater() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    customerMetadata = getDefaultCustomerMetadata(
                        isPaymentMethodSetAsDefaultEnabled = false
                    ),
                )
            )
        }
    }

    @Test
    fun testCardWithSaveForLaterAndSetAsDefaultShown() {
        val formElements = CardDefinition.formElements(
            metadata = metadata.copy(
                customerMetadata = getDefaultCustomerMetadata(
                    isPaymentMethodSetAsDefaultEnabled = true
                ),
            )
        )

        val saveForFutureUseElement = formElements.first {
            it.identifier == IdentifierSpec.SaveForFutureUse
        } as SaveForFutureUseElement

        saveForFutureUseElement.controller.onValueChange(true)

        paparazziRule.snapshot {
            FormUI(
                hiddenIdentifiers = emptySet(),
                enabled = true,
                elements = formElements,
                lastTextFieldIdentifier = null,
            )
        }
    }

    @Test
    fun testCardWithSameAsShipping() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    shippingDetails = AddressDetails(
                        address = PaymentSheet.Address(
                            line1 = "354 Oyster Point Blvd",
                            city = "South San Francisco",
                            state = "CA",
                            country = "US",
                            postalCode = "94080",
                        ),
                        isCheckboxSelected = true
                    ),
                )
            )
        }
    }

    @Test
    fun testCardWithMandate() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                        paymentMethodTypes = listOf("card"),
                    ),
                ),
            )
        }
    }

    @Test
    fun testCardWithMandateAndSaveForLater() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                        paymentMethodTypes = listOf("card"),
                    ),
                    paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
                    customerMetadata = getDefaultCustomerMetadata(
                        isPaymentMethodSetAsDefaultEnabled = false,
                    ),
                ),
            )
        }
    }

    @Test
    fun testCardWithCustomSpacing() {
        customSpacingPaparazziRule.snapshot {
            val setupIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "link"),
            )

            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    stripeIntent = setupIntent,
                    paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
                    customerMetadata = getDefaultCustomerMetadata(),
                ),
            )
        }
    }

    @Test
    fun testCardWithCustomTextFieldInsets() {
        customTextFieldsPaparazziRule.snapshot {
            val setupIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "link"),
            )

            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    stripeIntent = setupIntent,
                    paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
                    customerMetadata = getDefaultCustomerMetadata(),
                ),
            )
        }
    }

    @Test
    fun testCardRightToLeft() {
        rightToLeftPaparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
                    code = "card",
                    billingDetails = null,
                    requiresMandate = false,
                    overrideParamMap = mapOf(
                        "type" to "card",
                        "card" to mapOf(
                            "number" to "4242424242424242",
                            "exp_month" to "07",
                            "exp_year" to "2050",
                            "cvc" to "123",
                        ),
                        "billing_details" to mapOf(
                            "name" to "John Doe",
                            "phone" to "+11234567890",
                            "address" to mapOf(
                                "country" to "US",
                                "line1" to "123 Apple Street",
                                "state" to "CA",
                                "city" to "South San Francisco",
                                "postal_code" to "94080",
                            )
                        ),
                    ),
                    productUsage = emptySet(),
                ),
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    ),
                ),
            )
        }
    }

    @Test
    fun testCondensedAutocompleteForm() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
                    code = "card",
                    billingDetails = null,
                    requiresMandate = false,
                    overrideParamMap = mapOf(
                        "type" to "card",
                        "card" to mapOf(
                            "number" to "4242424242424242",
                            "exp_month" to "07",
                            "exp_year" to "2050",
                            "cvc" to "123",
                        ),
                        "billing_details" to emptyMap<IdentifierSpec, String?>(),
                    ),
                    productUsage = emptySet(),
                ),
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    ),
                ),
                autocompleteAddressInteractorFactory = {
                    TestAutocompleteAddressInteractor.noOp(
                        autocompleteConfig = AutocompleteAddressInteractor.Config(
                            googlePlacesApiKey = "123",
                            autocompleteCountries = setOf("US"),
                            isPlacesAvailable = true,
                        )
                    )
                }
            )
        }
    }

    @Test
    fun testExpandedAutocompleteForm() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
                    code = "card",
                    billingDetails = null,
                    requiresMandate = false,
                    overrideParamMap = mapOf(
                        "type" to "card",
                        "card" to mapOf(
                            "number" to "4242424242424242",
                            "exp_month" to "07",
                            "exp_year" to "2050",
                            "cvc" to "123",
                        ),
                        "billing_details" to mapOf(
                            "address" to mapOf(
                                "country" to "US",
                                "line1" to "123 Apple Street",
                                "state" to "CA",
                                "city" to "South San Francisco",
                                "postal_code" to "94080",
                            )
                        ),
                    ),
                    productUsage = emptySet(),
                ),
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    ),
                ),
                autocompleteAddressInteractorFactory = {
                    TestAutocompleteAddressInteractor.noOp(
                        autocompleteConfig = AutocompleteAddressInteractor.Config(
                            googlePlacesApiKey = "123",
                            autocompleteCountries = setOf("US"),
                            isPlacesAvailable = true,
                        )
                    )
                }
            )
        }
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    private data object CustomSpacingAppearance : PaparazziConfigOption {
        private val appearance = PaymentSheet.Appearance.Builder()
            .sectionSpacing(PaymentSheet.Spacing(spacingDp = 50f))
            .build()

        override fun initialize() {
            appearance.parseAppearance()
        }

        override fun reset() {
            DefaultAppearance.appearance.parseAppearance()
        }
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    private data object CustomTextInsetsAppearance : PaparazziConfigOption {
        private val appearance = PaymentSheet.Appearance.Builder()
            .textFieldInsets(
                PaymentSheet.Insets(
                    startDp = 24f,
                    endDp = 20f,
                    topDp = 28f,
                    bottomDp = 20f,
                )
            )
            .build()

        override fun initialize() {
            appearance.parseAppearance()
        }

        override fun reset() {
            DefaultAppearance.appearance.parseAppearance()
        }
    }
}
