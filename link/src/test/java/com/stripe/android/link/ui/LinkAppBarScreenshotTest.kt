package com.stripe.android.link.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.R
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.Locale
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class LinkAppBarScreenshotTest(
    private val testCase: TestCase
) {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @get:Rule
    val localesPaparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        Locale.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testLinkAppBarState() {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                LinkAppBar(
                    state = testCase.state,
                    onBackPressed = {},
                    showBottomSheetContent = {}
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<TestCase> {
            val showHeaderOptions = listOf(true to "HeaderShown", false to "NoHeader")
            val showOverflowMenuOptions = listOf(true to "WithOverflow", false to "NoOverflow")
            val emailOptions = listOf(null to "NoEmail", "test@test.com" to "WithEmail")
            val accountStatusOptions = AccountStatus.entries

            return showHeaderOptions.flatMap { (showHeader, headerName) ->
                showOverflowMenuOptions.flatMap { (showOverflow, overflowName) ->
                    emailOptions.flatMap { (email, emailName) ->
                        accountStatusOptions.map { status ->
                            TestCase(
                                name = "LinkAppBar$headerName$overflowName$emailName${status.name}",
                                state = LinkAppBarState(
                                    navigationIcon = R.drawable.stripe_link_close,
                                    showHeader = showHeader,
                                    showOverflowMenu = showOverflow,
                                    email = email,
                                    accountStatus = status
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    internal data class TestCase(val name: String, val state: LinkAppBarState) {
        override fun toString(): String = name
    }
}
