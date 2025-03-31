import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.SignUpParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale
import kotlin.collections.containsKey
import kotlin.text.get

class SignUpParamsTest {

    @Test
    fun `toParamMap should include all non-null fields`() {
        val locale = Locale.US
        val incentiveEligibilitySession = IncentiveEligibilitySession.PaymentIntent("incentive123")

        val signUpParams = SignUpParams(
            email = "test@example.com",
            phoneNumber = "1234567890",
            country = "US",
            name = "John Doe",
            locale = locale,
            amount = 1000L,
            currency = "USD",
            incentiveEligibilitySession = incentiveEligibilitySession,
            requestSurface = "MOBILE",
            consentAction = ConsumerSignUpConsentAction.Implied,
            verificationToken = "token123",
            appId = "appId123"
        )

        val paramMap = signUpParams.toParamMap()

        assertEquals("test@example.com", paramMap["email_address"])
        assertEquals("1234567890", paramMap["phone_number"])
        assertEquals("US", paramMap["country"])
        assertEquals("PHONE_NUMBER", paramMap["country_inferring_method"])
        assertEquals(1000L, paramMap["amount"])
        assertEquals("USD", paramMap["currency"])
        assertEquals("implied_consent_withspm_mobile_v0", paramMap["consent_action"])
        assertEquals("MOBILE", paramMap["request_surface"])
        assertEquals("en-US", paramMap["locale"])
        assertEquals("John Doe", paramMap["legal_name"])
        assertEquals("token123", paramMap["android_verification_token"])
        assertEquals("appId123", paramMap["app_id"])
        assertEquals("incentive_value", paramMap["incentive_key"])
    }

    @Test
    fun `toParamMap should exclude null fields`() {
        val signUpParams = SignUpParams(
            email = "test@example.com",
            phoneNumber = "1234567890",
            country = "US",
            name = null,
            locale = null,
            amount = null,
            currency = null,
            incentiveEligibilitySession = null,
            requestSurface = "MOBILE",
            consentAction = ConsumerSignUpConsentAction.Implied
        )

        val paramMap = signUpParams.toParamMap()

        assertEquals("test@example.com", paramMap["email_address"])
        assertEquals("1234567890", paramMap["phone_number"])
        assertEquals("US", paramMap["country"])
        assertEquals("PHONE_NUMBER", paramMap["country_inferring_method"])
        assertEquals("implied_consent_withspm_mobile_v0", paramMap["consent_action"])
        assertEquals("MOBILE", paramMap["request_surface"])
        assertFalse(paramMap.containsKey("amount"))
        assertFalse(paramMap.containsKey("currency"))
        assertFalse(paramMap.containsKey("locale"))
        assertFalse(paramMap.containsKey("legal_name"))
        assertFalse(paramMap.containsKey("android_verification_token"))
        assertFalse(paramMap.containsKey("app_id"))
    }

    @Test
    fun `toParamMap should exclude empty name`() {
        val signUpParams = SignUpParams(
            email = "test@example.com",
            phoneNumber = "1234567890",
            country = "US",
            name = "",
            locale = Locale.US,
            amount = 1000L,
            currency = "USD",
            incentiveEligibilitySession = IncentiveEligibilitySession.PaymentIntent("incentive123"),
            requestSurface = "MOBILE",
            consentAction = ConsumerSignUpConsentAction.Implied,
            verificationToken = "token123",
            appId = "appId123"
        )

        val paramMap = signUpParams.toParamMap()

        assertFalse(paramMap.containsKey("legal_name"))
    }
}