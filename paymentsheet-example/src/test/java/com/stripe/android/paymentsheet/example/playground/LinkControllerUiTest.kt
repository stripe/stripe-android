@file:OptIn(
    com.stripe.android.link.LinkControllerPreview::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
)

package com.stripe.android.paymentsheet.example.playground

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkController
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LinkControllerUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `financial connections permissions are displayed`() = runScenario {
        FINANCIAL_CONNECTIONS_PERMISSIONS.forEach { permission ->
            page.permission(permission)
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `selected financial connections permissions are used when presenting`() = runScenario {
        FINANCIAL_CONNECTIONS_PERMISSIONS.forEach { permission ->
            page.permission(permission).performScrollTo().performClick()
        }
        page.email.performScrollTo().performTextReplacement("email@example.com")

        page.present.performScrollTo().performClick()

        val call = presentCalls.awaitItem()
        assertThat(call.email).isEqualTo("email@example.com")
        assertThat(call.financialConnectionsPermissions)
            .containsExactlyElementsIn(FINANCIAL_CONNECTIONS_PERMISSIONS)
            .inOrder()
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val presentCalls = Turbine<PresentCall>()
        composeRule.setContent {
            PaymentSheetExampleTheme {
                LinkControllerUi(
                    modifier = Modifier,
                    controllerState = LinkController.State(),
                    playgroundState = LinkControllerPlaygroundState(),
                    onPaymentMethodButtonClick = { _, _ -> },
                    onCreatePaymentMethodClick = {},
                    onPresentClick = { email, phoneNumber, filter, financialConnectionsPermissions ->
                        presentCalls.add(
                            PresentCall(
                                email = email,
                                phoneNumber = phoneNumber,
                                filter = filter,
                                financialConnectionsPermissions = financialConnectionsPermissions,
                            )
                        )
                    },
                    onLookupClick = {},
                    onAuthenticationClick = { _, _ -> },
                    onAuthorizeClick = {},
                    onRegisterConsumerClick = { _, _, _, _ -> },
                    onUpdatePhoneNumberClick = {},
                    onLogOutClick = {},
                    onErrorMessage = {},
                )
            }
        }

        Scenario(
            page = LinkControllerPage(composeRule),
            presentCalls = presentCalls,
        ).apply { block() }

        presentCalls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val page: LinkControllerPage,
        val presentCalls: Turbine<PresentCall>,
    )

    private data class PresentCall(
        val email: String,
        val phoneNumber: String?,
        val filter: List<LinkController.PaymentMethodType>?,
        val financialConnectionsPermissions: List<String>?,
    )
}

private class LinkControllerPage(
    private val composeRule: ComposeContentTestRule,
) {
    val email = composeRule.onNodeWithTag(LINK_CONTROLLER_CONSUMER_EMAIL_TEST_TAG)
    val present = composeRule.onNodeWithTag(LINK_CONTROLLER_PRESENT_BUTTON_TEST_TAG)

    fun permission(permission: String) = composeRule.onNodeWithTag(
        financialConnectionsPermissionTestTag(permission)
    )
}

private val FINANCIAL_CONNECTIONS_PERMISSIONS = listOf(
    "payment_method",
    "balances",
    "ownership",
    "transactions",
)
