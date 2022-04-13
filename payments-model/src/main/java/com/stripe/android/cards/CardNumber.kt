package com.stripe.android.cards

import androidx.annotation.RestrictTo
import com.stripe.android.CardUtils
import com.stripe.android.model.CardBrand

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CardNumber {

    /**
     * A representation of a partial or full card number that hasn't been validated.
     */
    data class Unvalidated constructor(
        private val denormalized: String
    ) : CardNumber() {
        val normalized = denormalized.filterNot { REJECT_CHARS.contains(it) }

        val length = normalized.length

        val isMaxLength = length == MAX_PAN_LENGTH

        val bin: Bin? = Bin.create(normalized)

        val isValidLuhn = CardUtils.isValidLuhnNumber(normalized)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun validate(panLength: Int): Validated? {
            return if (panLength >= MIN_PAN_LENGTH &&
                normalized.length == panLength &&
                isValidLuhn
            ) {
                Validated(
                    value = normalized
                )
            } else {
                null
            }
        }

        /**
         * Format a number based on its expected length
         *
         * e.g. `"4242424242424242"` with pan length `16` will return `"4242 4242 4242 4242"`;
         * `"424242"` with pan length `16` will return `"4242 42"`;
         * `"4242424242424242"` with pan length `14` will return `"4242 424242 4242"`
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getFormatted(
            panLength: Int = DEFAULT_PAN_LENGTH
        ) = formatNumber(panLength)

        private fun formatNumber(panLength: Int): String {
            val spacePositions = getSpacePositions(panLength)
            val spacelessCardNumber = normalized.take(panLength)
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun isPartialEntry(panLength: Int) =
            (normalized.length != panLength) && normalized.isNotBlank()

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun isPossibleCardBrand(): Boolean {
            return normalized.isNotBlank() &&
                CardBrand.getCardBrands(normalized).first() != CardBrand.Unknown
        }

        private companion object {
            // characters to remove from a denormalized number to make it normalized
            private val REJECT_CHARS = setOf('-', ' ')
        }
    }

    /**
     * A representation of a client-side validated card number.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Validated constructor(
        val value: String
    ) : CardNumber()

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getSpacePositions(panLength: Int) = SPACE_POSITIONS[panLength]
            ?: DEFAULT_SPACE_POSITIONS

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val MIN_PAN_LENGTH = 14

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val MAX_PAN_LENGTH = 19
        const val DEFAULT_PAN_LENGTH = 16
        private val DEFAULT_SPACE_POSITIONS = setOf(4, 9, 14)

        private val SPACE_POSITIONS = mapOf(
            14 to setOf(4, 11),
            15 to setOf(4, 11),
            16 to setOf(4, 9, 14),
            19 to setOf(4, 9, 14, 19)
        )
    }
}
