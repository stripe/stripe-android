package com.stripe.android.uicore.elements

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.core.os.LocaleListCompat
import kotlin.math.max
import kotlin.math.min

/**
 * A formatter for user-input phone numbers.
 * The raw user input should first go through [userInputFilter] to clean it up of invalid or extra
 * characters. The resulting value should be shown in a TextField that uses [visualTransformation],
 * which will display it in the domestic format (e.g. "(555) 555-5555" for "US" locale).
 */
internal sealed class PhoneNumberFormatter {
    /**
     * Region prefix, like "+1" for US.
     */
    abstract val prefix: String

    /**
     * Placeholder for the TextField, showing the expected format of the phone number, like
     * "(555) 555-5555" for US.
     */
    abstract val placeholder: String

    /**
     * Country code that's being used by this formatter, like "US".
     */
    abstract val countryCode: String

    /**
     * The [VisualTransformation] to display the number in the national format on the TextField.
     * Inputs received should have already been filtered by [userInputFilter].
     */
    abstract val visualTransformation: VisualTransformation

    /**
     * Filter invalid characters in the user input, and limits its length based on E.164 specs.
     */
    abstract fun userInputFilter(input: String): String

    /**
     * Transforms the TextField value to the [E.164 format](https://en.wikipedia.org/wiki/E.164).
     */
    abstract fun toE164Format(input: String): String

    /**
     * Phone number formatter for a known region.
     */
    class WithRegion constructor(private val metadata: Metadata) : PhoneNumberFormatter() {
        override val prefix = metadata.prefix
        override val placeholder = metadata.pattern?.replace('#', '5') ?: ""
        override val countryCode = metadata.regionCode

        override fun userInputFilter(input: String) =
            input.filter { VALID_INPUT_RANGE.contains(it) }.run {
                substring(0, min(length, E164_MAX_DIGITS))
            }

        override fun toE164Format(input: String) = "${prefix}${userInputFilter(input).trimStart('0')}"

        override val visualTransformation = object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                // Already filtered in onValueChange
                val formatted = formatNumberNational(text.text)

                return TransformedText(
                    AnnotatedString(formatted),
                    object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int {
                            if (metadata.pattern == null) return offset

                            metadata.pattern.let {
                                if (offset == 0) return 0

                                var count = 0
                                var position = -1
                                it.forEachIndexed { index, c ->
                                    if (c == '#') {
                                        count++
                                        if (count == offset) {
                                            position = index + 1
                                        }
                                    }
                                }
                                // offset is bigger than the number of '#' in the pattern
                                if (position == -1) {
                                    // position is after the full pattern, plus one space, plus the
                                    // remaining offset
                                    position = it.length + 1 + (offset - count)
                                }
                                return position
                            }
                        }

                        override fun transformedToOriginal(offset: Int): Int {
                            return if (metadata.pattern == null) {
                                offset
                            } else if (offset == 0) {
                                0
                            } else {
                                metadata.pattern.let {
                                    // count the number of characters added as the number of non '#' characters
                                    var added = it.substring(0, min(offset, it.length))
                                        .filter { it != '#' }.length

                                    // if bigger than the pattern, one space is added after the pattern
                                    if (offset > it.length) {
                                        added++
                                    }
                                    offset - added
                                }
                            }
                        }
                    }
                )
            }
        }

        /**
         * Format the phone number to the national format, assuming the input was already filtered
         * for invalid characters and the numbers of characters limited based on E.164 spec.
         */
        fun formatNumberNational(filteredInput: String): String {
            if (metadata.pattern == null) {
                // If there's no pattern to format on, return early.
                return filteredInput
            }
            var inputIndex = 0
            val formatted = StringBuilder()
            metadata.pattern.forEach {
                if (inputIndex < filteredInput.length) {
                    formatted.append(
                        if (it == '#') {
                            filteredInput[inputIndex].also {
                                inputIndex++
                            }
                        } else {
                            it
                        }
                    )
                }
            }

            // if there are still any characters left, append them at the end after space
            if (inputIndex < filteredInput.length) {
                formatted.append(' ')
                    .append(filteredInput.substring(inputIndex).toCharArray())
            }

            return formatted.toString()
        }
    }

    /**
     * Phone number formatter for an ubknown region.
     */
    class UnknownRegion(override val countryCode: String) : PhoneNumberFormatter() {
        override val prefix = ""
        override val placeholder = "+############"

        override fun userInputFilter(input: String) =
            input.filter { VALID_INPUT_RANGE.contains(it) }.run {
                substring(0, min(length, E164_MAX_DIGITS))
            }

        override fun toE164Format(input: String) = "+${userInputFilter(input).trimStart('0')}"

        override val visualTransformation =
            VisualTransformation { text ->
                // Already filtered in onValueChange
                val formatted = "+${text.text}"

                TransformedText(
                    AnnotatedString(formatted),
                    object : OffsetMapping { // if no metadata, only the '+' is added
                        override fun originalToTransformed(offset: Int) = (offset + 1)
                        override fun transformedToOriginal(offset: Int) = max((offset - 1), 0)
                    }
                )
            }
    }

    data class Metadata(
        val prefix: String,
        val regionCode: String,
        val pattern: String? = null
    ) {
        init {
            require(pattern == null || pattern.isNotEmpty()) {
                "Pattern should not be empty. Set it to null if it's missing."
            }
        }
    }

    companion object {
        private const val E164_MAX_DIGITS = 15
        private const val COUNTRY_PREFIX_MAX_LENGTH = 5

        val VALID_INPUT_RANGE = ('0'..'9')

        fun forCountry(countryCode: String) =
            allMetadata[countryCode.uppercase()]?.let {
                WithRegion(it)
            } ?: UnknownRegion(countryCode)

        fun forPrefix(phoneNumber: String): PhoneNumberFormatter? {
            var charIndex = 1

            // Find the regions that match the phone number prefix, then pick the top match from the
            // device's locales
            while (charIndex < phoneNumber.lastIndex && charIndex < COUNTRY_PREFIX_MAX_LENGTH - 1) {
                charIndex++

                val country = findBestCountryForPrefix(
                    prefix = phoneNumber.substring(0, charIndex),
                    userLocales = LocaleListCompat.getAdjustedDefault(),
                )

                if (country != null) {
                    return forCountry(country)
                }
            }

            return null
        }

        private fun findBestCountryForPrefix(prefix: String, userLocales: LocaleListCompat) =
            countryCodesForPrefix(prefix).takeIf { it.isNotEmpty() }?.let {
                for (i in 0 until userLocales.size()) {
                    val locale = userLocales.get(i)!!
                    if (it.contains(locale.country)) {
                        return locale.country
                    }
                }
                it.first()
            }

        fun lengthForCountry(countryCode: String) =
            allMetadata[countryCode.uppercase()]?.pattern?.count { it == '#' }

        private fun countryCodesForPrefix(prefix: String) =
            allMetadata.filter { it.value.prefix == prefix }.map { it.value.regionCode }

        internal fun prefixForCountry(countryCode: String) =
            allMetadata[countryCode.uppercase()]?.prefix

        // List shared with iOS: https://github.com/stripe/stripe-ios/blob/master/StripeUICore/StripeUICore/Source/Validators/PhoneNumber.swift
        private val allMetadata = mapOf(
            // NANP member countries and territories (Zone 1)
            // https://en.wikipedia.org/wiki/North_American_Numbering_Plan#Countries_and_territories
            "US" to Metadata(prefix = "+1", regionCode = "US", pattern = "(###) ###-####"),
            "CA" to Metadata(prefix = "+1", regionCode = "CA", pattern = "(###) ###-####"),
            "AG" to Metadata(prefix = "+1", regionCode = "AG", pattern = "(###) ###-####"),
            "AS" to Metadata(prefix = "+1", regionCode = "AS", pattern = "(###) ###-####"),
            "AI" to Metadata(prefix = "+1", regionCode = "AI", pattern = "(###) ###-####"),
            "BB" to Metadata(prefix = "+1", regionCode = "BB", pattern = "(###) ###-####"),
            "BM" to Metadata(prefix = "+1", regionCode = "BM", pattern = "(###) ###-####"),
            "BS" to Metadata(prefix = "+1", regionCode = "BS", pattern = "(###) ###-####"),
            "DM" to Metadata(prefix = "+1", regionCode = "DM", pattern = "(###) ###-####"),
            "DO" to Metadata(prefix = "+1", regionCode = "DO", pattern = "(###) ###-####"),
            "GD" to Metadata(prefix = "+1", regionCode = "GD", pattern = "(###) ###-####"),
            "GU" to Metadata(prefix = "+1", regionCode = "GU", pattern = "(###) ###-####"),
            "JM" to Metadata(prefix = "+1", regionCode = "JM", pattern = "(###) ###-####"),
            "KN" to Metadata(prefix = "+1", regionCode = "KN", pattern = "(###) ###-####"),
            "KY" to Metadata(prefix = "+1", regionCode = "KY", pattern = "(###) ###-####"),
            "LC" to Metadata(prefix = "+1", regionCode = "LC", pattern = "(###) ###-####"),
            "MP" to Metadata(prefix = "+1", regionCode = "MP", pattern = "(###) ###-####"),
            "MS" to Metadata(prefix = "+1", regionCode = "MS", pattern = "(###) ###-####"),
            "PR" to Metadata(prefix = "+1", regionCode = "PR", pattern = "(###) ###-####"),
            "SX" to Metadata(prefix = "+1", regionCode = "SX", pattern = "(###) ###-####"),
            "TC" to Metadata(prefix = "+1", regionCode = "TC", pattern = "(###) ###-####"),
            "TT" to Metadata(prefix = "+1", regionCode = "TT", pattern = "(###) ###-####"),
            "VC" to Metadata(prefix = "+1", regionCode = "VC", pattern = "(###) ###-####"),
            "VG" to Metadata(prefix = "+1", regionCode = "VG", pattern = "(###) ###-####"),
            "VI" to Metadata(prefix = "+1", regionCode = "VI", pattern = "(###) ###-####"),
            // Rest of the world
            "EG" to Metadata(prefix = "+20", regionCode = "EG", pattern = "### ### ####"),
            "SS" to Metadata(prefix = "+211", regionCode = "SS", pattern = "### ### ###"),
            "MA" to Metadata(prefix = "+212", regionCode = "MA", pattern = "###-######"),
            "EH" to Metadata(prefix = "+212", regionCode = "EH", pattern = "###-######"),
            "DZ" to Metadata(prefix = "+213", regionCode = "DZ", pattern = "### ## ## ##"),
            "TN" to Metadata(prefix = "+216", regionCode = "TN", pattern = "## ### ###"),
            "LY" to Metadata(prefix = "+218", regionCode = "LY", pattern = "##-#######"),
            "GM" to Metadata(prefix = "+220", regionCode = "GM", pattern = "### ####"),
            "SN" to Metadata(prefix = "+221", regionCode = "SN", pattern = "## ### ## ##"),
            "MR" to Metadata(prefix = "+222", regionCode = "MR", pattern = "## ## ## ##"),
            "ML" to Metadata(prefix = "+223", regionCode = "ML", pattern = "## ## ## ##"),
            "GN" to Metadata(prefix = "+224", regionCode = "GN", pattern = "### ## ## ##"),
            "CI" to Metadata(prefix = "+225", regionCode = "CI", pattern = "## ## ## ##"),
            "BF" to Metadata(prefix = "+226", regionCode = "BF", pattern = "## ## ## ##"),
            "NE" to Metadata(prefix = "+227", regionCode = "NE", pattern = "## ## ## ##"),
            "TG" to Metadata(prefix = "+228", regionCode = "TG", pattern = "## ## ## ##"),
            "BJ" to Metadata(prefix = "+229", regionCode = "BJ", pattern = "## ## ## ##"),
            "MU" to Metadata(prefix = "+230", regionCode = "MU", pattern = "#### ####"),
            "LR" to Metadata(prefix = "+231", regionCode = "LR", pattern = "### ### ###"),
            "SL" to Metadata(prefix = "+232", regionCode = "SL", pattern = "## ######"),
            "GH" to Metadata(prefix = "+233", regionCode = "GH", pattern = "## ### ####"),
            "NG" to Metadata(prefix = "+234", regionCode = "NG", pattern = "### ### ####"),
            "TD" to Metadata(prefix = "+235", regionCode = "TD", pattern = "## ## ## ##"),
            "CF" to Metadata(prefix = "+236", regionCode = "CF", pattern = "## ## ## ##"),
            "CM" to Metadata(prefix = "+237", regionCode = "CM", pattern = "## ## ## ##"),
            "CV" to Metadata(prefix = "+238", regionCode = "CV", pattern = "### ## ##"),
            "ST" to Metadata(prefix = "+239", regionCode = "ST", pattern = "### ####"),
            "GQ" to Metadata(prefix = "+240", regionCode = "GQ", pattern = "### ### ###"),
            "GA" to Metadata(prefix = "+241", regionCode = "GA", pattern = "## ## ## ##"),
            "CG" to Metadata(prefix = "+242", regionCode = "CG", pattern = "## ### ####"),
            "CD" to Metadata(prefix = "+243", regionCode = "CD", pattern = "### ### ###"),
            "AO" to Metadata(prefix = "+244", regionCode = "AO", pattern = "### ### ###"),
            "GW" to Metadata(prefix = "+245", regionCode = "GW", pattern = "### ####"),
            "IO" to Metadata(prefix = "+246", regionCode = "IO", pattern = "### ####"),
            "AC" to Metadata(prefix = "+247", regionCode = "AC"),
            "SC" to Metadata(prefix = "+248", regionCode = "SC", pattern = "# ### ###"),
            "RW" to Metadata(prefix = "+250", regionCode = "RW", pattern = "### ### ###"),
            "ET" to Metadata(prefix = "+251", regionCode = "ET", pattern = "## ### ####"),
            "SO" to Metadata(prefix = "+252", regionCode = "SO", pattern = "## #######"),
            "DJ" to Metadata(prefix = "+253", regionCode = "DJ", pattern = "## ## ## ##"),
            "KE" to Metadata(prefix = "+254", regionCode = "KE", pattern = "## #######"),
            "TZ" to Metadata(prefix = "+255", regionCode = "TZ", pattern = "### ### ###"),
            "UG" to Metadata(prefix = "+256", regionCode = "UG", pattern = "### ######"),
            "BI" to Metadata(prefix = "+257", regionCode = "BI", pattern = "## ## ## ##"),
            "MZ" to Metadata(prefix = "+258", regionCode = "MZ", pattern = "## ### ####"),
            "ZM" to Metadata(prefix = "+260", regionCode = "ZM", pattern = "## #######"),
            "MG" to Metadata(prefix = "+261", regionCode = "MG", pattern = "## ## ### ##"),
            "RE" to Metadata(prefix = "+262", regionCode = "RE"),
            "TF" to Metadata(prefix = "+262", regionCode = "TF"),
            "YT" to Metadata(prefix = "+262", regionCode = "YT", pattern = "### ## ## ##"),
            "ZW" to Metadata(prefix = "+263", regionCode = "ZW", pattern = "## ### ####"),
            "NA" to Metadata(prefix = "+264", regionCode = "NA", pattern = "## ### ####"),
            "MW" to Metadata(prefix = "+265", regionCode = "MW", pattern = "### ## ## ##"),
            "LS" to Metadata(prefix = "+266", regionCode = "LS", pattern = "#### ####"),
            "BW" to Metadata(prefix = "+267", regionCode = "BW", pattern = "## ### ###"),
            "SZ" to Metadata(prefix = "+268", regionCode = "SZ", pattern = "#### ####"),
            "KM" to Metadata(prefix = "+269", regionCode = "KM", pattern = "### ## ##"),
            "ZA" to Metadata(prefix = "+27", regionCode = "ZA", pattern = "## ### ####"),
            "SH" to Metadata(prefix = "+290", regionCode = "SH"),
            "TA" to Metadata(prefix = "+290", regionCode = "TA"),
            "ER" to Metadata(prefix = "+291", regionCode = "ER", pattern = "# ### ###"),
            "AW" to Metadata(prefix = "+297", regionCode = "AW", pattern = "### ####"),
            "FO" to Metadata(prefix = "+298", regionCode = "FO", pattern = "######"),
            "GL" to Metadata(prefix = "+299", regionCode = "GL", pattern = "## ## ##"),
            "GR" to Metadata(prefix = "+30", regionCode = "GR", pattern = "### ### ####"),
            "NL" to Metadata(prefix = "+31", regionCode = "NL", pattern = "# ########"),
            "BE" to Metadata(prefix = "+32", regionCode = "BE", pattern = "### ## ## ##"),
            "FR" to Metadata(prefix = "+33", regionCode = "FR", pattern = "# ## ## ## ##"),
            "ES" to Metadata(prefix = "+34", regionCode = "ES", pattern = "### ## ## ##"),
            "GI" to Metadata(prefix = "+350", regionCode = "GI", pattern = "### #####"),
            "PT" to Metadata(prefix = "+351", regionCode = "PT", pattern = "### ### ###"),
            "LU" to Metadata(prefix = "+352", regionCode = "LU", pattern = "## ## ## ###"),
            "IE" to Metadata(prefix = "+353", regionCode = "IE", pattern = "## ### ####"),
            "IS" to Metadata(prefix = "+354", regionCode = "IS", pattern = "### ####"),
            "AL" to Metadata(prefix = "+355", regionCode = "AL", pattern = "## ### ####"),
            "MT" to Metadata(prefix = "+356", regionCode = "MT", pattern = "#### ####"),
            "CY" to Metadata(prefix = "+357", regionCode = "CY", pattern = "## ######"),
            "FI" to Metadata(prefix = "+358", regionCode = "FI", pattern = "## ### ## ##"),
            "AX" to Metadata(prefix = "+358", regionCode = "AX"),
            "BG" to Metadata(prefix = "+359", regionCode = "BG", pattern = "### ### ##"),
            "HU" to Metadata(prefix = "+36", regionCode = "HU", pattern = "## ### ####"),
            "LT" to Metadata(prefix = "+370", regionCode = "LT", pattern = "### #####"),
            "LV" to Metadata(prefix = "+371", regionCode = "LV", pattern = "## ### ###"),
            "EE" to Metadata(prefix = "+372", regionCode = "EE", pattern = "#### ####"),
            "MD" to Metadata(prefix = "+373", regionCode = "MD", pattern = "### ## ###"),
            "AM" to Metadata(prefix = "+374", regionCode = "AM", pattern = "## ######"),
            "BY" to Metadata(prefix = "+375", regionCode = "BY", pattern = "## ###-##-##"),
            "AD" to Metadata(prefix = "+376", regionCode = "AD", pattern = "### ###"),
            "MC" to Metadata(prefix = "+377", regionCode = "MC", pattern = "# ## ## ## ##"),
            "SM" to Metadata(prefix = "+378", regionCode = "SM", pattern = "## ## ## ##"),
            "VA" to Metadata(prefix = "+379", regionCode = "VA"),
            "UA" to Metadata(prefix = "+380", regionCode = "UA", pattern = "## ### ####"),
            "RS" to Metadata(prefix = "+381", regionCode = "RS", pattern = "## #######"),
            "ME" to Metadata(prefix = "+382", regionCode = "ME", pattern = "## ### ###"),
            "XK" to Metadata(prefix = "+383", regionCode = "XK", pattern = "## ### ###"),
            "HR" to Metadata(prefix = "+385", regionCode = "HR", pattern = "## ### ####"),
            "SI" to Metadata(prefix = "+386", regionCode = "SI", pattern = "## ### ###"),
            "BA" to Metadata(prefix = "+387", regionCode = "BA", pattern = "## ###-###"),
            "MK" to Metadata(prefix = "+389", regionCode = "MK", pattern = "## ### ###"),
            "IT" to Metadata(prefix = "+39", regionCode = "IT", pattern = "## #### ####"),
            "RO" to Metadata(prefix = "+40", regionCode = "RO", pattern = "## ### ####"),
            "CH" to Metadata(prefix = "+41", regionCode = "CH", pattern = "## ### ## ##"),
            "CZ" to Metadata(prefix = "+420", regionCode = "CZ", pattern = "### ### ###"),
            "SK" to Metadata(prefix = "+421", regionCode = "SK", pattern = "### ### ###"),
            "LI" to Metadata(prefix = "+423", regionCode = "LI", pattern = "### ### ###"),
            "AT" to Metadata(prefix = "+43", regionCode = "AT", pattern = "### ######"),
            "GB" to Metadata(prefix = "+44", regionCode = "GB", pattern = "#### ######"),
            "GG" to Metadata(prefix = "+44", regionCode = "GG", pattern = "#### ######"),
            "JE" to Metadata(prefix = "+44", regionCode = "JE", pattern = "#### ######"),
            "IM" to Metadata(prefix = "+44", regionCode = "IM", pattern = "#### ######"),
            "DK" to Metadata(prefix = "+45", regionCode = "DK", pattern = "## ## ## ##"),
            "SE" to Metadata(prefix = "+46", regionCode = "SE", pattern = "##-### ## ##"),
            "NO" to Metadata(prefix = "+47", regionCode = "NO", pattern = "### ## ###"),
            "BV" to Metadata(prefix = "+47", regionCode = "BV"),
            "SJ" to Metadata(prefix = "+47", regionCode = "SJ", pattern = "## ## ## ##"),
            "PL" to Metadata(prefix = "+48", regionCode = "PL", pattern = "## ### ## ##"),
            "DE" to Metadata(prefix = "+49", regionCode = "DE", pattern = "### #######"),
            "FK" to Metadata(prefix = "+500", regionCode = "FK"),
            "GS" to Metadata(prefix = "+500", regionCode = "GS"),
            "BZ" to Metadata(prefix = "+501", regionCode = "BZ", pattern = "###-####"),
            "GT" to Metadata(prefix = "+502", regionCode = "GT", pattern = "#### ####"),
            "SV" to Metadata(prefix = "+503", regionCode = "SV", pattern = "#### ####"),
            "HN" to Metadata(prefix = "+504", regionCode = "HN", pattern = "####-####"),
            "NI" to Metadata(prefix = "+505", regionCode = "NI", pattern = "#### ####"),
            "CR" to Metadata(prefix = "+506", regionCode = "CR", pattern = "#### ####"),
            "PA" to Metadata(prefix = "+507", regionCode = "PA", pattern = "####-####"),
            "PM" to Metadata(prefix = "+508", regionCode = "PM", pattern = "## ## ##"),
            "HT" to Metadata(prefix = "+509", regionCode = "HT", pattern = "## ## ####"),
            "PE" to Metadata(prefix = "+51", regionCode = "PE", pattern = "### ### ###"),
            "MX" to Metadata(prefix = "+52", regionCode = "MX", pattern = "### ### ####"),
            "AR" to Metadata(prefix = "+54", regionCode = "AR", pattern = "## ##-####-####"),
            "BR" to Metadata(prefix = "+55", regionCode = "BR", pattern = "## #####-####"),
            "CL" to Metadata(prefix = "+56", regionCode = "CL", pattern = "# #### ####"),
            "CO" to Metadata(prefix = "+57", regionCode = "CO", pattern = "### #######"),
            "VE" to Metadata(prefix = "+58", regionCode = "VE", pattern = "###-#######"),
            "BL" to Metadata(prefix = "+590", regionCode = "BL", pattern = "### ## ## ##"),
            "MF" to Metadata(prefix = "+590", regionCode = "MF"),
            "GP" to Metadata(prefix = "+590", regionCode = "GP", pattern = "### ## ## ##"),
            "BO" to Metadata(prefix = "+591", regionCode = "BO", pattern = "########"),
            "GY" to Metadata(prefix = "+592", regionCode = "GY", pattern = "### ####"),
            "EC" to Metadata(prefix = "+593", regionCode = "EC", pattern = "## ### ####"),
            "GF" to Metadata(prefix = "+594", regionCode = "GF", pattern = "### ## ## ##"),
            "PY" to Metadata(prefix = "+595", regionCode = "PY", pattern = "## #######"),
            "MQ" to Metadata(prefix = "+596", regionCode = "MQ", pattern = "### ## ## ##"),
            "SR" to Metadata(prefix = "+597", regionCode = "SR", pattern = "###-####"),
            "UY" to Metadata(prefix = "+598", regionCode = "UY", pattern = "#### ####"),
            "CW" to Metadata(prefix = "+599", regionCode = "CW", pattern = "# ### ####"),
            "BQ" to Metadata(prefix = "+599", regionCode = "BQ", pattern = "### ####"),
            "MY" to Metadata(prefix = "+60", regionCode = "MY", pattern = "##-### ####"),
            "AU" to Metadata(prefix = "+61", regionCode = "AU", pattern = "### ### ###"),
            "ID" to Metadata(prefix = "+62", regionCode = "ID", pattern = "###-###-###"),
            "PH" to Metadata(prefix = "+63", regionCode = "PH", pattern = "#### ######"),
            "NZ" to Metadata(prefix = "+64", regionCode = "NZ", pattern = "## ### ####"),
            "SG" to Metadata(prefix = "+65", regionCode = "SG", pattern = "#### ####"),
            "TH" to Metadata(prefix = "+66", regionCode = "TH", pattern = "## ### ####"),
            "TL" to Metadata(prefix = "+670", regionCode = "TL", pattern = "#### ####"),
            "AQ" to Metadata(prefix = "+672", regionCode = "AQ", pattern = "## ####"),
            "BN" to Metadata(prefix = "+673", regionCode = "BN", pattern = "### ####"),
            "NR" to Metadata(prefix = "+674", regionCode = "NR", pattern = "### ####"),
            "PG" to Metadata(prefix = "+675", regionCode = "PG", pattern = "### ####"),
            "TO" to Metadata(prefix = "+676", regionCode = "TO", pattern = "### ####"),
            "SB" to Metadata(prefix = "+677", regionCode = "SB", pattern = "### ####"),
            "VU" to Metadata(prefix = "+678", regionCode = "VU", pattern = "### ####"),
            "FJ" to Metadata(prefix = "+679", regionCode = "FJ", pattern = "### ####"),
            "WF" to Metadata(prefix = "+681", regionCode = "WF", pattern = "## ## ##"),
            "CK" to Metadata(prefix = "+682", regionCode = "CK", pattern = "## ###"),
            "NU" to Metadata(prefix = "+683", regionCode = "NU"),
            "WS" to Metadata(prefix = "+685", regionCode = "WS"),
            "KI" to Metadata(prefix = "+686", regionCode = "KI"),
            "NC" to Metadata(prefix = "+687", regionCode = "NC", pattern = "########"),
            "TV" to Metadata(prefix = "+688", regionCode = "TV"),
            "PF" to Metadata(prefix = "+689", regionCode = "PF", pattern = "## ## ##"),
            "TK" to Metadata(prefix = "+690", regionCode = "TK"),
            "RU" to Metadata(prefix = "+7", regionCode = "RU", pattern = "### ###-##-##"),
            "KZ" to Metadata(prefix = "+7", regionCode = "KZ"),
            "JP" to Metadata(prefix = "+81", regionCode = "JP", pattern = "##-####-####"),
            "KR" to Metadata(prefix = "+82", regionCode = "KR", pattern = "##-####-####"),
            "VN" to Metadata(prefix = "+84", regionCode = "VN", pattern = "## ### ## ##"),
            "HK" to Metadata(prefix = "+852", regionCode = "HK", pattern = "#### ####"),
            "MO" to Metadata(prefix = "+853", regionCode = "MO", pattern = "#### ####"),
            "KH" to Metadata(prefix = "+855", regionCode = "KH", pattern = "## ### ###"),
            "LA" to Metadata(prefix = "+856", regionCode = "LA", pattern = "## ## ### ###"),
            "CN" to Metadata(prefix = "+86", regionCode = "CN", pattern = "### #### ####"),
            "PN" to Metadata(prefix = "+872", regionCode = "PN"),
            "BD" to Metadata(prefix = "+880", regionCode = "BD", pattern = "####-######"),
            "TW" to Metadata(prefix = "+886", regionCode = "TW", pattern = "### ### ###"),
            "TR" to Metadata(prefix = "+90", regionCode = "TR", pattern = "### ### ####"),
            "IN" to Metadata(prefix = "+91", regionCode = "IN", pattern = "## ## ######"),
            "PK" to Metadata(prefix = "+92", regionCode = "PK", pattern = "### #######"),
            "AF" to Metadata(prefix = "+93", regionCode = "AF", pattern = "## ### ####"),
            "LK" to Metadata(prefix = "+94", regionCode = "LK", pattern = "## # ######"),
            "MM" to Metadata(prefix = "+95", regionCode = "MM", pattern = "# ### ####"),
            "MV" to Metadata(prefix = "+960", regionCode = "MV", pattern = "###-####"),
            "LB" to Metadata(prefix = "+961", regionCode = "LB", pattern = "## ### ###"),
            "JO" to Metadata(prefix = "+962", regionCode = "JO", pattern = "# #### ####"),
            "IQ" to Metadata(prefix = "+964", regionCode = "IQ", pattern = "### ### ####"),
            "KW" to Metadata(prefix = "+965", regionCode = "KW", pattern = "### #####"),
            "SA" to Metadata(prefix = "+966", regionCode = "SA", pattern = "## ### ####"),
            "YE" to Metadata(prefix = "+967", regionCode = "YE", pattern = "### ### ###"),
            "OM" to Metadata(prefix = "+968", regionCode = "OM", pattern = "#### ####"),
            "PS" to Metadata(prefix = "+970", regionCode = "PS", pattern = "### ### ###"),
            "AE" to Metadata(prefix = "+971", regionCode = "AE", pattern = "## ### ####"),
            "IL" to Metadata(prefix = "+972", regionCode = "IL", pattern = "##-###-####"),
            "BH" to Metadata(prefix = "+973", regionCode = "BH", pattern = "#### ####"),
            "QA" to Metadata(prefix = "+974", regionCode = "QA", pattern = "#### ####"),
            "BT" to Metadata(prefix = "+975", regionCode = "BT", pattern = "## ## ## ##"),
            "MN" to Metadata(prefix = "+976", regionCode = "MN", pattern = "#### ####"),
            "NP" to Metadata(prefix = "+977", regionCode = "NP", pattern = "###-#######"),
            "TJ" to Metadata(prefix = "+992", regionCode = "TJ", pattern = "### ## ####"),
            "TM" to Metadata(prefix = "+993", regionCode = "TM", pattern = "## ##-##-##"),
            "AZ" to Metadata(prefix = "+994", regionCode = "AZ", pattern = "## ### ## ##"),
            "GE" to Metadata(prefix = "+995", regionCode = "GE", pattern = "### ## ## ##"),
            "KG" to Metadata(prefix = "+996", regionCode = "KG", pattern = "### ### ###"),
            "UZ" to Metadata(prefix = "+998", regionCode = "UZ", pattern = "## ### ## ##")
        )
    }
}
