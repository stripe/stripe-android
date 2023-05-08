package com.stripe.android.financialconnections.features.linkaccountpicker

import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkAccountPickerStateTest {

    private val partnerAccount1 = partnerAccount().copy(id = "acct1", name = "Account 1")
    private val partnerAccount2 = partnerAccount().copy(id = "acct2", name = "Account 2")

    private val networkedAccount1 = LinkAccountPickerState.NetworkedAccount(partnerAccount1, true)
    private val networkedAccount2 = LinkAccountPickerState.NetworkedAccount(partnerAccount2, false)

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
    fun `selectedAccountStatus should return repairable when account is repairable`() {
        val payload = LinkAccountPickerState.Payload(
            accounts = listOf(networkedAccount1, networkedAccount2),
            accessibleData = accessibleDataCalloutModel,
            businessName = "Business Name",
            consumerSessionClientSecret = "secret",
            repairAuthorizationEnabled = true,
            stepUpAuthenticationRequired = false
        )

        val pickerState = LinkAccountPickerState(
            payload = Success(payload),
            selectedAccountId = partnerAccount1.id
        )

        assertEquals(R.string.stripe_link_account_picker_repair_cta, pickerState.ctaText)
    }

    @Test
    fun `selectedAccountStatus should return connectable when account is not repairable`() {
        val payload = LinkAccountPickerState.Payload(
            accounts = listOf(networkedAccount1, networkedAccount2),
            accessibleData = accessibleDataCalloutModel,
            businessName = "Business Name",
            consumerSessionClientSecret = "secret",
            repairAuthorizationEnabled = true,
            stepUpAuthenticationRequired = false
        )

        val pickerState = LinkAccountPickerState(
            payload = Success(payload),
            selectedAccountId = partnerAccount2.id
        )

        assertEquals(R.string.stripe_link_account_picker_cta, pickerState.ctaText)
    }

    @Test
    fun `selectedAccountStatus should return connectable when no account is selected`() {
        val payload = LinkAccountPickerState.Payload(
            accounts = listOf(networkedAccount1, networkedAccount2),
            accessibleData = accessibleDataCalloutModel,
            businessName = "Business Name",
            consumerSessionClientSecret = "secret",
            repairAuthorizationEnabled = true,
            stepUpAuthenticationRequired = false
        )

        val pickerState = LinkAccountPickerState(
            payload = Success(payload),
            selectedAccountId = null // No account selected
        )

        assertEquals(R.string.stripe_link_account_picker_cta, pickerState.ctaText)
    }
}
