package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.settings.CollectAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectEmailSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectNameSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectPhoneSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.test.core.TestParameters
import com.stripe.android.ui.core.forms.resources.LpmRepository
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCard : BasePlaygroundTest() {
    @Test
    fun testCard() {
        testDriver.confirmNewOrGuestComplete(
            TestParameters.create(
                paymentMethod = LpmRepository.HardcodedCard,
            ).copy(
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
            )
        )
    }

    @Test
    fun testCardWithCustomBillingDetailsCollection() {
        testDriver.confirmNewOrGuestComplete(
            TestParameters.create(
                paymentMethod = LpmRepository.HardcodedCard,
            ) { settings ->
                settings[DefaultBillingAddressSettingsDefinition] = true
                settings[CollectNameSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectEmailSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectPhoneSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            }.copy(
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
            ),
        )
    }

    @Test
    fun testCardInCustomFlow() {
        testDriver.confirmCustom(
            TestParameters.create(
                paymentMethod = LpmRepository.HardcodedCard,
            ).copy(
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
            )
        )
    }
}
