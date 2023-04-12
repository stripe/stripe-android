package com.stripe.android.test.core.ui

import com.stripe.android.test.core.Browser

sealed class BrowserUI(val name: String, val packageName: String, val resourceID: String) {
    object Chrome : BrowserUI(
        "Chrome",
        "com.android.chrome",
        "com.android.chrome:id/coordinator"
    )

    object Firefox : BrowserUI(
        "Firefox",
        "org.mozilla.firefox",
        "org.mozilla.firefox:id/action_bar_root"
    )

    companion object {
        fun values() = setOf(Chrome, Firefox)
        fun convert(browser: Browser?): BrowserUI? {
            return when (browser) {
                Browser.Chrome -> Chrome
                Browser.Firefox -> Firefox
                else -> null
            }
        }
    }
}
