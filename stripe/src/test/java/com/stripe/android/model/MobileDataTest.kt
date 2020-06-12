package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.StripeIntent.NextActionData.RedirectToUrl.MobileData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MobileDataTest {
    @Test
    fun `Alipay data should parse return_url correctly`() {
        val data = MobileData.Alipay("_input_charset=utf-8&app_pay=Y" +
            "&currency=USD&forex_biz=FP" +
            "&notify_url=https%3A%2F%2Fhooks.stripe.com%2Falipay%2Falipay%2Fhook%2F6255d30b067c8f7a162c79c654483646%2Fsrc_1Gt188KlwPmebFhp4SWhZwn1" +
            "&out_trade_no=src_1Gt188KlwPmebFhp4SWhZwn1" +
            "&partner=2088621828244481&payment_type=1" +
            "&product_code=NEW_WAP_OVERSEAS_SELLER" +
            "&return_url=https%3A%2F%2Fhooks.stripe.com%2Fadapter%2Falipay%2Fredirect%2Fcomplete%2Fsrc_1Gt188KlwPmebFhp4SWhZwn1%2Fsrc_client_secret_RMaQKPfAmHOdUwcNhXEjolR4" +
            "&secondary_merchant_id=acct_1EqOyCKlwPmebFhp" +
            "&secondary_merchant_industry=5734" +
            "&secondary_merchant_name=Yuki-Test" +
            "&sendFormat=normal" +
            "&service=create_forex_trade_wap" +
            "&sign=44e797e6d5ba1c784d3cceb176c359db" +
            "&sign_type=MD5" +
            "&subject=Yuki-Test" +
            "&supplier=Yuki-Test" +
            "&timeout_rule=20m" +
            "&total_fee=1.00")
        assertThat(data.authCompleteUrl).isEqualTo(
            "https://hooks.stripe.com/adapter/alipay/redirect/complete/src_1Gt188KlwPmebFhp4SWhZwn1/src_client_secret_RMaQKPfAmHOdUwcNhXEjolR4"
        )
    }

    @Test
    fun `Alipay data should handle missing data`() {
        val data = MobileData.Alipay("")
        assertThat(data.authCompleteUrl).isNull()
    }

    @Test
    fun `Alipay data should ignore non-stripe urls`() {
        val data = MobileData.Alipay("return_url=https://google.com")
        assertThat(data.authCompleteUrl).isNull()
    }
}
