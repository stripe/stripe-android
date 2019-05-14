package com.stripe.android.model;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AccountParamsTest {

    @Test
    public void toParamMap_withBusinessData() {
        final Map<String, Object> businessData = new HashMap<>();
        businessData.put("name", "Stripe");
        final Map<String, Object> params =
                AccountParams.createAccountParams(true,
                        AccountParams.BusinessType.Company, businessData)
                        .toParamMap();

        final Map<String, ?> accountData = (Map<String, ?>) params.get("account");
        assertNotNull(accountData);
        assertEquals(3, accountData.size());
        assertEquals(Boolean.TRUE, accountData.get(AccountParams.API_TOS_SHOWN_AND_ACCEPTED));
        assertEquals(AccountParams.BusinessType.Company.code,
                accountData.get(AccountParams.API_BUSINESS_TYPE));
        assertEquals(businessData,
                accountData.get(AccountParams.BusinessType.Company.code));
    }

    @Test
    public void toParamMap_withNoBusinessData() {
        final Map<String, Object> params =
                AccountParams.createAccountParams(true, null, null)
                        .toParamMap();

        final Map<String, ?> accountData = (Map<String, ?>) params.get("account");
        assertNotNull(accountData);
        assertEquals(1, accountData.size());
        assertEquals(Boolean.TRUE, accountData.get(AccountParams.API_TOS_SHOWN_AND_ACCEPTED));
    }
}
