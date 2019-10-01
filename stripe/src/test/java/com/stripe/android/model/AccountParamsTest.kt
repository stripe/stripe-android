package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccountParamsTest {

    @Test
    fun toParamMap_withBusinessData() {
        val businessData = mapOf("name" to "Stripe")
        val params = AccountParams
            .createAccountParams(
                true,
                AccountParams.BusinessType.Company,
                businessData
            )
            .toParamMap()

        val accountData = params["account"] as Map<String, *>?
        assertNotNull(accountData)
        assertEquals(3, accountData.size)
        assertEquals(java.lang.Boolean.TRUE, accountData[AccountParams.API_TOS_SHOWN_AND_ACCEPTED])
        assertEquals(AccountParams.BusinessType.Company.code,
            accountData[AccountParams.API_BUSINESS_TYPE])
        assertEquals(businessData,
            accountData[AccountParams.BusinessType.Company.code])
    }

    @Test
    fun toParamMap_withNoBusinessData() {
        val params =
            AccountParams.createAccountParams(true, null, null)
                .toParamMap()

        val accountData = params["account"] as Map<String, *>?
        assertNotNull(accountData)
        assertEquals(1, accountData.size)
        assertEquals(java.lang.Boolean.TRUE, accountData[AccountParams.API_TOS_SHOWN_AND_ACCEPTED])
    }
}
