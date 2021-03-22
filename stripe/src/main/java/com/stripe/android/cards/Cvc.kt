package com.stripe.android.cards

internal sealed class Cvc {

    /**
     * A representation of a partial or full CVC that hasn't been validated.
     */
    internal data class Unvalidated internal constructor(
        private val denormalized: String
    ) : Cvc() {
        internal val normalized = denormalized.filter { it.isDigit() }

        fun validate(maxLength: Int): Validated? {
            return if (isComplete(maxLength)) {
                Validated(normalized)
            } else {
                null
            }
        }

        private fun isComplete(maxLength: Int) =
            setOf(COMMON_LENGTH, maxLength).contains(normalized.length)

        fun isPartialEntry(maxLength: Int) = normalized.isNotBlank() && !isComplete(maxLength)
    }

    /**
     * A representation of a client-side validated CVC.
     */
    internal data class Validated internal constructor(
        internal val value: String
    ) : Cvc()

    private companion object {
        private const val COMMON_LENGTH: Int = 3
    }
}
