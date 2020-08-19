package com.stripe.android.cards

import com.stripe.android.StripeTextUtils

/**
 * A representation of a partial or full card number.
 */
internal data class CardNumber(
    private val number: String
) {
    private val normalizedNumber = StripeTextUtils.removeSpacesAndHyphens(number).orEmpty()

    /**
     * Format a number based on its expected length
     *
     * e.g. `"4242424242424242"` with pan length `16` will return `"4242 4242 4242 4242"`;
     * `"424242"` with pan length `16` will return `"4242 42"`;
     * `"4242424242424242"` with pan length `14` will return `"4242 424242 4242"`;
     */
    fun getFormatted(
        panLength: Int = DEFAULT_PAN_LENGTH
    ) = formatNumber(panLength, getSpacePositions(panLength))

    private fun formatNumber(
        panLength: Int,
        spacePositions: Set<Int>
    ): String {
        val spacelessCardNumber = normalizedNumber.take(panLength)
        val groups = arrayOfNulls<String?>(spacePositions.size + 1)

        val length = spacelessCardNumber.length
        var lastUsedIndex = 0

        spacePositions
            .toList().sorted().forEachIndexed { idx, spacePosition ->
                val adjustedSpacePosition = spacePosition - idx
                if (length > adjustedSpacePosition) {
                    groups[idx] = spacelessCardNumber.substring(
                        lastUsedIndex,
                        adjustedSpacePosition
                    )
                    lastUsedIndex = adjustedSpacePosition
                }
            }

        // populate any remaining digits in the first index with a null value
        groups
            .indexOfFirst { it == null }
            .takeIf {
                it != -1
            }?.let {
                groups[it] = spacelessCardNumber.substring(lastUsedIndex)
            }

        return groups
            .takeWhile { it != null }
            .joinToString(" ")
    }

    private companion object {
        private fun getSpacePositions(panLength: Int) = SPACE_POSITIONS[panLength]
            ?: DEFAULT_SPACE_POSITIONS

        private const val DEFAULT_PAN_LENGTH = 16
        private val DEFAULT_SPACE_POSITIONS = setOf(4, 9, 14)

        private val SPACE_POSITIONS = mapOf(
            14 to setOf(4, 11),
            15 to setOf(4, 11),
            16 to setOf(4, 9, 14),
            19 to setOf(4, 9, 14, 19)
        )
    }
}
