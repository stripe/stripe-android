import com.stripe.android.model.ConsumerSession

/**
 * Mask the phone number to show only the last 4 digits.
 */
internal fun ConsumerSession.getRedactedPhoneNumber(): String {
    return redactedFormattedPhoneNumber.replace("*", "•")
}
