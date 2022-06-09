package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
        override val placeholder = metadata.pattern.replace('#', '5')
        override val countryCode = metadata.regionCode

        // Maximum number of digits for the subscriber number for this region.
        private val maxSubscriberDigits = E164_MAX_DIGITS -
            (prefix.length - 1) // prefix minus the '+'

        override fun userInputFilter(input: String) =
            input.filter { VALID_INPUT_RANGE.contains(it) }.run {
                substring(0, min(length, maxSubscriberDigits))
            }

        override fun toE164Format(input: String) = "${prefix}${userInputFilter(input)}"

        override val visualTransformation = object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                // Already filtered in onValueChange
                val formatted = formatNumberNational(text.text)

                return TransformedText(
                    AnnotatedString(formatted),
                    object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int {
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
                            return if (offset == 0) 0 else metadata.pattern.let {
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
                )
            }
        }

        /**
         * Format the phone number to the national format, assuming the input was already filtered
         * for invalid characters and the numbers of characters limited based on E.164 spec.
         */
        fun formatNumberNational(filteredInput: String): String {
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

        override fun toE164Format(input: String) = "+${userInputFilter(input)}"

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
        val pattern: String
    )

    companion object {
        const val E164_MAX_DIGITS = 15
        val VALID_INPUT_RANGE = ('0'..'9')

        fun forCountry(countryCode: String) =
            allMetadata.find { countryCode.uppercase() == it.regionCode }?.let {
                WithRegion(it)
            } ?: UnknownRegion(countryCode)

        // List shared with iOS: https://github.com/stripe/stripe-ios/blob/master/StripeUICore/StripeUICore/Source/Validators/PhoneNumber.swift
        private val allMetadata = listOf(
            // NANP member countries and territories (Zone 1)
            // https://en.wikipedia.org/wiki/North_American_Numbering_Plan#Countries_and_territories
            Metadata(prefix = "+1", regionCode = "US", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "CA", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "AG", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "AS", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "AI", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "BB", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "BM", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "BS", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "DM", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "DO", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "GD", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "GU", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "JM", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "KN", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "KY", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "LC", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "MP", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "MS", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "PR", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "SX", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "TC", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "TT", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "VC", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "VG", pattern = "(###) ###-####"),
            Metadata(prefix = "+1", regionCode = "VI", pattern = "(###) ###-####"),
            // Rest of the world
            Metadata(prefix = "+20", regionCode = "EG", pattern = "### ### ####"),
            Metadata(prefix = "+211", regionCode = "SS", pattern = "### ### ###"),
            Metadata(prefix = "+212", regionCode = "MA", pattern = "###-######"),
            Metadata(prefix = "+212", regionCode = "EH", pattern = "###-######"),
            Metadata(prefix = "+213", regionCode = "DZ", pattern = "### ## ## ##"),
            Metadata(prefix = "+216", regionCode = "TN", pattern = "## ### ###"),
            Metadata(prefix = "+218", regionCode = "LY", pattern = "##-#######"),
            Metadata(prefix = "+220", regionCode = "GM", pattern = "### ####"),
            Metadata(prefix = "+221", regionCode = "SN", pattern = "## ### ## ##"),
            Metadata(prefix = "+222", regionCode = "MR", pattern = "## ## ## ##"),
            Metadata(prefix = "+223", regionCode = "ML", pattern = "## ## ## ##"),
            Metadata(prefix = "+224", regionCode = "GN", pattern = "### ## ## ##"),
            Metadata(prefix = "+225", regionCode = "CI", pattern = "## ## ## ##"),
            Metadata(prefix = "+226", regionCode = "BF", pattern = "## ## ## ##"),
            Metadata(prefix = "+227", regionCode = "NE", pattern = "## ## ## ##"),
            Metadata(prefix = "+228", regionCode = "TG", pattern = "## ## ## ##"),
            Metadata(prefix = "+229", regionCode = "BJ", pattern = "## ## ## ##"),
            Metadata(prefix = "+230", regionCode = "MU", pattern = "#### ####"),
            Metadata(prefix = "+231", regionCode = "LR", pattern = "### ### ###"),
            Metadata(prefix = "+232", regionCode = "SL", pattern = "## ######"),
            Metadata(prefix = "+233", regionCode = "GH", pattern = "## ### ####"),
            Metadata(prefix = "+234", regionCode = "NG", pattern = "### ### ####"),
            Metadata(prefix = "+235", regionCode = "TD", pattern = "## ## ## ##"),
            Metadata(prefix = "+236", regionCode = "CF", pattern = "## ## ## ##"),
            Metadata(prefix = "+237", regionCode = "CM", pattern = "## ## ## ##"),
            Metadata(prefix = "+238", regionCode = "CV", pattern = "### ## ##"),
            Metadata(prefix = "+239", regionCode = "ST", pattern = "### ####"),
            Metadata(prefix = "+240", regionCode = "GQ", pattern = "### ### ###"),
            Metadata(prefix = "+241", regionCode = "GA", pattern = "## ## ## ##"),
            Metadata(prefix = "+242", regionCode = "CG", pattern = "## ### ####"),
            Metadata(prefix = "+243", regionCode = "CD", pattern = "### ### ###"),
            Metadata(prefix = "+244", regionCode = "AO", pattern = "### ### ###"),
            Metadata(prefix = "+245", regionCode = "GW", pattern = "### ####"),
            Metadata(prefix = "+246", regionCode = "IO", pattern = "### ####"),
            Metadata(prefix = "+247", regionCode = "AC", pattern = ""),
            Metadata(prefix = "+248", regionCode = "SC", pattern = "# ### ###"),
            Metadata(prefix = "+250", regionCode = "RW", pattern = "### ### ###"),
            Metadata(prefix = "+251", regionCode = "ET", pattern = "## ### ####"),
            Metadata(prefix = "+252", regionCode = "SO", pattern = "## #######"),
            Metadata(prefix = "+253", regionCode = "DJ", pattern = "## ## ## ##"),
            Metadata(prefix = "+254", regionCode = "KE", pattern = "## #######"),
            Metadata(prefix = "+255", regionCode = "TZ", pattern = "### ### ###"),
            Metadata(prefix = "+256", regionCode = "UG", pattern = "### ######"),
            Metadata(prefix = "+257", regionCode = "BI", pattern = "## ## ## ##"),
            Metadata(prefix = "+258", regionCode = "MZ", pattern = "## ### ####"),
            Metadata(prefix = "+260", regionCode = "ZM", pattern = "## #######"),
            Metadata(prefix = "+261", regionCode = "MG", pattern = "## ## ### ##"),
            Metadata(prefix = "+262", regionCode = "RE", pattern = ""),
            Metadata(prefix = "+262", regionCode = "TF", pattern = ""),
            Metadata(prefix = "+262", regionCode = "YT", pattern = "### ## ## ##"),
            Metadata(prefix = "+263", regionCode = "ZW", pattern = "## ### ####"),
            Metadata(prefix = "+264", regionCode = "NA", pattern = "## ### ####"),
            Metadata(prefix = "+265", regionCode = "MW", pattern = "### ## ## ##"),
            Metadata(prefix = "+266", regionCode = "LS", pattern = "#### ####"),
            Metadata(prefix = "+267", regionCode = "BW", pattern = "## ### ###"),
            Metadata(prefix = "+268", regionCode = "SZ", pattern = "#### ####"),
            Metadata(prefix = "+269", regionCode = "KM", pattern = "### ## ##"),
            Metadata(prefix = "+27", regionCode = "ZA", pattern = "## ### ####"),
            Metadata(prefix = "+290", regionCode = "SH", pattern = ""),
            Metadata(prefix = "+290", regionCode = "TA", pattern = ""),
            Metadata(prefix = "+291", regionCode = "ER", pattern = "# ### ###"),
            Metadata(prefix = "+297", regionCode = "AW", pattern = "### ####"),
            Metadata(prefix = "+298", regionCode = "FO", pattern = "######"),
            Metadata(prefix = "+299", regionCode = "GL", pattern = "## ## ##"),
            Metadata(prefix = "+30", regionCode = "GR", pattern = "### ### ####"),
            Metadata(prefix = "+31", regionCode = "NL", pattern = "# ########"),
            Metadata(prefix = "+32", regionCode = "BE", pattern = "### ## ## ##"),
            Metadata(prefix = "+33", regionCode = "FR", pattern = "# ## ## ## ##"),
            Metadata(prefix = "+34", regionCode = "ES", pattern = "### ## ## ##"),
            Metadata(prefix = "+350", regionCode = "GI", pattern = "### #####"),
            Metadata(prefix = "+351", regionCode = "PT", pattern = "### ### ###"),
            Metadata(prefix = "+352", regionCode = "LU", pattern = "## ## ## ###"),
            Metadata(prefix = "+353", regionCode = "IE", pattern = "## ### ####"),
            Metadata(prefix = "+354", regionCode = "IS", pattern = "### ####"),
            Metadata(prefix = "+355", regionCode = "AL", pattern = "## ### ####"),
            Metadata(prefix = "+356", regionCode = "MT", pattern = "#### ####"),
            Metadata(prefix = "+357", regionCode = "CY", pattern = "## ######"),
            Metadata(prefix = "+358", regionCode = "FI", pattern = "## ### ## ##"),
            Metadata(prefix = "+358", regionCode = "AX", pattern = ""),
            Metadata(prefix = "+359", regionCode = "BG", pattern = "### ### ##"),
            Metadata(prefix = "+36", regionCode = "HU", pattern = "## ### ####"),
            Metadata(prefix = "+370", regionCode = "LT", pattern = "### #####"),
            Metadata(prefix = "+371", regionCode = "LV", pattern = "## ### ###"),
            Metadata(prefix = "+372", regionCode = "EE", pattern = "#### ####"),
            Metadata(prefix = "+373", regionCode = "MD", pattern = "### ## ###"),
            Metadata(prefix = "+374", regionCode = "AM", pattern = "## ######"),
            Metadata(prefix = "+375", regionCode = "BY", pattern = "## ###-##-##"),
            Metadata(prefix = "+376", regionCode = "AD", pattern = "### ###"),
            Metadata(prefix = "+377", regionCode = "MC", pattern = "# ## ## ## ##"),
            Metadata(prefix = "+378", regionCode = "SM", pattern = "## ## ## ##"),
            Metadata(prefix = "+379", regionCode = "VA", pattern = ""),
            Metadata(prefix = "+380", regionCode = "UA", pattern = "## ### ####"),
            Metadata(prefix = "+381", regionCode = "RS", pattern = "## #######"),
            Metadata(prefix = "+382", regionCode = "ME", pattern = "## ### ###"),
            Metadata(prefix = "+383", regionCode = "XK", pattern = "## ### ###"),
            Metadata(prefix = "+385", regionCode = "HR", pattern = "## ### ####"),
            Metadata(prefix = "+386", regionCode = "SI", pattern = "## ### ###"),
            Metadata(prefix = "+387", regionCode = "BA", pattern = "## ###-###"),
            Metadata(prefix = "+389", regionCode = "MK", pattern = "## ### ###"),
            Metadata(prefix = "+39", regionCode = "IT", pattern = "## #### ####"),
            Metadata(prefix = "+40", regionCode = "RO", pattern = "## ### ####"),
            Metadata(prefix = "+41", regionCode = "CH", pattern = "## ### ## ##"),
            Metadata(prefix = "+420", regionCode = "CZ", pattern = "### ### ###"),
            Metadata(prefix = "+421", regionCode = "SK", pattern = "### ### ###"),
            Metadata(prefix = "+423", regionCode = "LI", pattern = "### ### ###"),
            Metadata(prefix = "+43", regionCode = "AT", pattern = "### ######"),
            Metadata(prefix = "+44", regionCode = "GB", pattern = "#### ######"),
            Metadata(prefix = "+44", regionCode = "GG", pattern = "#### ######"),
            Metadata(prefix = "+44", regionCode = "JE", pattern = "#### ######"),
            Metadata(prefix = "+44", regionCode = "IM", pattern = "#### ######"),
            Metadata(prefix = "+45", regionCode = "DK", pattern = "## ## ## ##"),
            Metadata(prefix = "+46", regionCode = "SE", pattern = "##-### ## ##"),
            Metadata(prefix = "+47", regionCode = "NO", pattern = "### ## ###"),
            Metadata(prefix = "+47", regionCode = "BV", pattern = ""),
            Metadata(prefix = "+47", regionCode = "SJ", pattern = "## ## ## ##"),
            Metadata(prefix = "+48", regionCode = "PL", pattern = "## ### ## ##"),
            Metadata(prefix = "+49", regionCode = "DE", pattern = "### #######"),
            Metadata(prefix = "+500", regionCode = "FK", pattern = ""),
            Metadata(prefix = "+500", regionCode = "GS", pattern = ""),
            Metadata(prefix = "+501", regionCode = "BZ", pattern = "###-####"),
            Metadata(prefix = "+502", regionCode = "GT", pattern = "#### ####"),
            Metadata(prefix = "+503", regionCode = "SV", pattern = "#### ####"),
            Metadata(prefix = "+504", regionCode = "HN", pattern = "####-####"),
            Metadata(prefix = "+505", regionCode = "NI", pattern = "#### ####"),
            Metadata(prefix = "+506", regionCode = "CR", pattern = "#### ####"),
            Metadata(prefix = "+507", regionCode = "PA", pattern = "####-####"),
            Metadata(prefix = "+508", regionCode = "PM", pattern = "## ## ##"),
            Metadata(prefix = "+509", regionCode = "HT", pattern = "## ## ####"),
            Metadata(prefix = "+51", regionCode = "PE", pattern = "### ### ###"),
            Metadata(prefix = "+52", regionCode = "MX", pattern = "### ### ### ####"),
            Metadata(prefix = "+537", regionCode = "CY", pattern = ""),
            Metadata(prefix = "+54", regionCode = "AR", pattern = "## ##-####-####"),
            Metadata(prefix = "+55", regionCode = "BR", pattern = "## #####-####"),
            Metadata(prefix = "+56", regionCode = "CL", pattern = "# #### ####"),
            Metadata(prefix = "+57", regionCode = "CO", pattern = "### #######"),
            Metadata(prefix = "+58", regionCode = "VE", pattern = "###-#######"),
            Metadata(prefix = "+590", regionCode = "BL", pattern = "### ## ## ##"),
            Metadata(prefix = "+590", regionCode = "MF", pattern = ""),
            Metadata(prefix = "+590", regionCode = "GP", pattern = "### ## ## ##"),
            Metadata(prefix = "+591", regionCode = "BO", pattern = "########"),
            Metadata(prefix = "+592", regionCode = "GY", pattern = "### ####"),
            Metadata(prefix = "+593", regionCode = "EC", pattern = "## ### ####"),
            Metadata(prefix = "+594", regionCode = "GF", pattern = "### ## ## ##"),
            Metadata(prefix = "+595", regionCode = "PY", pattern = "## #######"),
            Metadata(prefix = "+596", regionCode = "MQ", pattern = "### ## ## ##"),
            Metadata(prefix = "+597", regionCode = "SR", pattern = "###-####"),
            Metadata(prefix = "+598", regionCode = "UY", pattern = "#### ####"),
            Metadata(prefix = "+599", regionCode = "CW", pattern = "# ### ####"),
            Metadata(prefix = "+599", regionCode = "BQ", pattern = "### ####"),
            Metadata(prefix = "+60", regionCode = "MY", pattern = "##-### ####"),
            Metadata(prefix = "+61", regionCode = "AU", pattern = "### ### ###"),
            Metadata(prefix = "+62", regionCode = "ID", pattern = "###-###-###"),
            Metadata(prefix = "+63", regionCode = "PH", pattern = "#### ######"),
            Metadata(prefix = "+64", regionCode = "NZ", pattern = "## ### ####"),
            Metadata(prefix = "+65", regionCode = "SG", pattern = "#### ####"),
            Metadata(prefix = "+66", regionCode = "TH", pattern = "## ### ####"),
            Metadata(prefix = "+670", regionCode = "TL", pattern = "#### ####"),
            Metadata(prefix = "+672", regionCode = "AQ", pattern = "## ####"),
            Metadata(prefix = "+673", regionCode = "BN", pattern = "### ####"),
            Metadata(prefix = "+674", regionCode = "NR", pattern = "### ####"),
            Metadata(prefix = "+675", regionCode = "PG", pattern = "### ####"),
            Metadata(prefix = "+676", regionCode = "TO", pattern = "### ####"),
            Metadata(prefix = "+677", regionCode = "SB", pattern = "### ####"),
            Metadata(prefix = "+678", regionCode = "VU", pattern = "### ####"),
            Metadata(prefix = "+679", regionCode = "FJ", pattern = "### ####"),
            Metadata(prefix = "+681", regionCode = "WF", pattern = "## ## ##"),
            Metadata(prefix = "+682", regionCode = "CK", pattern = "## ###"),
            Metadata(prefix = "+683", regionCode = "NU", pattern = ""),
            Metadata(prefix = "+685", regionCode = "WS", pattern = ""),
            Metadata(prefix = "+686", regionCode = "KI", pattern = ""),
            Metadata(prefix = "+687", regionCode = "NC", pattern = "########"),
            Metadata(prefix = "+688", regionCode = "TV", pattern = ""),
            Metadata(prefix = "+689", regionCode = "PF", pattern = "## ## ##"),
            Metadata(prefix = "+690", regionCode = "TK", pattern = ""),
            Metadata(prefix = "+7", regionCode = "RU", pattern = "### ###-##-##"),
            Metadata(prefix = "+7", regionCode = "KZ", pattern = ""),
            Metadata(prefix = "+81", regionCode = "JP", pattern = "##-####-####"),
            Metadata(prefix = "+82", regionCode = "KR", pattern = "##-####-####"),
            Metadata(prefix = "+84", regionCode = "VN", pattern = "## ### ## ##"),
            Metadata(prefix = "+852", regionCode = "HK", pattern = "#### ####"),
            Metadata(prefix = "+853", regionCode = "MO", pattern = "#### ####"),
            Metadata(prefix = "+855", regionCode = "KH", pattern = "## ### ###"),
            Metadata(prefix = "+856", regionCode = "LA", pattern = "## ## ### ###"),
            Metadata(prefix = "+86", regionCode = "CN", pattern = "### #### ####"),
            Metadata(prefix = "+872", regionCode = "PN", pattern = ""),
            Metadata(prefix = "+880", regionCode = "BD", pattern = "####-######"),
            Metadata(prefix = "+886", regionCode = "TW", pattern = "### ### ###"),
            Metadata(prefix = "+90", regionCode = "TR", pattern = "### ### ####"),
            Metadata(prefix = "+91", regionCode = "IN", pattern = "## ## ######"),
            Metadata(prefix = "+92", regionCode = "PK", pattern = "### #######"),
            Metadata(prefix = "+93", regionCode = "AF", pattern = "## ### ####"),
            Metadata(prefix = "+94", regionCode = "LK", pattern = "## # ######"),
            Metadata(prefix = "+95", regionCode = "MM", pattern = "# ### ####"),
            Metadata(prefix = "+960", regionCode = "MV", pattern = "###-####"),
            Metadata(prefix = "+961", regionCode = "LB", pattern = "## ### ###"),
            Metadata(prefix = "+962", regionCode = "JO", pattern = "# #### ####"),
            Metadata(prefix = "+964", regionCode = "IQ", pattern = "### ### ####"),
            Metadata(prefix = "+965", regionCode = "KW", pattern = "### #####"),
            Metadata(prefix = "+966", regionCode = "SA", pattern = "## ### ####"),
            Metadata(prefix = "+967", regionCode = "YE", pattern = "### ### ###"),
            Metadata(prefix = "+968", regionCode = "OM", pattern = "#### ####"),
            Metadata(prefix = "+970", regionCode = "PS", pattern = "### ### ###"),
            Metadata(prefix = "+971", regionCode = "AE", pattern = "## ### ####"),
            Metadata(prefix = "+972", regionCode = "IL", pattern = "##-###-####"),
            Metadata(prefix = "+973", regionCode = "BH", pattern = "#### ####"),
            Metadata(prefix = "+974", regionCode = "QA", pattern = "#### ####"),
            Metadata(prefix = "+975", regionCode = "BT", pattern = "## ## ## ##"),
            Metadata(prefix = "+976", regionCode = "MN", pattern = "#### ####"),
            Metadata(prefix = "+977", regionCode = "NP", pattern = "###-#######"),
            Metadata(prefix = "+992", regionCode = "TJ", pattern = "### ## ####"),
            Metadata(prefix = "+993", regionCode = "TM", pattern = "## ##-##-##"),
            Metadata(prefix = "+994", regionCode = "AZ", pattern = "## ### ## ##"),
            Metadata(prefix = "+995", regionCode = "GE", pattern = "### ## ## ##"),
            Metadata(prefix = "+996", regionCode = "KG", pattern = "### ### ###"),
            Metadata(prefix = "+998", regionCode = "UZ", pattern = "## ### ## ##")
        )
    }
}
