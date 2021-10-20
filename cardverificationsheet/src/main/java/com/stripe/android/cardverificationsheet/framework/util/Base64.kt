package com.stripe.android.cardverificationsheet.framework.util

import android.util.Base64
import java.nio.charset.Charset

fun b64Encode(s: String): String =
    Base64.encodeToString(
        s.toByteArray(Charset.defaultCharset()),
        Base64.URL_SAFE + Base64.NO_WRAP,
    )

fun b64Encode(b: ByteArray): String =
    Base64.encodeToString(b, Base64.URL_SAFE + Base64.NO_WRAP)
