package com.stripe.android.financialconnections.features.linkaccountpicker

import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkAccountPickerStateTest {

    private val partnerAccount1 = partnerAccount().copy(id = "acct1", name = "Account 1", status = FinancialConnectionsAccount.Status.INACTIVE)
    private val partnerAccount2 = partnerAccount().copy(id = "acct2", name = "Account 2", status = FinancialConnectionsAccount.Status.ACTIVE)

    private val accessibleDataCalloutModel = AccessibleDataCalloutModel(
        businessName = "My business",
        permissions = listOf(
            FinancialConnectionsAccount.Permissions.PAYMENT_METHOD,
            FinancialConnectionsAccount.Permissions.BALANCES,
            FinancialConnectionsAccount.Permissions.OWNERSHIP,
            FinancialConnectionsAccount.Permissions.TRANSACTIONS
        ),
        isStripeDirect = false,
        isNetworking = false,
        dataPolicyUrl = ""
    )

    @Test
    fun `ctaText should return repair CTA account is repairable`() {
        val payload = LinkAccountPickerState.Payload(
            accounts = listOf(partnerAccount1, partnerAccount2),
            accessibleData = accessibleDataCalloutModel,
            businessName = "Business Name",
            consumerSessionClientSecret = "secret",
            stepUpAuthenticationRequired = false
        )

        val pickerState = LinkAccountPickerState(
            payload = Success(payload),
            selectedAccountId = partnerAccount1.id
        )

        assertEquals(R.string.stripe_link_account_picker_repair_cta, pickerState.ctaText)
    }

    @Test
    fun `ctaText should return connect CTA when account is not repairable`() {
        val payload = LinkAccountPickerState.Payload(
            accounts = listOf(partnerAccount1, partnerAccount2),
            accessibleData = accessibleDataCalloutModel,
            businessName = "Business Name",
            consumerSessionClientSecret = "secret",
            stepUpAuthenticationRequired = false
        )

        val pickerState = LinkAccountPickerState(
            payload = Success(payload),
            selectedAccountId = partnerAccount2.id
        )

        assertEquals(R.string.stripe_link_account_picker_cta, pickerState.ctaText)
    }

    @Test
    fun `ctaText should return connect CTA when no account is selected`() {
        val payload = LinkAccountPickerState.Payload(
            accounts = listOf(partnerAccount1, partnerAccount2),
            accessibleData = accessibleDataCalloutModel,
            businessName = "Business Name",
            consumerSessionClientSecret = "secret",
            stepUpAuthenticationRequired = false
        )

        val pickerState = LinkAccountPickerState(
            payload = Success(payload),
            selectedAccountId = null // No account selected
        )

        assertEquals(R.string.stripe_link_account_picker_cta, pickerState.ctaText)
    }
}
