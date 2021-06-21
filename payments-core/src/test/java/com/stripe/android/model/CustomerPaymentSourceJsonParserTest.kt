package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.parsers.CustomerPaymentSourceJsonParser
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Test class for [CustomerPaymentSource] model class.
 */
class CustomerPaymentSourceJsonParserTest {

    @Test
    fun parse_whenCard_createsCustomerSourceData() {
        val customerCard = assertIs<CustomerCard>(parse(CardFixtures.CARD_USD_JSON))
        assertThat(customerCard.id)
            .isEqualTo("card_189fi32eZvKYlo2CHK8NPRME")
        assertThat(customerCard.tokenizationMethod)
            .isNull()
    }

    @Test
    fun parse_whenCardWithTokenization_createsSourceDataWithTokenization() {
        val customerCard = assertIs<CustomerCard>(parse(SourceFixtures.APPLE_PAY))
        assertThat(customerCard.id)
            .isEqualTo("card_189fi32eZvKYlo2CHK8NPRME")
        assertThat(customerCard.tokenizationMethod)
            .isEqualTo(TokenizationMethod.ApplePay)
    }

    @Test
    fun parse_whenSource_createsCustomerSourceData() {
        val customerSource = assertIs<CustomerSource>(
            parse(SourceFixtures.CUSTOMER_SOURCE_CARD_JSON)
        )
        assertThat(customerSource.id)
            .isEqualTo("src_19t3xKBZqEXluyI4uz2dxAfQ")
    }

    @Test
    fun getSourceType_whenCustomerSource_returnsSourceType() {
        val customerSource = assertIs<CustomerSource>(parse(SourceFixtures.ALIPAY_JSON))
        assertThat(customerSource.source.type)
            .isEqualTo(Source.SourceType.ALIPAY)
    }

    private companion object {
        private fun parse(jsonObject: JSONObject): CustomerPaymentSource {
            return requireNotNull(
                CustomerPaymentSourceJsonParser().parse(jsonObject)
            )
        }
    }
}
