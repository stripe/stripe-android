package com.stripe.android.stripecardscan.payment.card

import android.annotation.SuppressLint
import androidx.test.filters.SmallTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SAMPLE_AMEX_PAN = "340000000000009"
private const val SAMPLE_DINERS_CLUB_PAN_14 = "36281412218285"
private const val SAMPLE_DINERS_CLUB_PAN_16 = "3628141221828005"
private const val SAMPLE_DISCOVER_PAN = "6011000000000004"
private const val SAMPLE_JCB_PAN = "3528902605615800"
private const val SAMPLE_MASTERCARD_PAN = "5500000000000004"
private const val SAMPLE_UNIONPAY_16_PAN = "6212345678901232"
private const val SAMPLE_UNIONPAY_17_PAN = "62123456789000003"
private const val SAMPLE_UNIONPAY_18_PAN = "621234567890000002"
private const val SAMPLE_UNIONPAY_19_PAN = "6212345678900000003"
private const val SAMPLE_VISA_PAN = "4847186095118770"

private const val SAMPLE_AMEX_IIN = "340000"
private const val SAMPLE_DINERS_CLUB_IIN = "300000"
private const val SAMPLE_DISCOVER_IIN = "601100"
private const val SAMPLE_JCB_IIN = "352890"
private const val SAMPLE_MASTERCARD_IIN = "550000"
private const val SAMPLE_UNIONPAY_IIN = "621234"
private const val SAMPLE_VISA_IIN = "411111"

private const val SAMPLE_AMEX_CVC = "1234"
private const val SAMPLE_NORMAL_CVC = "123"
private const val SAMPLE_INVALID_CVC = "12"

private const val SAMPLE_CUSTOM_16_PAN = "9900000000000101"
private const val SAMPLE_CUSTOM_17_PAN = "99000000000000002"
private const val SAMPLE_CUSTOM_18_PAN = "990000000000000903"
private const val SAMPLE_CUSTOM_19_PAN = "9900000000000000804"
private const val SAMPLE_ADVANCED_CUSTOM_20_PAN = "99100000000000000505"

private const val SAMPLE_CUSTOM_IIN = "990023"
private const val SAMPLE_ADVANCED_CUSTOM_IIN = "991456"

private const val SAMPLE_CUSTOM_CVC = "123"
private const val SAMPLE_ADVANCED_CUSTOM_CVC = "1234"

private val SAMPLE_CUSTOM_CARD_ISSUER = CardIssuer.Custom("Custom")
private val SAMPLE_ADVANCED_CUSTOM_CARD_ISSUER = CardIssuer.Custom("Advanced Custom")

class PaymentCardTest {

    @SuppressLint("CheckResult")
    @Before
    fun addCardIssuers() {
        supportCardIssuer(
            990000..990024,
            SAMPLE_CUSTOM_CARD_ISSUER,
            (16..19).toList(),
            listOf(3)
        )
        supportCardIssuer(
            991000..991999,
            SAMPLE_ADVANCED_CUSTOM_CARD_ISSUER,
            listOf(20),
            listOf(4)
        )

        addFormatPan(SAMPLE_CUSTOM_CARD_ISSUER, 16, 4, 3, 5, 4)
        addFormatPan(SAMPLE_CUSTOM_CARD_ISSUER, 17, 4, 4, 5, 4)
        addFormatPan(SAMPLE_CUSTOM_CARD_ISSUER, 18, 4, 5, 5, 4)
        addFormatPan(SAMPLE_CUSTOM_CARD_ISSUER, 19, 5, 5, 5, 4)
        addFormatPan(SAMPLE_ADVANCED_CUSTOM_CARD_ISSUER, 20, 5, 5, 5, 5)
    }

    @Test
    @SmallTest
    fun getCardIssuer() {
        assertEquals(CardIssuer.AmericanExpress, getCardIssuer(SAMPLE_AMEX_PAN))
        assertEquals(CardIssuer.DinersClub, getCardIssuer(SAMPLE_DINERS_CLUB_PAN_14))
        assertEquals(CardIssuer.DinersClub, getCardIssuer(SAMPLE_DINERS_CLUB_PAN_16))
        assertEquals(CardIssuer.Discover, getCardIssuer(SAMPLE_DISCOVER_PAN))
        assertEquals(CardIssuer.JCB, getCardIssuer(SAMPLE_JCB_PAN))
        assertEquals(CardIssuer.MasterCard, getCardIssuer(SAMPLE_MASTERCARD_PAN))
        assertEquals(CardIssuer.UnionPay, getCardIssuer(SAMPLE_UNIONPAY_16_PAN))
        assertEquals(CardIssuer.UnionPay, getCardIssuer(SAMPLE_UNIONPAY_17_PAN))
        assertEquals(CardIssuer.UnionPay, getCardIssuer(SAMPLE_UNIONPAY_18_PAN))
        assertEquals(CardIssuer.UnionPay, getCardIssuer(SAMPLE_UNIONPAY_19_PAN))
        assertEquals(CardIssuer.Visa, getCardIssuer(SAMPLE_VISA_PAN))
        assertEquals(SAMPLE_CUSTOM_CARD_ISSUER, getCardIssuer(SAMPLE_CUSTOM_16_PAN))
        assertEquals(SAMPLE_CUSTOM_CARD_ISSUER, getCardIssuer(SAMPLE_CUSTOM_17_PAN))
        assertEquals(SAMPLE_CUSTOM_CARD_ISSUER, getCardIssuer(SAMPLE_CUSTOM_18_PAN))
        assertEquals(SAMPLE_CUSTOM_CARD_ISSUER, getCardIssuer(SAMPLE_CUSTOM_19_PAN))
        assertEquals(
            SAMPLE_ADVANCED_CUSTOM_CARD_ISSUER,
            getCardIssuer(SAMPLE_ADVANCED_CUSTOM_20_PAN)
        )
    }

    @Test
    @SmallTest
    fun isValidPan() {
        assertTrue { isValidPan(SAMPLE_AMEX_PAN) }
        assertTrue { isValidPan(SAMPLE_DINERS_CLUB_PAN_14) }
        assertTrue { isValidPan(SAMPLE_DINERS_CLUB_PAN_16) }
        assertTrue { isValidPan(SAMPLE_DISCOVER_PAN) }
        assertTrue { isValidPan(SAMPLE_JCB_PAN) }
        assertTrue { isValidPan(SAMPLE_MASTERCARD_PAN) }
        assertTrue { isValidPan(SAMPLE_UNIONPAY_16_PAN) }
        assertTrue { isValidPan(SAMPLE_UNIONPAY_17_PAN) }
        assertTrue { isValidPan(SAMPLE_UNIONPAY_18_PAN) }
        assertTrue { isValidPan(SAMPLE_UNIONPAY_19_PAN) }
        assertTrue { isValidPan(SAMPLE_VISA_PAN) }
        assertTrue { isValidPan(SAMPLE_CUSTOM_16_PAN) }
        assertTrue { isValidPan(SAMPLE_CUSTOM_17_PAN) }
        assertTrue { isValidPan(SAMPLE_CUSTOM_18_PAN) }
        assertTrue { isValidPan(SAMPLE_CUSTOM_19_PAN) }
        assertTrue { isValidPan(SAMPLE_ADVANCED_CUSTOM_20_PAN) }
    }

    @Test
    @SmallTest
    fun isValidIin() {
        assertTrue { isValidIin(SAMPLE_AMEX_IIN) }
        assertTrue { isValidIin(SAMPLE_DINERS_CLUB_IIN) }
        assertTrue { isValidIin(SAMPLE_DISCOVER_IIN) }
        assertTrue { isValidIin(SAMPLE_JCB_IIN) }
        assertTrue { isValidIin(SAMPLE_MASTERCARD_IIN) }
        assertTrue { isValidIin(SAMPLE_UNIONPAY_IIN) }
        assertTrue { isValidIin(SAMPLE_VISA_IIN) }
        assertTrue { isValidIin(SAMPLE_CUSTOM_IIN) }
        assertTrue { isValidIin(SAMPLE_ADVANCED_CUSTOM_IIN) }
    }

    @Test
    @SmallTest
    fun isValidCvc() {
        assertTrue { isValidCvc(SAMPLE_AMEX_CVC, CardIssuer.AmericanExpress) }
        assertTrue { isValidCvc(SAMPLE_NORMAL_CVC, CardIssuer.Visa) }
        assertTrue { isValidCvc(SAMPLE_CUSTOM_CVC, SAMPLE_CUSTOM_CARD_ISSUER) }
        assertTrue { isValidCvc(SAMPLE_ADVANCED_CUSTOM_CVC, SAMPLE_ADVANCED_CUSTOM_CARD_ISSUER) }
        assertFalse { isValidCvc(SAMPLE_AMEX_CVC, CardIssuer.MasterCard) }
        assertTrue { isValidCvc(SAMPLE_NORMAL_CVC, CardIssuer.AmericanExpress) }
        assertFalse { isValidCvc(SAMPLE_INVALID_CVC, null) }
        assertTrue { isValidCvc(SAMPLE_NORMAL_CVC, null) }
        assertTrue { isValidCvc(SAMPLE_AMEX_CVC, null) }
        assertFalse { isValidCvc("a12", CardIssuer.Visa) }
    }

    @Test
    @SmallTest
    fun formatPan() {
        assertEquals("3400 000000 00009", formatPan(SAMPLE_AMEX_PAN))
        assertEquals("3628 141221 8285", formatPan(SAMPLE_DINERS_CLUB_PAN_14))
        assertEquals("3628 1412 2182 8005", formatPan(SAMPLE_DINERS_CLUB_PAN_16))
        assertEquals("6011 0000 0000 0004", formatPan(SAMPLE_DISCOVER_PAN))
        assertEquals("3528 9026 0561 5800", formatPan(SAMPLE_JCB_PAN))
        assertEquals("5500 0000 0000 0004", formatPan(SAMPLE_MASTERCARD_PAN))
        assertEquals("6212 3456 7890 1232", formatPan(SAMPLE_UNIONPAY_16_PAN))
        assertEquals("6212 3456 7890 00003", formatPan(SAMPLE_UNIONPAY_17_PAN))
        assertEquals("6212 3456 7890 000002", formatPan(SAMPLE_UNIONPAY_18_PAN))
        assertEquals("621234 5678900000003", formatPan(SAMPLE_UNIONPAY_19_PAN))
        assertEquals("4847 1860 9511 8770", formatPan(SAMPLE_VISA_PAN))
        assertEquals("9900 000 00000 0101", formatPan(SAMPLE_CUSTOM_16_PAN))
        assertEquals("9900 0000 00000 0002", formatPan(SAMPLE_CUSTOM_17_PAN))
        assertEquals("9900 00000 00000 0903", formatPan(SAMPLE_CUSTOM_18_PAN))
        assertEquals("99000 00000 00000 0804", formatPan(SAMPLE_CUSTOM_19_PAN))
        assertEquals("99100 00000 00000 00505", formatPan(SAMPLE_ADVANCED_CUSTOM_20_PAN))
        assertEquals("1234 5678 9012 3456", formatPan("1234567890123456"))
    }
}
