package com.stripe.android.identity.utils

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.core.text.HtmlCompat

/**
 * Set a html string to this TextView and make links clickable.
 */
internal fun TextView.setHtmlString(htmlString: String) {
    this.text = HtmlCompat.fromHtml(htmlString, HtmlCompat.FROM_HTML_MODE_LEGACY)
    this.movementMethod = LinkMovementMethod.getInstance()
}
