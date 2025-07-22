package com.stripe.android.link.ui.signup

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.LinkScreenshotSurface
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.EmailConfig
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class EmailCollectionSectionScreenshotTest(
    private val testCase: TestCase
) {
    @get:Rule
    val paparazziRule = PaparazziRule(
        listOf(SystemAppearance.DarkTheme),
        listOf(FontSize.DefaultFont)
    )

    @Test
    fun test() {
        paparazziRule.snapshot {
            LinkScreenshotSurface {
                StripeThemeForLink {
                    EmailCollectionSection(
                        canEditForm = testCase.canEditEmail,
                        canEditEmail = testCase.canEditEmail,
                        emailController = EmailConfig.createController(testCase.email),
                        signUpState = testCase.signUpState,
                        focusRequester = remember { FocusRequester() }
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<TestCase> {
            return SignUpState.entries.flatMap { signUpState ->
                listOf(
                    TestCase(name = "Basic"),
                    TestCase(name = "Email", email = "email@email.com"),
                    TestCase(name = "InvalidEmail", email = "emai"),
                    TestCase(name = "CannotEditForm", canEditForm = false),
                    TestCase(name = "CannotEditEmail", canEditEmail = false),
                    TestCase(name = "CannotEditEmailWithEmail", email = "email@email.com", canEditEmail = false),
                ).map {
                    it.copy(
                        name = "${it.name}${signUpState.name}",
                        signUpState = signUpState,
                    )
                }
            }
        }
    }

    internal data class TestCase(
        val name: String,
        val canEditForm: Boolean = true,
        val canEditEmail: Boolean = true,
        val email: String = "",
        val signUpState: SignUpState = SignUpState.InputtingPrimaryField,
    ) {
        override fun toString(): String = name
    }
}
