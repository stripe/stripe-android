package com.stripe.android.model.parsers

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.MicrodepositType
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentIntentFixtures.BOLETO_REQUIRES_ACTION
import com.stripe.android.model.PaymentIntentFixtures.PAY_NOW_REQUIRES_ACTION
import com.stripe.android.model.StripeIntent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentIntentJsonParserTest {
    @Test
    fun parse_withExpandedPaymentMethod_shouldCreateExpectedObject() {
        val paymentIntent = PaymentIntentJsonParser().parse(
            PaymentIntentFixtures.EXPANDED_PAYMENT_METHOD_JSON
        )
        assertThat(paymentIntent?.paymentMethodId)
            .isEqualTo("pm_1GSTxOCRMbs6FrXfYCosDqyr")
        assertThat(paymentIntent?.paymentMethod?.id)
            .isEqualTo("pm_1GSTxOCRMbs6FrXfYCosDqyr")
    }

    @Test
    fun parse_withShipping_shouldCreateExpectedObject() {
        val paymentIntent = PaymentIntentJsonParser().parse(
            PaymentIntentFixtures.PI_WITH_SHIPPING_JSON
        )

        assertThat(paymentIntent?.shipping)
            .isEqualTo(
                PaymentIntent.Shipping(
                    address = Address(
                        line1 = "123 Market St",
                        line2 = "#345",
                        city = "San Francisco",
                        state = "CA",
                        postalCode = "94107",
                        country = "US"
                    ),
                    carrier = "UPS",
                    name = "Jenny Rosen",
                    phone = "1-800-555-1234",
                    trackingNumber = "12345"
                )
            )
    }

    @Test
    fun parse_withOxxo_shouldCreateExpectedNextActionData() {
        val paymentIntent = requireNotNull(
            PaymentIntentJsonParser().parse(
                PaymentIntentFixtures.OXXO_REQUIRES_ACTION_JSON
            )
        )
        assertThat(paymentIntent.nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.DisplayOxxoDetails(
                    expiresAfter = 1617944399,
                    number = "12345678901234657890123456789012",
                    hostedVoucherUrl = "https://payments.stripe.com/oxxo/voucher/test_YWNjdF8xSWN1c1VMMzJLbFJvdDAxLF9KRlBtckVBMERWM0lBZEUyb"
                )
            )
    }

    @Test
    fun parse_withBoleto_shouldCreateExpectedNextActionData() {
        val paymentIntent = BOLETO_REQUIRES_ACTION
        assertThat(paymentIntent.nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.DisplayBoletoDetails(
                    hostedVoucherUrl = "https://payments.stripe.com/boleto/voucher/test_YWNjdF8xTm5pZllBQVlObzc4dXh0LF9PYk81bUhVTGNSZGNIeHlyckJ4djBFQ3lkNkswS1lt0100qH3SxPW7"
                )
            )
    }

    @Test
    fun parse_withPayNow_shouldCreateExpectedNextActionData() {
        val paymentIntent = PAY_NOW_REQUIRES_ACTION
        assertThat(paymentIntent.nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.DisplayPayNowDetails(
                    hostedVoucherUrl = "https://payments.stripe.com/promptpay/instructions/testdata"
                )
            )
    }

    @Test
    fun parse_withRedirectAction_shouldCreateExpectedNextActionData() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_REDIRECT
        assertThat(paymentIntent.nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.RedirectToUrl(
                    Uri.parse("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv"),
                    returnUrl = "stripe://deeplink"
                )
            )
    }

    @Test
    fun parse_with3ds1Action_shouldCreateExpectedNextActionData() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1
        assertThat(paymentIntent.nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.SdkData.Use3DS1(
                    "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW"
                )
            )
    }

    @Test
    fun parse_with3ds2Action_shouldCreateExpectedNextActionData() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
        assertThat(paymentIntent.nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.SdkData.Use3DS2(
                    "src_1ExkUeAWhjPjYwPiLWUvXrSA",
                    "mastercard",
                    "34b16ea1-1206-4ee8-84d2-d292bc73c2ae",
                    StripeIntent.NextActionData.SdkData.Use3DS2.DirectoryServerEncryption(
                        "A000000004",
                        "-----BEGIN CERTIFICATE-----\nMIIFtTCCA52gAwIBAgIQJqSRaPua/6cpablmVDHWUDANBgkqhkiG9w0BAQsFADB6\nMQswCQYDVQQGEwJVUzETMBEGA1UEChMKTWFzdGVyQ2FyZDEoMCYGA1UECxMfTWFz\ndGVyQ2FyZCBJZGVudGl0eSBDaGVjayBHZW4gMzEsMCoGA1UEAxMjUFJEIE1hc3Rl\nckNhcmQgM0RTMiBBY3F1aXJlciBTdWIgQ0EwHhcNMTgxMTIwMTQ1MzIzWhcNMjEx\nMTIwMTQ1MzIzWjBxMQswCQYDVQQGEwJVUzEdMBsGA1UEChMUTWFzdGVyQ2FyZCBX\nb3JsZHdpZGUxGzAZBgNVBAsTEmdhdGV3YXktZW5jcnlwdGlvbjEmMCQGA1UEAxMd\nM2RzMi5kaXJlY3RvcnkubWFzdGVyY2FyZC5jb20wggEiMA0GCSqGSIb3DQEBAQUA\nA4IBDwAwggEKAoIBAQCFlZjqbbL9bDKOzZFawdbyfQcezVEUSDCWWsYKw/V6co9A\nGaPBUsGgzxF6+EDgVj3vYytgSl8xFvVPsb4ZJ6BJGvimda8QiIyrX7WUxQMB3hyS\nBOPf4OB72CP+UkaFNR6hdlO5ofzTmB2oj1FdLGZmTN/sj6ZoHkn2Zzums8QAHFjv\nFjspKUYCmms91gpNpJPUUztn0N1YMWVFpFMytahHIlpiGqTDt4314F7sFABLxzFr\nDmcqhf623SPV3kwQiLVWOvewO62ItYUFgHwle2dq76YiKrUv1C7vADSk2Am4gqwv\n7dcCnFeM2AHbBFBa1ZBRQXosuXVw8ZcQqfY8m4iNAgMBAAGjggE+MIIBOjAOBgNV\nHQ8BAf8EBAMCAygwCQYDVR0TBAIwADAfBgNVHSMEGDAWgBSakqJUx4CN/s5W4wMU\n/17uSLhFuzBIBggrBgEFBQcBAQQ8MDowOAYIKwYBBQUHMAGGLGh0dHA6Ly9vY3Nw\nLnBraS5pZGVudGl0eWNoZWNrLm1hc3RlcmNhcmQuY29tMCgGA1UdEQQhMB+CHTNk\nczIuZGlyZWN0b3J5Lm1hc3RlcmNhcmQuY29tMGkGA1UdHwRiMGAwXqBcoFqGWGh0\ndHA6Ly9jcmwucGtpLmlkZW50aXR5Y2hlY2subWFzdGVyY2FyZC5jb20vOWE5MmEy\nNTRjNzgwOGRmZWNlNTZlMzAzMTRmZjVlZWU0OGI4NDViYi5jcmwwHQYDVR0OBBYE\nFHxN6+P0r3+dFWmi/+pDQ8JWaCbuMA0GCSqGSIb3DQEBCwUAA4ICAQAtwW8siyCi\nmhon1WUAUmufZ7bbegf3cTOafQh77NvA0xgVeloELUNCwsSSZgcOIa4Zgpsa0xi5\nfYxXsPLgVPLM0mBhTOD1DnPu1AAm32QVelHe6oB98XxbkQlHGXeOLs62PLtDZd94\n7pm08QMVb+MoCnHLaBLV6eKhKK+SNrfcxr33m0h3v2EMoiJ6zCvp8HgIHEhVpleU\n8H2Uo5YObatb/KUHgtp2z0vEfyGhZR7hrr48vUQpfVGBABsCV0aqUkPxtAXWfQo9\n1N9B7H3EIcSjbiUz5vkj9YeDSyJIi0Y/IZbzuNMsz2cRi1CWLl37w2fe128qWxYq\nY/k+Y4HX7uYchB8xPaZR4JczCvg1FV2JrkOcFvElVXWSMpBbe2PS6OMr3XxrHjzp\nDyM9qvzge0Ai9+rq8AyGoG1dP2Ay83Ndlgi42X3yl1uEUW2feGojCQQCFFArazEj\nLUkSlrB2kA12SWAhsqqQwnBLGSTp7PqPZeWkluQVXS0sbj0878kTra6TjG3U+KqO\nJCj8v6G380qIkAXe1xMHHNQ6GS59HZMeBPYkK2y5hmh/JVo4bRfK7Ya3blBSBfB8\nAVWQ5GqVWklvXZsQLN7FH/fMIT3y8iE1W19Ua4whlhvn7o/aYWOkHr1G2xyh8BHj\n7H63A2hjcPlW/ZAJSTuBZUClAhsNohH2Jg==\n-----END CERTIFICATE-----\n",
                        listOf(
                            "-----BEGIN CERTIFICATE-----\nMIIFxzCCA6+gAwIBAgIQFsjyIuqhw80wNMjXU47lfjANBgkqhkiG9w0BAQsFADB8\nMQswCQYDVQQGEwJVUzETMBEGA1UEChMKTWFzdGVyQ2FyZDEoMCYGA1UECxMfTWFz\ndGVyQ2FyZCBJZGVudGl0eSBDaGVjayBHZW4gMzEuMCwGA1UEAxMlUFJEIE1hc3Rl\nckNhcmQgSWRlbnRpdHkgQ2hlY2sgUm9vdCBDQTAeFw0xNjA3MTQwNzI0MDBaFw0z\nMDA3MTUwODEwMDBaMHwxCzAJBgNVBAYTAlVTMRMwEQYDVQQKEwpNYXN0ZXJDYXJk\nMSgwJgYDVQQLEx9NYXN0ZXJDYXJkIElkZW50aXR5IENoZWNrIEdlbiAzMS4wLAYD\nVQQDEyVQUkQgTWFzdGVyQ2FyZCBJZGVudGl0eSBDaGVjayBSb290IENBMIICIjAN\nBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAxZF3nCEiT8XFFaq+3BPT0cMDlWE7\n6IBsdx27w3hLxwVLog42UTasIgzmysTKpBc17HEZyNAqk9GrCHo0Oyk4JZuXHoW8\n0goZaR2sMnn49ytt7aGsE1PsfVup8gqAorfm3IFab2/CniJJNXaWPgn94+U/nsoa\nqTQ6j+6JBoIwnFklhbXHfKrqlkUZJCYaWbZRiQ7nkANYYM2Td3N87FmRanmDXj5B\nG6lc9o1clTC7UvRQmNIL9OdDDZ8qlqY2Fi0eztBnuo2DUS5tGdVy8SgqPM3E12ft\nk4EdlKyrWmBqFcYwGx4AcSJ88O3rQmRBMxtk0r5vhgr6hDCGq7FHK/hQFP9LhUO9\n1qxWEtMn76Sa7DPCLas+tfNRVwG12FBuEZFhdS/qKMdIYUE5Q6uwGTEvTzg2kmgJ\nT3sNa6dbhlYnYn9iIjTh0dPGgiXap1Bhi8B9aaPFcHEHSqW8nZUINcrwf5AUi+7D\n+q/AG5ItiBtQTCaaFm74gv51yutzwgKnH9Q+x3mtuK/uwlLCslj9DeXgOzMWFxFg\nuuwLGX39ktDnetxNw3PLabjHkDlGDIfx0MCQakM74sTcuW8ICiHvNA7fxXCnbtjs\ny7at/yXYwAd+IDS51MA/g3OYVN4M+0pG843Re6Z53oODp0Ymugx0FNO1NxT3HO1h\nd7dXyjAV/tN/GGcCAwEAAaNFMEMwDgYDVR0PAQH/BAQDAgGGMBIGA1UdEwEB/wQI\nMAYBAf8CAQEwHQYDVR0OBBYEFNSlUaqS2hGLFMT/EXrhHeEx+UqxMA0GCSqGSIb3\nDQEBCwUAA4ICAQBLqIYorrtVz56F6WOoLX9CcRjSFim7gO873a3p7+62I6joXMsM\nr0nd9nRPcEwduEloZXwFgErVUQWaUZWNpue0mGvU7BUAgV9Tu0J0yA+9srizVoMv\nx+o4zTJ3Vu5p5aTf1aYoH1xYVo5ooFgl/hI/EXD2lo/xOUfPKXBY7twfiqOziQmT\nGBuqPRq8h3dQRlXYxX/rzGf80SecIT6wo9KavDkjOmJWGzzHsn6Ryo6MEClMaPn0\nte87ukNN740AdPhTvNeZdWlwyqWAJpsv24caEckjSpgpoIZOjc7PAcEVQOWFSxUe\nsMk4Jz5bVZa/ABjzcp+rsq1QLSJ5quqHwWFTewChwpw5gpw+E5SpKY6FIHPlTdl+\nqHThvN8lsKNAQg0qTdEbIFZCUQC0Cl3Ti3q/cXv8tguLJNWvdGzB600Y32QHclMp\neyabT4/QeOesqpx6Da70J2KvLT1j6Ch2BsKSzeVLahrjnoPrdgiIYYBOgeA3T8SE\n1pgagt56R7nIkRQbtesoRKi+NfC7pPb/G1VUsj/cREAHH1i1UKa0aCsIiANfEdQN\n5Ok6wtFJJhp3apAvnVkrZDfOG5we9bYzvGoI7SUnleURBJ+N3ihjARfL4hDeeRHh\nYyLkM3kEyEkrJBL5r0GDjicxM+aFcR2fCBAkv3grT5kz4kLcvsmHX+9DBw==\n-----END CERTIFICATE-----\n\n"
                        ),
                        "7c4debe3f4af7f9d1569a2ffea4343c2566826ee"
                    ),
                    "pi_1ExkUeAWhjPjYwPiLWUvXrSA",
                    "pk_test_nextActionData"
                )
            )
    }

    @Test
    fun parse_withAlipayAction_shoulddCreateExpectedNextActionData() {
        val paymentIntent = PaymentIntentJsonParser().parse(
            PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION_JSON
        )
        assertThat(paymentIntent?.nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.AlipayRedirect(
                    "_input_charset=utf-8&app_pay=Y&currency=USD&forex_biz=FP&notify_url=https%3A%2F%2Fhooks.stripe.com%2Falipay%2Falipay%2Fhook%2F6255d30b067c8f7a162c79c654483646%2Fsrc_1HDEFWKlwPmebFhp6tcpln8T&out_trade_no=src_1HDEFWKlwPmebFhp6tcpln8T&partner=2088621828244481&payment_type=1&product_code=NEW_WAP_OVERSEAS_SELLER&return_url=https%3A%2F%2Fhooks.stripe.com%2Fadapter%2Falipay%2Fredirect%2Fcomplete%2Fsrc_1HDEFWKlwPmebFhp6tcpln8T%2Fsrc_client_secret_S6H9mVMKK6qxk9YxsUvbH55K&secondary_merchant_id=acct_1EqOyCKlwPmebFhp&secondary_merchant_industry=5734&secondary_merchant_name=Yuki-Test&sendFormat=normal&service=create_forex_trade_wap&sign=b691876a7f0bd889530f54a271d314d5&sign_type=MD5&subject=Yuki-Test&supplier=Yuki-Test&timeout_rule=20m&total_fee=1.00",
                    "https://hooks.stripe.com/redirect/authenticate/src_1HDEFWKlwPmebFhp6tcpln8T?client_secret=src_client_secret_S6H9mVMKK6qxk9YxsUvbH55K",
                    "example://return_url"
                )
            )
    }

    @Test
    fun parse_withVerifyWithMicrodepositsAction_shoulddCreateExpectedNextActionData() {
        val paymentIntent = PaymentIntentJsonParser().parse(
            PaymentIntentFixtures.PI_WITH_US_BANK_ACCOUNT_IN_PAYMENT_METHODS_JSON
        )
        assertThat(paymentIntent?.nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.VerifyWithMicrodeposits(
                    1647241200,
                    "https://payments.stripe.com/microdeposit/pacs_test_YWNjdF8xS2J1SjlGbmt1bWlGVUZ4LHBhX25vbmNlX0xJcFVEaERaU0JOVVR3akhxMXc5eklOQkl3UTlwNWo0000v3GS1Jej",
                    MicrodepositType.AMOUNTS
                )
            )
    }

    @Test
    fun parse_withLinkFundingSources_shouldCreateExpectedObject() {
        val paymentIntent = PaymentIntentFixtures.PI_WITH_LINK_FUNDING_SOURCES
        assertThat(paymentIntent.linkFundingSources).containsExactly("card", "bank_account")
    }

    @Test
    fun parse_withCountryCode_shouldCreateExpectedObject() {
        val paymentIntent = PaymentIntentFixtures.PI_WITH_COUNTRY_CODE
        assertThat(paymentIntent.countryCode).isEqualTo("US")
    }
}
