package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.BankAccount
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerBankAccount
import org.json.JSONObject
import kotlin.test.Test

class CustomerJsonParserTest {

    @Test
    fun `should correctly parse Customer with BankAccount source`() {
        assertThat(
            CustomerJsonParser()
                .parse(CUSTOMER_JSON)
        ).isEqualTo(
            Customer(
                id = "cus_HcLIwF3BCi",
                defaultSource = "ba_1H3NOMCRMbs6FrXfahj",
                shippingInformation = null,
                sources = listOf(
                    CustomerBankAccount(
                        BankAccount(
                            id = "ba_1H3NOMCRMbs6FrXfahj",
                            accountHolderName = "Test Bank Account",
                            accountHolderType = BankAccount.Type.Individual,
                            bankName = "STRIPE TEST BANK",
                            countryCode = "US",
                            currency = "usd",
                            fingerprint = "wxXSAD5idPUzgBEz",
                            last4 = "6789",
                            routingNumber = "110000000",
                            status = BankAccount.Status.New
                        )
                    )
                ),
                hasMore = false,
                totalCount = 1,
                url = "/v1/customers/cus_HcLIwF3BCi/sources",
                description = "mobile SDK example customer",
                email = "jenny.rosen@example.com",
                liveMode = true
            )
        )
    }

    private companion object {
        private val CUSTOMER_JSON = JSONObject(
            """
            {
            	"id": "cus_HcLIwF3BCi",
            	"object": "customer",
            	"created": 1594327465,
            	"default_source": "ba_1H3NOMCRMbs6FrXfahj",
            	"description": "mobile SDK example customer",
            	"email": "jenny.rosen@example.com",
            	"livemode": true,
            	"shipping": null,
            	"sources": {
            		"object": "list",
            		"data": [{
            			"id": "ba_1H3NOMCRMbs6FrXfahj",
            			"object": "bank_account",
            			"account_holder_name": "Test Bank Account",
            			"account_holder_type": "individual",
            			"bank_name": "STRIPE TEST BANK",
            			"country": "US",
            			"currency": "usd",
            			"customer": "cus_HcLIwF3BCiRp0t",
            			"fingerprint": "wxXSAD5idPUzgBEz",
            			"last4": "6789",
            			"metadata": {},
            			"routing_number": "110000000",
            			"status": "new"
            		}],
            		"has_more": false,
            		"total_count": 1,
            		"url": "\/v1\/customers\/cus_HcLIwF3BCi\/sources"
            	}
            }
            """.trimIndent()
        )
    }
}
