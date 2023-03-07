import com.stripe.android.model.ConsumerSession

/**
 * Mask the phone number to show only the last 4 digits.
 */
@Suppress("MagicNumber")
internal fun ConsumerSession.getRedactedPhoneNumber(): String {
    val number = redactedPhoneNumber.replace("*", "•")
    val formattedPhoneNumber = buildString {
        append(number.substring(0, 2))
        append("(")
        append(number.substring(2, 5))
        append(")")
        append(number.substring(5))
    }
    return buildString {
        append(formattedPhoneNumber.substring(0, 7))
        append(" ")
        append(formattedPhoneNumber.substring(7, 10))
        append(" ")
        append(formattedPhoneNumber.substring(10))
    }
}
