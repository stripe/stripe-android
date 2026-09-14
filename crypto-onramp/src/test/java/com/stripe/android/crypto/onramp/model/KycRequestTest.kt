package com.stripe.android.crypto.onramp.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.CryptoCustomerRequestParams.Credentials
import com.stripe.android.model.DateOfBirth
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

class KycRequestTest {
    @Test
    fun `collection uses social security number by default`() {
        val kycInfo = KycInfo(
            firstName = "Jenny",
            lastName = "Rosen",
            idNumber = "000000000",
            dateOfBirth = DateOfBirth(day = 1, month = 1, year = 1990),
            address = PaymentSheet.Address(country = "US"),
        )

        val request = KycCollectionRequest.fromKycInfo(kycInfo, TEST_CREDENTIALS)

        assertThat(request.idType).isEqualTo("social_security_number")
    }

    @Test
    fun `collection uses Canadian social insurance number`() {
        val request = createCollectionRequest(IdType.CanadianSocialInsuranceNumber)

        assertThat(request.idType).isEqualTo("ca_sin")
    }

    @Test
    fun `collection uses Colombian tax identification number`() {
        val request = createCollectionRequest(IdType.ColombianTaxIdentificationNumber)

        assertThat(request.idType).isEqualTo("co_nit")
    }

    @Test
    fun `collection uses Philippines taxpayer identification number`() {
        val request = createCollectionRequest(IdType.PhilippinesTaxpayerIdentificationNumber)

        assertThat(request.idType).isEqualTo("ph_tin")
    }

    @Test
    fun `collection omits type when identification number is missing`() {
        val kycInfo = KycInfo(
            firstName = "Jenny",
            lastName = "Rosen",
            idNumber = null,
            idType = IdType.CanadianSocialInsuranceNumber,
            dateOfBirth = DateOfBirth(day = 1, month = 1, year = 1990),
            address = PaymentSheet.Address(country = "CA"),
        )

        val request = KycCollectionRequest.fromKycInfo(kycInfo, TEST_CREDENTIALS)

        assertThat(request.idType).isNull()
    }

    @Test
    fun `refresh preserves the retrieved identification type`() {
        val kycInfo = RefreshKycInfo(
            firstName = "Jenny",
            lastName = "Rosen",
            idNumberLastFour = "0000",
            idType = "ca_sin",
            dateOfBirth = DateOfBirth(day = 1, month = 1, year = 1990),
            address = PaymentSheet.Address(country = "CA"),
        )

        val request = KycRefreshRequest.fromRefreshKycInfo(kycInfo, TEST_CREDENTIALS)

        assertThat(request.idType).isEqualTo("ca_sin")
    }

    private fun createCollectionRequest(idType: IdType): KycCollectionRequest {
        val kycInfo = KycInfo(
            firstName = "Jenny",
            lastName = "Rosen",
            idNumber = "000000000",
            idType = idType,
            dateOfBirth = DateOfBirth(day = 1, month = 1, year = 1990),
            address = PaymentSheet.Address(),
        )
        return KycCollectionRequest.fromKycInfo(kycInfo, TEST_CREDENTIALS)
    }

    private companion object {
        val TEST_CREDENTIALS = Credentials("test-secret")
    }
}
