@file:JvmName("PaymentCardUtils")

package com.stripe.android.stripecardscan.payment.card

import androidx.annotation.CheckResult

/*
 * Payment cards always have a PAN (Primary Account Number) on one side of the card. This PAN
 * contains identification information about the card itself.
 *
 * The PAN consists of the following:
 *
 * IIN / BIN       Check Digit
 *    |              |
 * |-----|           |
 * 4833 1200 3412 3456
 * |      |---------|
 * |           |
 * MII      Account Number
 *
 * # MII
 * The first digit in a card PAN is the MII (Major Industry Identifier). The following table matches
 * the MII to an industry (taken from https://chargebacks911.com/):
 *
 * | --------------- | --------------------------------------------------------------------------- |
 * | MII Digit Value | Issuer Category                                                             |
 * | --------------- | --------------------------------------------------------------------------- |
 * |        0        | ISO/TC 68 Assignment                                                        |
 * |        1        | Airline cards                                                               |
 * |        2        | Airlines cards (and other future industry assignments)                      |
 * |        3        | Travel and Entertainment Cards                                              |
 * |        4        | Banking and Financial Cards                                                 |
 * |        5        | Banking and Financial Cards                                                 |
 * |        6        | Merchandising and Financial Cards                                           |
 * |        7        | Gas Cards, Other Future Industry Assignments                                |
 * |        8        | Healthcare Cards, Telecommunications, Other Future Industry Assignments     |
 * |        9        | For Use by National Standards Bodies                                        |
 * | --------------- | --------------------------------------------------------------------------- |
 *
 * # IIN / BIN
 * Issuer networks can be identified by the IIN (Issuer Identification Number / Bank Identification
 * Number). The IIN consists of the first few numbers (up to 6) of the card PAN. The IIN translates
 * to the following information:
 *
 * 1. The name, address, and phone number of the bank funds will be transferred from
 * 2. The card brand (Visa, Mastercard, American Express, etc.)
 * 3. What type of card it is (debit, credit, prepaid, etc.)
 * 4. What level the card is (black, platinum, business)
 * 5. Whether the issuer is in the same country as the device used in the transaction
 * 6. Whether the address provided by the cardholder matches the one on file
 *
 * The following table maps supported IINs to issuers, taken from wikipedia
 * https://en.wikipedia.org/wiki/Payment_card_number#Issuer_identification_number_(IIN)
 *
 * | --------------- | ------------------------- | ------- | ------- | --------------------------- |
 * | IIN             | Issuer                    | PAN Len | CVC Len | Validation                  |
 * | --------------- | ------------------------- | ------- | ------- | --------------------------- |
 * | 34****          | American Express          | 15      | 3-4     | Luhn                        |
 * | 37****          | American Express          | 15      | 3-4     | Luhn                        |
 * | 300*** - 305*** | Diners Club International | 16 - 19 | 3       | Luhn                        |
 * | 3095**          | Diners Club International | 16 - 19 | 3       | Luhn                        |
 * | 36****          | Diners Club International | 14 - 19 | 3       | Luhn                        |
 * | 38**** - 39**** | Diners Club International | 16 - 19 | 3       | Luhn                        |
 * | 6011**          | Discover                  | 16 - 19 | 3       | Luhn                        |
 * | 622126 - 622925 | Discover                  | 16 - 19 | 3       | Luhn                        |
 * | 624000 - 626999 | Discover                  | 16 - 19 | 3       | Luhn                        |
 * | 628200 - 628899 | Discover                  | 16 - 19 | 3       | Luhn                        |
 * | 64**** - 65**** | Discover                  | 16 - 19 | 3       | Luhn                        |
 * | 3528** - 3589** | JCB                       | 16 - 19 | 3       | Luhn                        |
 * | 2221** - 2720** | MasterCard                | 16      | 3       | Luhn                        |
 * | 51**** - 55**** | MasterCard                | 16      | 3       | Luhn                        |
 * | 50****          | MasterCard (Maestro)      | 12 - 19 | 3       | Luhn                        |
 * | 56**** - 69**** | MasterCard (Maestro)      | 12 - 19 | 3       | Luhn                        |
 * | 6759**          | MasterCard (Maestro)      | 12 - 19 | 3       | Luhn                        |
 * | 676770          | MasterCard (Maestro)      | 12 - 19 | 3       | Luhn                        |
 * | 676774          | MasterCard (Maestro)      | 12 - 19 | 3       | Luhn                        |
 * | 62****          | UnionPay                  | 16 - 19 | 3       | Luhn                        |
 * | 81****          | UnionPay                  | 16 - 19 | 3       | Luhn                        |
 * | 4*****          | Visa                      | 16 - 19 | 3       | Luhn                        |
 * | --------------- | ------------------------- | ------- | ------- | --------------------------- |
 */

private const val IIN_LENGTH = 6
internal const val QUICK_READ_LENGTH = 16
internal const val QUICK_READ_GROUP_LENGTH = 4

private val VALID_CVC_LENGTHS = 3..4

internal data class IssuerData(
    val iinRange: IntRange,
    val issuer: CardIssuer,
    val panLengths: List<Int>,
    val cvcLengths: List<Int>,
    val panValidator: PanValidator
)

/**
 * This list describes the table above. The order of this list indicates priority. Items higher in
 * the list get priority over items lower in the list when selecting by IIN.
 */
private val ISSUER_TABLE: List<IssuerData> = listOf(
    IssuerData(
        340000..349999,
        CardIssuer.AmericanExpress,
        listOf(15),
        (3..4).toList(),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        370000..379999,
        CardIssuer.AmericanExpress,
        listOf(15),
        (3..4).toList(),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        300000..305999,
        CardIssuer.DinersClub,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        309500..309599,
        CardIssuer.DinersClub,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        360000..369999,
        CardIssuer.DinersClub,
        (14..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        380000..399999,
        CardIssuer.DinersClub,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        601100..601199,
        CardIssuer.Discover,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        622126..622925,
        CardIssuer.Discover,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        624000..626999,
        CardIssuer.Discover,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        628200..628899,
        CardIssuer.Discover,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        640000..659999,
        CardIssuer.Discover,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        352800..358999,
        CardIssuer.JCB,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        620000..629999,
        CardIssuer.UnionPay,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        810000..819999,
        CardIssuer.UnionPay,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        222100..272099,
        CardIssuer.MasterCard,
        (16..16).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        510000..559999,
        CardIssuer.MasterCard,
        (16..16).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        500000..509999,
        CardIssuer.MasterCard,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        560000..699999,
        CardIssuer.MasterCard,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        675900..675999,
        CardIssuer.MasterCard,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        676770..676770,
        CardIssuer.MasterCard,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        676774..676774,
        CardIssuer.MasterCard,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    ),
    IssuerData(
        400000..499999,
        CardIssuer.Visa,
        (16..19).toList(),
        listOf(3),
        LengthPanValidator + LuhnPanValidator
    )
)

/**
 * This list is an extension of the list above and includes any custom card configurations as
 * required by certain users.
 */
private var CUSTOM_ISSUER_TABLE: MutableList<IssuerData> = mutableListOf()

/**
 * Get an issuer from a complete or partial card number. If the pan is null, return an unknown
 * issuer.
 */
@CheckResult
internal fun getCardIssuer(pan: String?): CardIssuer =
    normalizeCardNumber(pan).let { normalizedPan ->
        getIssuerData(normalizedPan)?.issuer ?: CardIssuer.Unknown
    }

/**
 * Determine if a PAN is valid.
 *
 * TODO: this should use a contract like the following once contracts are no longer experimental:
 * ```
 * contract { returns(true) implies (pan != null) }
 * ```
 */
@CheckResult
internal fun isValidPan(pan: String?): Boolean {
    // contract { returns(true) implies (pan != null) }
    return normalizeCardNumber(pan).let { normalizedPan ->
        val iinData = getIssuerData(normalizedPan) ?: return false
        iinData.panValidator.isValidPan(normalizedPan)
    }
}

/**
 * Determine if an IIN is valid.
 */
@CheckResult
internal fun isValidIin(iin: String?): Boolean {
    // contract { returns(true) implies (iin != null) }
    return normalizeCardNumber(iin).let { normalizedPan ->
        (getIssuerData(normalizedPan)?.issuer ?: CardIssuer.Unknown) != CardIssuer.Unknown
    }
}

/**
 * Determine if a CVC is valid based on an issuer.
 */
@CheckResult
internal fun isValidCvc(cvc: String?, issuer: CardIssuer?): Boolean {
    // contract { returns(true) implies (cvc != null) }
    return normalizeCardNumber(cvc).let { cvcNumber ->
        val issuerDataList = getIssuerData(issuer ?: CardIssuer.Unknown)
        if (issuerDataList.isEmpty()) {
            cvcNumber.length in VALID_CVC_LENGTHS
        } else {
            issuerDataList.any { cvcNumber.length in it.cvcLengths }
        }
    }
}

/**
 * Get an IIN for a given PAN.
 */
@CheckResult
internal fun iinFromPan(pan: String): String =
    if (pan.length < IIN_LENGTH) {
        pan.padEnd(IIN_LENGTH, '0')
    } else {
        pan.take(IIN_LENGTH)
    }

/**
 * Get data for a given IIN or PAN.
 */
@CheckResult
internal fun getIssuerData(cardNumber: String): IssuerData? = (CUSTOM_ISSUER_TABLE + ISSUER_TABLE)
    .firstOrNull { iinFromPan(cardNumber).toInt() in it.iinRange }

/**
 * Get data for a given [CardIssuer].
 */
@CheckResult
private fun getIssuerData(issuer: CardIssuer): List<IssuerData> =
    (CUSTOM_ISSUER_TABLE + ISSUER_TABLE).filter { it.issuer == issuer }

/**
 * Adds support for a new [CardIssuer]
 */
@CheckResult
fun supportCardIssuer(
    iins: IntRange,
    cardIssuer: CardIssuer,
    panLengths: List<Int>,
    cvcLengths: List<Int>,
    validationFunction: PanValidator = LengthPanValidator + LuhnPanValidator
) = CUSTOM_ISSUER_TABLE.add(
    IssuerData(iins, cardIssuer, panLengths, cvcLengths, validationFunction)
)

/**
 * Normalize a PAN by removing all non-numeric characters.
 */
@CheckResult
internal fun normalizeCardNumber(cardNumber: String?) = cardNumber?.filter { it.isDigit() } ?: ""
