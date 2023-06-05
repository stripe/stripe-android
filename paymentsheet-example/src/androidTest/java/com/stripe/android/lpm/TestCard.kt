package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.test.core.Billing
import com.stripe.android.test.core.Customer
import com.stripe.android.ui.core.forms.resources.LpmRepository
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCard : BaseLpmTest() {
    @Test
    fun testCard() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                customer = Customer.New,
                billing = Billing.On,
                paymentMethod = LpmRepository.HardcodedCard,
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
                saveCheckboxValue = false,
            )
        )
    }

    @Test
    fun testCardWithCustomBillingDetailsCollection() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                customer = Customer.New,
                billing = Billing.On,
                paymentMethod = LpmRepository.HardcodedCard,
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
                saveCheckboxValue = false,
                attachDefaults = false,
                collectName = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                collectEmail = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                collectPhone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                collectAddress = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
        )
    }

    @Test
    fun testCardInCustomFlow() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = LpmRepository.HardcodedCard,
                saveCheckboxValue = true,
                saveForFutureUseCheckboxVisible = true,
            )
        )
    }
}
