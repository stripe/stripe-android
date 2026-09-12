package com.stripe.android.financialconnections.features.common

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.model.LegalDetailsBody
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.model.ServerLink
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ModalBottomSheetContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule: TestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `clicking a Link legal row opens its embedded URL`() = runScenario {
        composeRule.onNodeWithTag("${LEGAL_LINK_ROW_TEST_TAG_PREFIX}_0").performClick()

        composeRule.runOnIdle {
            assertThat(clickedUrl()).isEqualTo("https://stripe.com/legal")
        }
    }

    private fun runScenario(block: TestScenario.() -> Unit) {
        var clickedUrl: String? = null
        composeRule.setContent {
            FinancialConnectionsPreview(theme = Theme.LinkLight) {
                LegalDetailsBottomSheetContent(
                    legalDetails = LegalDetailsNotice(
                        icon = null,
                        title = "Terms and privacy policy",
                        subtitle = null,
                        body = LegalDetailsBody(
                            links = listOf(
                                ServerLink(
                                    title = "<a href=\"https://stripe.com/legal\">Terms</a>",
                                    content = "Read the terms",
                                )
                            )
                        ),
                        cta = "OK",
                        disclaimer = null,
                    ),
                    onClickableTextClick = { clickedUrl = it },
                    onConfirmModalClick = {},
                )
            }
        }

        block(TestScenario(clickedUrl = { clickedUrl }))
    }

    private data class TestScenario(
        val clickedUrl: () -> String?,
    )
}
