package com.stripe.android.model

import com.stripe.android.view.DateUtils

sealed class ExpirationDate {
    internal data class Unvalidated(
        val month: String,
        val year: String
    ) : ExpirationDate() {
        val isMonthValid = runCatching {
            month.toInt() in 1..12
        }.getOrDefault(false)

        constructor(
            month: Int,
            year: Int
        ) : this(
            month = month.toString(),
            year = year.toString()
        )

        fun validate(): Validated? {
            val month = this.month
            val year = this.year

            return runCatching {
                Validated(
                    month.toInt(),
                    DateUtils.convertTwoDigitYearToFour(year.toInt())
                )
            }.getOrNull()
        }

        /**
         * Creates a string value to be entered into an expiration date text field
         * without a divider. For instance, (1, 2020) => "0120". It doesn't matter if
         * the year is two-digit or four. (1, 20) => "0120".
         *
         * Note: A four-digit year will be truncated, so (1, 2720) => "0120". If the year
         * date is 3 digits, the data will be considered invalid and the empty string will be returned.
         * A one-digit date is valid (represents 2001, for instance).
         *
         * @return a length-four string representing the date, or an empty string if input is invalid
         */
        fun getDisplayString(): String {
            // Three-digit years are invalid.
            if (year.length == 3) {
                return ""
            }

            return listOf(
                month.padStart(2, '0'),
                year.takeLast(2).padStart(2, '0')
            ).joinToString(separator = "")
        }

        internal companion object {
            /**
             * Converts raw string input of the format MMYY into a [ExpirationDate.Unvalidated].
             * The month and year fields may be incomplete. This method does not validate the input.
             *
             * "123" would result in ExpirationDate.Unvalidated(month = "12", year = "3")
             * "1" would result in ExpirationDate.Unvalidated(month = "1", year = "")
             *
             * @param input up to four characters of user input
             * @return a [ExpirationDate.Unvalidated] with the first two characters as the month and
             * the last two characters as the year.
             */
            fun create(
                input: String
            ): Unvalidated {
                return input.replace("/".toRegex(), "").let {
                    Unvalidated(
                        month = it.take(2),
                        year = it.drop(2)
                    )
                }
            }
        }
    }

    data class Validated(
        val month: Int,
        val year: Int
    ) : ExpirationDate()
}
