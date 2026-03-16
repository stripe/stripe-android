package com.stripe.android.stripecardscan.payment.card

/*
 * The following are known PAN formats. The information in this table was taken from
 * https://baymard.com/checkout-usability/credit-card-patterns and indirectly from
 * https://web.archive.org/web/20170822221741/https://www.discovernetwork.com/downloads/IPP_VAR_Compliance.pdf
 *
 * | ------------------------- | ------- | ------------------------------------ |
 * | Issuer                    | PAN Len | Display Format                       |
 * | ------------------------- | ------- | ------------------------------------ |
 * | American Express          | 15      | 4 - 6 - 5                            |
 * | Diners Club International | 14      | 4 - 6 - 4                            |
 * | Diners Club International | 15      | Unknown                              |
 * | Diners Club International | 16      | 4 - 4 - 4 - 4                        |
 * | Diners Club International | 17      | Unknown                              |
 * | Diners Club International | 18      | Unknown                              |
 * | Diners Club International | 19      | Unknown                              |
 * | Discover                  | 16      | 4 - 4 - 4 - 4                        |
 * | Discover                  | 17      | Unknown                              |
 * | Discover                  | 18      | Unknown                              |
 * | Discover                  | 19      | Unknown                              |
 * | MasterCard                | 16      | 4 - 4 - 4 - 4                        |
 * | MasterCard (Maestro)      | 12      | Unknown                              |
 * | MasterCard (Maestro)      | 13      | 4 - 4 - 5                            |
 * | MasterCard (Maestro)      | 14      | Unknown                              |
 * | MasterCard (Maestro)      | 15      | 4 - 6 - 5                            |
 * | MasterCard (Maestro)      | 16      | 4 - 4 - 4 - 4                        |
 * | MasterCard (Maestro)      | 17      | Unknown                              |
 * | MasterCard (Maestro)      | 18      | Unknown                              |
 * | MasterCard (Maestro)      | 19      | 4 - 4 - 4 - 4 - 3                    |
 * | UnionPay                  | 16      | 4 - 4 - 4 - 4                        |
 * | UnionPay                  | 17      | Unknown                              |
 * | UnionPay                  | 18      | Unknown                              |
 * | UnionPay                  | 19      | 6 - 13                               |
 * | Visa                      | 16      | 4 - 4 - 4 - 4                        |
 * | ------------------------- | ------- | ------------------------------------ |
 */

/**
 * Format a card PAN for display.
 */
internal fun formatPan(pan: String) = normalizeCardNumber(pan).let {
    val issuer = getCardIssuer(pan)
    val formatter = CUSTOM_PAN_FORMAT_TABLE[issuer]?.get(pan.length)
        ?: PAN_FORMAT_TABLE[issuer]?.get(pan.length) ?: DEFAULT_PAN_FORMATTERS[pan.length]
    formatter?.formatPan(pan) ?: pan
}

/**
 * Add a new way to format a PAN
 */
internal fun addFormatPan(cardIssuer: CardIssuer, length: Int, vararg blockSizes: Int) {
    CUSTOM_PAN_FORMAT_TABLE.getOrPut(cardIssuer, { mutableMapOf() })[length] =
        PanFormatter(*blockSizes)
}

/**
 * A class that can format a PAN for display given a list of number block sizes.
 */
private class PanFormatter(vararg blockSizes: Int) {
    private val blockIndices = blockSizesToIndicies(blockSizes)

    private fun blockSizesToIndicies(blockSizes: IntArray): List<Int> {
        var currentIndex = 0
        return blockSizes.map {
            val newValue = it + currentIndex
            currentIndex = newValue
            newValue
        }
    }

    /**
     * Format the PAN for display using the number block sizes.
     */
    fun formatPan(pan: String): String {
        val builder = StringBuilder()
        for (i in pan.indices) {
            if (i in blockIndices) {
                builder.append(' ')
            }
            builder.append(pan[i])
        }

        return builder.toString()
    }
}

/**
 * A mapping of [CardIssuer] to length and [PanFormatter]
 */
private val PAN_FORMAT_TABLE: Map<CardIssuer, Map<Int, PanFormatter>> = mapOf(
    CardIssuer.AmericanExpress to mapOf(
        15 to PanFormatter(4, 6, 5)
    ),

    CardIssuer.DinersClub to mapOf(
        14 to PanFormatter(4, 6, 4),
        15 to PanFormatter(4, 6, 5), // Best guess
        16 to PanFormatter(4, 4, 4, 4),
        17 to PanFormatter(4, 4, 4, 5), // Best guess
        18 to PanFormatter(4, 4, 4, 6), // Best guess
        19 to PanFormatter(4, 4, 4, 4, 3) // Best guess
    ),

    CardIssuer.Discover to mapOf(
        16 to PanFormatter(4, 4, 4, 4),
        17 to PanFormatter(4, 4, 4, 4, 1), // Best guess
        18 to PanFormatter(4, 4, 4, 4, 2), // Best guess
        19 to PanFormatter(4, 4, 4, 4, 3) // Best guess
    ),

    CardIssuer.MasterCard to mapOf(
        12 to PanFormatter(4, 4, 4), // Best guess
        13 to PanFormatter(4, 4, 5),
        14 to PanFormatter(4, 6, 4), // Best guess
        15 to PanFormatter(4, 6, 5),
        16 to PanFormatter(4, 4, 4, 4),
        17 to PanFormatter(4, 4, 4, 5), // Best guess
        18 to PanFormatter(4, 4, 4, 6), // Best guess
        19 to PanFormatter(4, 4, 4, 4, 3) // Best guess
    ),

    CardIssuer.UnionPay to mapOf(
        16 to PanFormatter(4, 4, 4, 4),
        17 to PanFormatter(4, 4, 4, 5), // Best guess
        18 to PanFormatter(4, 4, 4, 6), // Best guess
        19 to PanFormatter(6, 13)
    ),

    CardIssuer.Visa to mapOf(
        16 to PanFormatter(4, 4, 4, 4)
    )
)

/**
 * Default length [PanFormatter] mappings.
 */
private val DEFAULT_PAN_FORMATTERS: Map<Int, PanFormatter> = mapOf(
    12 to PanFormatter(4, 4, 4),
    13 to PanFormatter(4, 4, 5),
    14 to PanFormatter(4, 6, 4),
    15 to PanFormatter(4, 6, 5),
    16 to PanFormatter(4, 4, 4, 4),
    17 to PanFormatter(4, 4, 4, 5),
    18 to PanFormatter(4, 4, 4, 2),
    19 to PanFormatter(4, 4, 4, 4, 3)
)

/**
 * A mapping of [CardIssuer] to length and [PanFormatter]
 */
private val CUSTOM_PAN_FORMAT_TABLE: MutableMap<CardIssuer, MutableMap<Int, PanFormatter>> =
    mutableMapOf()
