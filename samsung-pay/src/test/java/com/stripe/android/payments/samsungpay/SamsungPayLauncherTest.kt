package com.stripe.android.payments.samsungpay

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SamsungPayLauncherTest {

    @Test
    fun `Config with default brands includes all four brands`() {
        val config = SamsungPayLauncher.Config(
            environment = SamsungPayEnvironment.Production,
            productId = "test_id",
            merchantName = "Test",
        )

        assertThat(config.allowedCardBrands).containsExactly(
            CardBrand.Visa,
            CardBrand.Mastercard,
            CardBrand.AmericanExpress,
            CardBrand.Discover,
        )
    }

    @Test
    fun `Config builder produces correct config`() {
        val config = SamsungPayLauncher.Config.Builder(
            environment = SamsungPayEnvironment.Test,
            productId = "my_product",
            merchantName = "My Store",
        )
            .setAllowedCardBrands(setOf(CardBrand.Visa, CardBrand.Mastercard))
            .setCardHolderNameEnabled(true)
            .setAddressConfig(AddressConfig(format = AddressConfig.Format.BillingOnly))
            .build()

        assertThat(config.environment).isEqualTo(SamsungPayEnvironment.Test)
        assertThat(config.productId).isEqualTo("my_product")
        assertThat(config.merchantName).isEqualTo("My Store")
        assertThat(config.allowedCardBrands).containsExactly(CardBrand.Visa, CardBrand.Mastercard)
        assertThat(config.cardHolderNameEnabled).isTrue()
        assertThat(config.addressConfig.format).isEqualTo(AddressConfig.Format.BillingOnly)
    }

    @Test
    fun `Config equality`() {
        val config1 = SamsungPayLauncher.Config(
            environment = SamsungPayEnvironment.Production,
            productId = "id",
            merchantName = "name",
        )
        val config2 = SamsungPayLauncher.Config(
            environment = SamsungPayEnvironment.Production,
            productId = "id",
            merchantName = "name",
        )

        assertThat(config1).isEqualTo(config2)
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode())
    }

    @Test
    fun `SamsungPayTokenRequest equality`() {
        val request1 = SamsungPayTokenRequest(
            rawCredential = "{}",
            cryptogram = "abc",
            cryptogramType = "S",
            version = "100",
            cardBrand = "VI",
            last4Dpan = "1234",
            last4Fpan = "5678",
            currencyType = "USD",
        )
        val request2 = SamsungPayTokenRequest(
            rawCredential = "{}",
            cryptogram = "abc",
            cryptogramType = "S",
            version = "100",
            cardBrand = "VI",
            last4Dpan = "1234",
            last4Fpan = "5678",
            currencyType = "USD",
        )

        assertThat(request1).isEqualTo(request2)
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
    }

    @Test
    fun `SamsungPayException equality`() {
        val ex1 = SamsungPayException(errorCode = 1, errorReason = 2, message = "test")
        val ex2 = SamsungPayException(errorCode = 1, errorReason = 2, message = "test")

        assertThat(ex1).isEqualTo(ex2)
        assertThat(ex1.hashCode()).isEqualTo(ex2.hashCode())
    }

    @Test
    fun `static tokenExchangeHandlerHolder starts null`() {
        // Ensure the static holder is null by default
        SamsungPayLauncher.tokenExchangeHandlerHolder = null
        assertThat(SamsungPayLauncher.tokenExchangeHandlerHolder).isNull()
    }
}
