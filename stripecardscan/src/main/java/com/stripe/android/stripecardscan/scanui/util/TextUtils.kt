package com.stripe.android.stripecardscan.scanui.util

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.widget.TextView

/**
 * Set a html string to this TextView and make links clickable.
 */
internal fun TextView.setHtmlString(htmlString: String) {
    var spannedText: Spanned? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        spannedText = Html.fromHtml(htmlString, Html.FROM_HTML_MODE_LEGACY)
    } else {
        spannedText = Html.fromHtml(htmlString)
    }
    this.text = spannedText
    this.movementMethod = LinkMovementMethod.getInstance()
}
