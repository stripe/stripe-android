package com.stripe.android.customersheet

import androidx.compose.ui.graphics.Color
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.customersheet.data.CustomerSheetSession
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SavedSelection
import java.util.UUID

internal object CustomerSheetFixtures {
    val MINIMUM_CONFIG = CustomerSheet.Configuration
        .builder(merchantDisplayName = "Merchant, Inc")
        .build()

    val CONFIG_WITH_GOOGLE_PAY_ENABLED = CustomerSheet.Configuration
        .builder(merchantDisplayName = "Merchant, Inc")
        .googlePayEnabled(googlePayEnabled = true)
        .build()

    @OptIn(ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class)
    val CONFIG_WITH_EVERYTHING = CustomerSheet.Configuration
        .builder(merchantDisplayName = "Merchant, Inc")
        .defaultBillingDetails(PaymentSheet.BillingDetails(name = "Skyler"))
        .headerTextForSelectionScreen("Select a payment method!")
        .appearance(
            PaymentSheet.Appearance(
                colorsLight = PaymentSheet.Colors.configureDefaultLight(primary = Color(0)),
                colorsDark = PaymentSheet.Colors.configureDefaultDark(primary = Color(0)),
                shapes = PaymentSheet.Shapes(
                    cornerRadiusDp = 0.0f,
                    borderStrokeWidthDp = 0.0f
                ),
                typography = PaymentSheet.Typography(
                    sizeScaleFactor = 1.1f,
                    fontResId = 0
                ),
                primaryButton = PaymentSheet.PrimaryButton(
                    colorsLight = PaymentSheet.PrimaryButtonColors(background = 0, onBackground = 0, border = 0),
                    colorsDark = PaymentSheet.PrimaryButtonColors(background = 0, onBackground = 0, border = 0),
                    shape = PaymentSheet.PrimaryButtonShape(
                        cornerRadiusDp = 0.0f,
                        borderStrokeWidthDp = 20.0f
                    ),
                    typography = PaymentSheet.PrimaryButtonTypography(
                        fontResId = 0
                    )
                )
            )
        )
        .billingDetailsCollectionConfiguration(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = true,
            )
        )
        .preferredNetworks(listOf(CardBrand.CartesBancaires, CardBrand.Visa))
        .allowsRemovalOfLastSavedPaymentMethod(false)
        .paymentMethodOrder(listOf("klarna", "afterpay", "card"))
        .googlePayEnabled(googlePayEnabled = true)
        .build()

    fun createCustomerSheetSession(
        hasCustomerSession: Boolean = false,
        isPaymentMethodSyncDefaultEnabled: Boolean = false,
        hasDefaultPaymentMethod: Boolean = false
    ): CustomerSheetSession {
        val customer = getCustomer(hasCustomerSession, isPaymentMethodSyncDefaultEnabled, hasDefaultPaymentMethod)

        val elementsSession = ElementsSession(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            cardBrandChoice = null,
            merchantCountry = null,
            isGooglePayEnabled = false,
            customer = customer,
            linkSettings = null,
            externalPaymentMethodData = null,
            customPaymentMethods = emptyList(),
            paymentMethodSpecs = null,
            flags = emptyMap(),
            elementsSessionId = "session_1234",
            orderedPaymentMethodTypesAndWallets = listOf("card"),
            experimentsData = null,
            passiveCaptcha = null,
            merchantLogoUrl = null,
            elementsSessionConfigId = UUID.randomUUID().toString(),
        )

        return CustomerSheetSession(
            elementsSession = elementsSession,
            paymentMethods = listOf(),
            savedSelection = SavedSelection.None,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            permissions = CustomerPermissions(
                removePaymentMethod = PaymentMethodRemovePermission.Full,
                canRemoveLastPaymentMethod = true,
                canUpdateFullPaymentMethodDetails = true,
            ),
            defaultPaymentMethodId = null,
            customerId = customer?.session?.customerId ?: "unused_for_customer_adapter_data_source",
            customerEphemeralKeySecret = customer?.session?.apiKey ?: "unused_for_customer_adapter_data_source",
            customerSessionClientSecret = customer?.session?.id,
        )
    }

    private fun getCustomer(
        hasCustomerSession: Boolean,
        isPaymentMethodSyncDefaultEnabled: Boolean,
        hasDefaultPaymentMethod: Boolean
    ) = if (hasCustomerSession) {
        ElementsSession.Customer(
            paymentMethods = listOf(),
            session = ElementsSession.Customer.Session(
                id = "cuss_123",
                customerId = "cus_123",
                liveMode = false,
                apiKey = "ek_123",
                apiKeyExpiry = 999999999,
                components = ElementsSession.Customer.Components(
                    mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                    customerSheet = ElementsSession.Customer.Components.CustomerSheet.Enabled(
                        paymentMethodRemove =
                        ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Disabled,
                        paymentMethodRemoveLast =
                        ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
                        isPaymentMethodSyncDefaultEnabled = isPaymentMethodSyncDefaultEnabled,
                    ),
                )
            ),
            defaultPaymentMethod = if (hasDefaultPaymentMethod) "pm_123" else null,
        )
    } else {
        null
    }
}
