package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.CryptoCustomerRequestParams
import com.stripe.android.crypto.onramp.model.IdType
import com.stripe.android.crypto.onramp.model.KycCollectionRequest
import com.stripe.android.crypto.onramp.model.KycInfo
import org.junit.Test

class KycInfoTest {
    @Test
    fun `primary constructor contact fields default to null`() {
        val info = KycInfo(null, null, null, IdType.SocialSecurityNumber, null, null)

        assertThat(info.email).isNull()
        assertThat(info.phone).isNull()
        assertThat(info.rawPhone).isNull()
    }

    @Test
    fun `legacy short constructor preserves defaults`() {
        val info = KycInfo(null, null, null, null, null)

        assertThat(info.idType).isEqualTo(IdType.SocialSecurityNumber)
        assertThat(info.email).isNull()
        assertThat(info.phone).isNull()
        assertThat(info.rawPhone).isNull()
    }

    @Test
    fun `legacy full constructor preserves defaults`() {
        val info = KycInfo(
            firstName = null,
            lastName = null,
            idNumber = null,
            dateOfBirth = null,
            address = null,
            birthCountry = null,
            birthCity = null,
            nationalities = null,
        )

        assertThat(info.idType).isEqualTo(IdType.SocialSecurityNumber)
        assertThat(info.email).isNull()
        assertThat(info.phone).isNull()
        assertThat(info.rawPhone).isNull()
    }

    @Test
    fun `equal contact information has equal hash codes`() {
        val first = info(email = "user@example.com", phone = "+12125551234", rawPhone = "(212) 555-1234")
        val second = info(email = "user@example.com", phone = "+12125551234", rawPhone = "(212) 555-1234")

        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun `email participates in equality`() {
        assertThat(info(email = "user@example.com")).isNotEqualTo(info())
    }

    @Test
    fun `phone participates in equality`() {
        assertThat(info(phone = "+12125551234")).isNotEqualTo(info())
    }

    @Test
    fun `raw phone participates in equality`() {
        assertThat(info(rawPhone = "(212) 555-1234")).isNotEqualTo(info())
    }

    @Test
    fun `contact information does not change KYC submission`() {
        val credentials = CryptoCustomerRequestParams.Credentials("consumer_secret")
        val withContact = KycCollectionRequest.fromKycInfo(
            info(email = "user@example.com", phone = "+12125551234", rawPhone = "(212) 555-1234"),
            credentials
        )
        val withoutContact = KycCollectionRequest.fromKycInfo(info(), credentials)

        assertThat(withContact).isEqualTo(withoutContact)
    }

    private fun info(email: String? = null, phone: String? = null, rawPhone: String? = null) = KycInfo(
        firstName = null,
        lastName = null,
        idNumber = null,
        idType = IdType.SocialSecurityNumber,
        dateOfBirth = null,
        address = null,
        email = email,
        phone = phone,
        rawPhone = rawPhone,
    )
}
