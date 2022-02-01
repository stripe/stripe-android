package com.stripe.android.stripe3ds2.transactions

enum class UiType constructor(
    val code: String,
    internal val requiresSubmitButton: Boolean
) {
    Text("01", true),
    SingleSelect("02", true),
    MultiSelect("03", true),

    /**
     * Out of band means the challenge will be resolved outside of the merchant's app (e.g. the
     * customer's banking app)
     */
    OutOfBand("04", false),

    Html("05", false);

    internal companion object {
        internal fun fromCode(code: String?): UiType? {
            return values().firstOrNull { code == it.code }
        }
    }
}
