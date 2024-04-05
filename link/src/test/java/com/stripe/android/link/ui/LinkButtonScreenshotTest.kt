package com.stripe.android.link.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.ElementsSession
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.After
import org.junit.Rule
import org.junit.Test

internal class LinkButtonScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        LinkRebrand.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @After
    fun resetUseNewBrand() {
        LinkUi.useNewBrand = ElementsSession.LinkSettings.useRebrandDefault
    }

    @Test
    fun testNewUser() {
        paparazziRule.snapshot {
            LinkButton(email = null, enabled = true, onClick = { })
        }
    }

    @Test
    fun testNewUserDisabled() {
        paparazziRule.snapshot {
            LinkButton(email = null, enabled = false, onClick = { })
        }
    }

    @Test
    fun testExistingUser() {
        paparazziRule.snapshot {
            LinkButton(email = "jaynewstrom@test.com", enabled = true, onClick = { })
        }
    }

    @Test
    fun testExistingUserDisabled() {
        paparazziRule.snapshot {
            LinkButton(email = "jaynewstrom@test.com", enabled = false, onClick = { })
        }
    }

    @Test
    fun testExistingUserWithLongEmail() {
        paparazziRule.snapshot {
            LinkButton(email = "jaynewstrom12345678987654321@test.com", enabled = true, onClick = { })
        }
    }

    @Test
    fun testExistingUserWithLongEmailDisabled() {
        paparazziRule.snapshot {
            LinkButton(email = "jaynewstrom12345678987654321@test.com", enabled = false, onClick = { })
        }
    }
}
