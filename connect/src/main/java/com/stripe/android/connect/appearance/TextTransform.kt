package com.stripe.android.connect.appearance

import com.stripe.android.connect.PrivateBetaConnectSDK

@PrivateBetaConnectSDK
enum class TextTransform {
    /**
     * No text transformation applied.
     */
    None,

    /**
     * Text displayed in all uppercase characters.
     */
    Uppercase,

    /**
     * Text displayed in all lowercase characters.
     */
    Lowercase,

    /**
     * Text displayed with the first character capitalized.
     */
    Capitalize
}
