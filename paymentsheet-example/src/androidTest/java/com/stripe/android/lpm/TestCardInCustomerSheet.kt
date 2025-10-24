package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.settings.CollectAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectEmailSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectNameSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectPhoneSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionOnBehalfOfSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSheetPaymentMethodModeDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.GooglePaySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodMode
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.FieldPopulator
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCardInCustomerSheet : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "card",
        authorizationAction = null,
    ).copyPlaygroundSettings { settings ->
        settings[CustomerSettingsDefinition] = CustomerType.NEW
        settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.CreateAndAttach
        settings[GooglePaySettingsDefinition] = false
    }

    @Test
    fun testCard() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CountrySettingsDefinition] = Country.US
            },
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCardWithNonUsMerchant() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CountrySettingsDefinition] = Country.FR
            },
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCardWithSetupIntent() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CountrySettingsDefinition] = Country.US
                settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.SetupIntent
            },
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCardWithOBO_DoNotSelectCardBrand() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CountrySettingsDefinition] = Country.US
                settings[CustomerSessionSettingsDefinition] = true
                settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.SetupIntent
                settings[CustomerSessionOnBehalfOfSettingsDefinition] =
                    CustomerSessionOnBehalfOfSettingsDefinition.OnBehalfOf.FR_CONNECTED_ACCOUNT
                settings[GooglePaySettingsDefinition] = false
            },

            populateCustomLpmFields = {
                populateCardBrandChoiceCardDetails(null)
            },
        )
    }

    @Test
    fun testCardWithOBO_UseCartesBancaires() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[GooglePaySettingsDefinition] = false
                settings[CountrySettingsDefinition] = Country.US
                settings[CustomerSessionOnBehalfOfSettingsDefinition] =
                    CustomerSessionOnBehalfOfSettingsDefinition.OnBehalfOf.FR_CONNECTED_ACCOUNT
                settings[CustomerSessionSettingsDefinition] = true
                settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.SetupIntent
            },

            populateCustomLpmFields = {
                populateCardBrandChoiceCardDetails("Cartes Bancaires")
            },
        )
    }

    @Test
    fun testCardWithOBO_UseVisa() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CountrySettingsDefinition] = Country.US
                settings[CustomerSessionSettingsDefinition] = true
                settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.SetupIntent
                settings[CustomerSessionOnBehalfOfSettingsDefinition] =
                    CustomerSessionOnBehalfOfSettingsDefinition.OnBehalfOf.FR_CONNECTED_ACCOUNT
                settings[GooglePaySettingsDefinition] = false
            },

            populateCustomLpmFields = {
                populateCardBrandChoiceCardDetails("Visa")
            },
        )
    }

    @Test
    fun testCardWith3ds2() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CountrySettingsDefinition] = Country.US
                settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.SetupIntent
            }.copy(
                authorizationAction = AuthorizeAction.Authorize3ds2,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000000000003220")
        )
    }

    @Test
    fun testCardWithSetupIntentAndNonUsMerchant() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CountrySettingsDefinition] = Country.FR
                settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.SetupIntent
            },
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCardWithBillingDetailsCollection() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CountrySettingsDefinition] = Country.US
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.Off
                settings[CollectNameSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectEmailSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectPhoneSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            },
            populateCustomLpmFields = {
                populateCardDetails()
                populateEmail()
                populateName("Name on card")
                populateAddress()
                populatePhoneNumber()
            },
        )
    }

    @Test
    fun testCardWithBillingDetailsCollectionWithDefaults() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CountrySettingsDefinition] = Country.US
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
                settings[CollectNameSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectEmailSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectPhoneSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            },
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }
}
