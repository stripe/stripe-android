package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccountParamsTest {

    @Test
    fun toParamMap_withBusinessData() {
        val company = AccountParams.BusinessTypeParams.Company(name = "Stripe")
        val params = AccountParams.create(
            true,
            company
        )
            .toParamMap()

        val accountData = params["account"] as Map<String, *>?
        assertNotNull(accountData)
        assertEquals(3, accountData.size)
        assertEquals(java.lang.Boolean.TRUE, accountData[AccountParams.PARAM_TOS_SHOWN_AND_ACCEPTED])
        assertEquals(AccountParams.BusinessType.Company.code,
            accountData[AccountParams.PARAM_BUSINESS_TYPE])
        assertEquals(
            company.toParamMap(),
            accountData[AccountParams.BusinessType.Company.code]
        )
    }

    @Test
    fun toParamMap_withNoBusinessData() {
        val params = AccountParams.create(true).toParamMap()

        val accountData = params["account"] as Map<String, *>?
        assertNotNull(accountData)
        assertEquals(1, accountData.size)
        assertEquals(java.lang.Boolean.TRUE, accountData[AccountParams.PARAM_TOS_SHOWN_AND_ACCEPTED])
    }
}
