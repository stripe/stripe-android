package com.stripe.android.uicore.text

import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.utils.PaparazziRule
import com.stripe.android.uicore.utils.SystemAppearance
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class HtmlScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values()
    )

    @Test
    @Ignore("Until fixed by https://issuetracker.google.com/issues/262773698")
    fun testBold() {
        paparazziRule.snapshot {
            StripeTheme {
                Html(html = "this is some <b>bold</b> text")
            }
        }
    }

    @Test
    fun testItalic() {
        paparazziRule.snapshot {
            StripeTheme {
                Html(html = "this is some <i>italic</i> text")
            }
        }
    }

    @Test
    fun testUnderline() {
        paparazziRule.snapshot {
            StripeTheme {
                Html(html = "this is some <u>underline</u> text")
            }
        }
    }

    @Test
    fun testAnchor() {
        paparazziRule.snapshot {
            StripeTheme {
                Html(html = "this is some <a href='stripe.com'>link</a> text")
            }
        }
    }

    @Test
    fun testListItem() {
        paparazziRule.snapshot {
            StripeTheme {
                Html(html = "these are some list items: <li>item1</li><li>item2</li><li>item3</li>")
            }
        }
    }
}
