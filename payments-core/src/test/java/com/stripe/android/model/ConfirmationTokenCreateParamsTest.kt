package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ConfirmationTokenCreateParamsTest {

    @Test
    fun createWithPaymentMethodCreateParams_shouldCreateExpectedObject() {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams,
            returnUrl = "https://example.com/return",
            save = true,
            setupFutureUsage = ConfirmationTokenCreateParams.SetupFutureUsage.OnSession,
            receiptEmail = "test@example.com",
            productUsageTokens = setOf("token1", "token2")
        )
        
        assertThat(confirmationTokenCreateParams.paymentMethodType).isEqualTo(PaymentMethod.Type.Card)
        assertThat(confirmationTokenCreateParams.paymentMethodCreateParams).isEqualTo(paymentMethodCreateParams)
        assertThat(confirmationTokenCreateParams.paymentMethodId).isNull()
        assertThat(confirmationTokenCreateParams.returnUrl).isEqualTo("https://example.com/return")
        assertThat(confirmationTokenCreateParams.save).isTrue()
        assertThat(confirmationTokenCreateParams.setupFutureUsage).isEqualTo(ConfirmationTokenCreateParams.SetupFutureUsage.OnSession)
        assertThat(confirmationTokenCreateParams.receiptEmail).isEqualTo("test@example.com")
        assertThat(confirmationTokenCreateParams.productUsageTokens).isEqualTo(setOf("token1", "token2"))
    }

    @Test
    fun createWithPaymentMethodCreateParams_withInvalidCode_shouldThrowException() {
        // Create a PaymentMethodCreateParams with invalid type for testing
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        // Use reflection or create a test case that would cause fromCode to return null
        // For now, let's test with a mock scenario by creating valid params then changing behavior
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        // Create params with invalid override to simulate invalid code scenario
        val invalidParams = PaymentMethodCreateParams(
            code = "invalid_payment_method",
            requiresMandate = false,
            overrideParamMap = mapOf("type" to "invalid_payment_method")
        )
        
        assertFailsWith<IllegalArgumentException> {
            ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = invalidParams
            )
        }
    }

    @Test
    fun createWithPaymentMethodId_shouldCreateExpectedObject() {
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodId(
            paymentMethodId = "pm_1234567890",
            paymentMethodType = PaymentMethod.Type.Card,
            returnUrl = "https://example.com/return",
            save = false,
            setupFutureUsage = ConfirmationTokenCreateParams.SetupFutureUsage.OffSession,
            receiptEmail = "test@example.com",
            productUsageTokens = setOf("token3")
        )
        
        assertThat(confirmationTokenCreateParams.paymentMethodType).isEqualTo(PaymentMethod.Type.Card)
        assertThat(confirmationTokenCreateParams.paymentMethodId).isEqualTo("pm_1234567890")
        assertThat(confirmationTokenCreateParams.paymentMethodCreateParams).isNull()
        assertThat(confirmationTokenCreateParams.returnUrl).isEqualTo("https://example.com/return")
        assertThat(confirmationTokenCreateParams.save).isFalse()
        assertThat(confirmationTokenCreateParams.setupFutureUsage).isEqualTo(ConfirmationTokenCreateParams.SetupFutureUsage.OffSession)
        assertThat(confirmationTokenCreateParams.receiptEmail).isEqualTo("test@example.com")
        assertThat(confirmationTokenCreateParams.productUsageTokens).isEqualTo(setOf("token3"))
    }

    @Test
    fun createCard_shouldCreateExpectedObject() {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 8,
            expiryYear = 2030,
            cvc = "456"
        )
        val billingDetails = PaymentMethod.BillingDetails(
            name = "Jenny Rosen",
            email = "jenny@example.com"
        )
        
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createCard(
            card = cardParams,
            billingDetails = billingDetails,
            returnUrl = "https://example.com/return",
            save = true,
            setupFutureUsage = ConfirmationTokenCreateParams.SetupFutureUsage.OnSession,
            receiptEmail = "jenny@example.com",
            productUsageTokens = setOf("card_token")
        )
        
        assertThat(confirmationTokenCreateParams.paymentMethodType).isEqualTo(PaymentMethod.Type.Card)
        assertThat(confirmationTokenCreateParams.paymentMethodCreateParams).isNotNull()
        assertThat(confirmationTokenCreateParams.paymentMethodCreateParams!!.billingDetails).isEqualTo(billingDetails)
        assertThat(confirmationTokenCreateParams.returnUrl).isEqualTo("https://example.com/return")
        assertThat(confirmationTokenCreateParams.save).isTrue()
        assertThat(confirmationTokenCreateParams.setupFutureUsage).isEqualTo(ConfirmationTokenCreateParams.SetupFutureUsage.OnSession)
        assertThat(confirmationTokenCreateParams.receiptEmail).isEqualTo("jenny@example.com")
        assertThat(confirmationTokenCreateParams.productUsageTokens).isEqualTo(setOf("card_token"))
    }

    @Test
    fun builder_withPaymentMethodCreateParams_shouldCreateExpectedObject() {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4000000000003220",
            expiryMonth = 6,
            expiryYear = 2028,
            cvc = "789"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.Builder()
            .setPaymentMethodCreateParams(paymentMethodCreateParams)
            .setReturnUrl("https://example.com/callback")
            .setSave(true)
            .setSetupFutureUsage(ConfirmationTokenCreateParams.SetupFutureUsage.OffSession)
            .setReceiptEmail("user@example.com")
            .setProductUsageTokens(setOf("builder_token"))
            .build()
        
        assertThat(confirmationTokenCreateParams.paymentMethodType).isEqualTo(PaymentMethod.Type.Card)
        assertThat(confirmationTokenCreateParams.paymentMethodCreateParams).isEqualTo(paymentMethodCreateParams)
        assertThat(confirmationTokenCreateParams.paymentMethodId).isNull()
        assertThat(confirmationTokenCreateParams.returnUrl).isEqualTo("https://example.com/callback")
        assertThat(confirmationTokenCreateParams.save).isTrue()
        assertThat(confirmationTokenCreateParams.setupFutureUsage).isEqualTo(ConfirmationTokenCreateParams.SetupFutureUsage.OffSession)
        assertThat(confirmationTokenCreateParams.receiptEmail).isEqualTo("user@example.com")
        assertThat(confirmationTokenCreateParams.productUsageTokens).isEqualTo(setOf("builder_token"))
    }

    @Test
    fun builder_withPaymentMethodId_shouldCreateExpectedObject() {
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.Builder()
            .setPaymentMethodId("pm_abcdef123456", PaymentMethod.Type.USBankAccount)
            .setReturnUrl("https://example.com/success")
            .setSave(false)
            .setSetupFutureUsage(ConfirmationTokenCreateParams.SetupFutureUsage.OnSession)
            .setReceiptEmail("customer@example.com")
            .setProductUsageTokens(setOf("us_bank_token"))
            .build()
        
        assertThat(confirmationTokenCreateParams.paymentMethodType).isEqualTo(PaymentMethod.Type.USBankAccount)
        assertThat(confirmationTokenCreateParams.paymentMethodId).isEqualTo("pm_abcdef123456")
        assertThat(confirmationTokenCreateParams.paymentMethodCreateParams).isNull()
        assertThat(confirmationTokenCreateParams.returnUrl).isEqualTo("https://example.com/success")
        assertThat(confirmationTokenCreateParams.save).isFalse()
        assertThat(confirmationTokenCreateParams.setupFutureUsage).isEqualTo(ConfirmationTokenCreateParams.SetupFutureUsage.OnSession)
        assertThat(confirmationTokenCreateParams.receiptEmail).isEqualTo("customer@example.com")
        assertThat(confirmationTokenCreateParams.productUsageTokens).isEqualTo(setOf("us_bank_token"))
    }

    @Test
    fun builder_setPaymentMethodCreateParams_shouldClearPaymentMethodId() {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "5555555555554444",
            expiryMonth = 3,
            expiryYear = 2027,
            cvc = "321"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        val builder = ConfirmationTokenCreateParams.Builder()
            .setPaymentMethodId("pm_initial_id", PaymentMethod.Type.Card)
            .setPaymentMethodCreateParams(paymentMethodCreateParams)
        
        val confirmationTokenCreateParams = builder.build()
        
        assertThat(confirmationTokenCreateParams.paymentMethodId).isNull()
        assertThat(confirmationTokenCreateParams.paymentMethodCreateParams).isEqualTo(paymentMethodCreateParams)
        assertThat(confirmationTokenCreateParams.paymentMethodType).isEqualTo(PaymentMethod.Type.Card)
    }

    @Test
    fun builder_setPaymentMethodId_shouldClearPaymentMethodCreateParams() {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4000056655665556",
            expiryMonth = 9,
            expiryYear = 2029,
            cvc = "654"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        val builder = ConfirmationTokenCreateParams.Builder()
            .setPaymentMethodCreateParams(paymentMethodCreateParams)
            .setPaymentMethodId("pm_new_id", PaymentMethod.Type.SepaDebit)
        
        val confirmationTokenCreateParams = builder.build()
        
        assertThat(confirmationTokenCreateParams.paymentMethodCreateParams).isNull()
        assertThat(confirmationTokenCreateParams.paymentMethodId).isEqualTo("pm_new_id")
        assertThat(confirmationTokenCreateParams.paymentMethodType).isEqualTo(PaymentMethod.Type.SepaDebit)
    }

    @Test
    fun builder_withMissingPaymentMethodType_shouldThrowException() {
        val builder = ConfirmationTokenCreateParams.Builder()
        
        assertFailsWith<IllegalArgumentException> {
            builder.build()
        }
    }

    @Test
    fun builder_withNeitherPaymentMethodCreateParamsNorId_shouldThrowException() {
        val builder = ConfirmationTokenCreateParams.Builder()
            .setPaymentMethodType(PaymentMethod.Type.Card)
        
        assertFailsWith<IllegalArgumentException> {
            builder.build()
        }
    }

    @Test
    fun builder_withShippingInformation_shouldCreateExpectedObject() {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        val shipping = ShippingInformation(
            name = "John Doe",
            address = Address(
                line1 = "123 Main St",
                city = "New York",
                state = "NY",
                postalCode = "10001",
                country = "US"
            ),
            phone = "+1234567890"
        )
        
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.Builder()
            .setPaymentMethodCreateParams(paymentMethodCreateParams)
            .setShipping(shipping)
            .build()
        
        assertThat(confirmationTokenCreateParams.shipping).isEqualTo(shipping)
    }

    @Test
    fun builder_withMandateData_shouldCreateExpectedObject() {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        val mandateData = MandateDataParams(
            MandateDataParams.Type.Online(
                ipAddress = "192.168.1.1",
                userAgent = "TestUserAgent/1.0"
            )
        )
        
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.Builder()
            .setPaymentMethodCreateParams(paymentMethodCreateParams)
            .setMandateData(mandateData)
            .build()
        
        assertThat(confirmationTokenCreateParams.mandateData).isEqualTo(mandateData)
    }

    @Test
    fun toParamMap_withPaymentMethodCreateParams_shouldCreateExpectedMap() {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams,
            returnUrl = "https://example.com/return",
            save = true,
            setupFutureUsage = ConfirmationTokenCreateParams.SetupFutureUsage.OnSession,
            receiptEmail = "test@example.com"
        )
        
        val paramMap = confirmationTokenCreateParams.toParamMap()
        
        // payment_method_type is not supported by ConfirmationToken API
        assertThat(paramMap["payment_method_type"]).isEqualTo("card")
        assertThat(paramMap["payment_method_data"]).isEqualTo(paymentMethodCreateParams.toParamMap())
        assertThat(paramMap["return_url"]).isEqualTo("https://example.com/return")
        // 'save' parameter is not supported by ConfirmationToken API
        assertThat(paramMap.containsKey("save")).isFalse()
        assertThat(paramMap["setup_future_usage"]).isEqualTo("on_session")
        assertThat(paramMap["receipt_email"]).isEqualTo("test@example.com")
        assertThat(paramMap.containsKey("payment_method")).isFalse()
    }

    @Test
    fun toParamMap_withPaymentMethodId_shouldCreateExpectedMap() {
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodId(
            paymentMethodId = "pm_1234567890",
            paymentMethodType = PaymentMethod.Type.Card,
            returnUrl = "https://example.com/return",
            save = false,
            setupFutureUsage = ConfirmationTokenCreateParams.SetupFutureUsage.OffSession,
            receiptEmail = "test@example.com"
        )
        
        val paramMap = confirmationTokenCreateParams.toParamMap()
        
        // payment_method_type is not supported by ConfirmationToken API
        assertThat(paramMap["payment_method_type"]).isEqualTo("card")
        assertThat(paramMap["payment_method"]).isEqualTo("pm_1234567890")
        assertThat(paramMap["return_url"]).isEqualTo("https://example.com/return")
        // 'save' parameter is not supported by ConfirmationToken API 
        assertThat(paramMap.containsKey("save")).isFalse()
        assertThat(paramMap["setup_future_usage"]).isEqualTo("off_session")
        assertThat(paramMap["receipt_email"]).isEqualTo("test@example.com")
        assertThat(paramMap.containsKey("payment_method_data")).isFalse()
    }

    @Test
    fun toParamMap_withNullOptionalFields_shouldOnlyIncludeRequiredFields() {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams
        )
        
        val paramMap = confirmationTokenCreateParams.toParamMap()
        
        // payment_method_type is not supported by ConfirmationToken API
        assertThat(paramMap["payment_method_type"]).isEqualTo("card")
        assertThat(paramMap["payment_method_data"]).isEqualTo(paymentMethodCreateParams.toParamMap())
        assertThat(paramMap.containsKey("return_url")).isFalse()
        assertThat(paramMap.containsKey("save")).isFalse()
        assertThat(paramMap.containsKey("setup_future_usage")).isFalse()
        assertThat(paramMap.containsKey("receipt_email")).isFalse()
        assertThat(paramMap.containsKey("shipping")).isFalse()
        assertThat(paramMap.containsKey("mandate_data")).isFalse()
    }

    @Test
    fun toParamMap_withShippingAndMandateData_shouldIncludeAllFields() {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        val shipping = ShippingInformation(
            name = "Jane Doe",
            address = Address(
                line1 = "456 Oak Ave",
                city = "Los Angeles",
                state = "CA",
                postalCode = "90210",
                country = "US"
            )
        )
        
        val mandateData = MandateDataParams(
            MandateDataParams.Type.Online(
                ipAddress = "10.0.0.1",
                userAgent = "TestBrowser/2.0"
            )
        )
        
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams,
            shipping = shipping,
            mandateData = mandateData
        )
        
        val paramMap = confirmationTokenCreateParams.toParamMap()
        
        assertThat(paramMap["shipping"]).isEqualTo(shipping.toParamMap())
        assertThat(paramMap["mandate_data"]).isEqualTo(mandateData.toParamMap())
    }

    @Test
    fun setupFutureUsageEnum_shouldHaveCorrectCodes() {
        assertThat(ConfirmationTokenCreateParams.SetupFutureUsage.OnSession.code).isEqualTo("on_session")
        assertThat(ConfirmationTokenCreateParams.SetupFutureUsage.OffSession.code).isEqualTo("off_session")
    }
}