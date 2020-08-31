package com.stripe.android.cards

internal sealed class Cvc {

    /**
     * A representation of a partial or full CVC that hasn't been validated.
     */
    internal data class Unvalidated internal constructor(
        private val denormalized: String
    ) : Cvc() {
        private val normalizedValue = denormalized.filter { it.isDigit() }

        fun validate(maxLength: Int): Validated? {
            return if (setOf(COMMON_LENGTH, maxLength).contains(normalizedValue.length)) {
                Validated(normalizedValue)
            } else {
                null
            }
        }
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
