package com.stripe.android.paymentsheet.addresselement

import androidx.compose.material.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NavHostAddressElementNavigatorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `root Back returns false and leaves Input entry accessible`() = runScenario {
        val backHandled = composeRule.runOnIdle { navigator.onBack() }

        assertThat(backHandled).isFalse()
        composeRule.onNodeWithText(INPUT_DESTINATION).assertIsDisplayed()
        assertThat(inputResultFlow()).isNotNull()
    }

    @Test
    fun `Back returns true and pops nested destination`() = runScenario {
        navigateToAutocomplete()

        val backHandled = composeRule.runOnIdle { navigator.onBack() }
        composeRule.waitForIdle()

        assertThat(backHandled).isTrue()
        composeRule.onNodeWithText(INPUT_DESTINATION).assertIsDisplayed()
        composeRule.onNodeWithText(AUTOCOMPLETE_DESTINATION).assertDoesNotExist()
    }

    @Test
    fun `result flow subscribed on Input receives an event while Autocomplete is current`() = runScenario {
        val resultFlow = requireNotNull(inputResultFlow())
        val expectedEvent = AddressElementNavigator.AutocompleteEvent.OnBack(address = null)

        resultFlow.test {
            navigateToAutocomplete()
            composeRule.runOnIdle {
                navigator.setResult(AddressElementNavigator.AutocompleteEvent.KEY, expectedEvent)
            }

            assertThat(awaitItem()).isEqualTo(expectedEvent)
            ensureAllEventsConsumed()
        }
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val navigator = NavHostAddressElementNavigator()

        composeRule.setContent {
            val navController = rememberNavController()
            SideEffect {
                navigator.navigationController = navController
            }
            NavHost(
                navController = navController,
                startDestination = AddressElementScreen.InputAddress.route,
            ) {
                composable(AddressElementScreen.InputAddress.route) {
                    Text(INPUT_DESTINATION)
                }
                composable(
                    route = AddressElementScreen.Autocomplete.route,
                    arguments = listOf(
                        navArgument(AddressElementScreen.Autocomplete.countryArg) {
                            type = NavType.StringType
                        }
                    ),
                ) { backStackEntry ->
                    val country = backStackEntry.arguments?.getString(
                        AddressElementScreen.Autocomplete.countryArg
                    )
                    Text("Autocomplete: $country")
                }
            }
        }
        composeRule.waitForIdle()

        Scenario(
            composeRule = composeRule,
            navigator = navigator,
        ).block()
    }

    private data class Scenario(
        val composeRule: ComposeTestRule,
        val navigator: NavHostAddressElementNavigator,
    ) {
        fun navigateToAutocomplete() {
            composeRule.runOnIdle {
                navigator.navigateTo(AddressElementScreen.Autocomplete(country = "US"))
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithText(AUTOCOMPLETE_DESTINATION).assertIsDisplayed()
        }

        fun inputResultFlow(): Flow<AddressElementNavigator.AutocompleteEvent?>? =
            composeRule.runOnIdle {
                navigator.getResultFlow<AddressElementNavigator.AutocompleteEvent?>(
                    AddressElementNavigator.AutocompleteEvent.KEY
                )
            }
    }

    private companion object {
        const val INPUT_DESTINATION = "Input"
        const val AUTOCOMPLETE_DESTINATION = "Autocomplete: US"
    }
}
