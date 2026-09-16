package com.stripe.android.identity.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NetworkedIdentityNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `entering NI removes the consent history`() = runScenario {
        composeRule.runOnIdle { navController.navigateTo(ConsentDestination) }
        composeRule.runOnIdle {
            assertThat(navController.previousBackStackEntry?.destination?.route)
                .isEqualTo(InitialLoadingDestination.ROUTE.route)
            navController.navigateReplacingIdentityStack(NetworkedIdentityDestination)
        }
        composeRule.runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(NetworkedIdentityDestination.ROUTE.route)
            assertThat(navController.previousBackStackEntry).isNull()
        }
    }

    @Test
    fun `ordinary navigation after NI cannot return to the completed flow`() = runScenario {
        composeRule.runOnIdle { navController.navigateReplacingIdentityStack(NetworkedIdentityDestination) }
        composeRule.runOnIdle { navController.navigateTo(DocWarmupDestination) }
        composeRule.runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(DocWarmupDestination.ROUTE.route)
            assertThat(navController.previousBackStackEntry).isNull()
        }
    }

    @Test
    fun `NI resume to a remaining requirement replaces the stack`() = runScenario {
        composeRule.runOnIdle { navController.navigateReplacingIdentityStack(NetworkedIdentityDestination) }
        composeRule.runOnIdle { navController.navigateReplacingIdentityStack(SelfieWarmupDestination) }
        composeRule.runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(SelfieWarmupDestination.ROUTE.route)
            assertThat(navController.previousBackStackEntry).isNull()
        }
    }

    @Test
    fun `NI route has a stable analytics name`() {
        assertThat(NetworkedIdentityDestination.ROUTE.route.routeToScreenName()).isEqualTo("networked_identity")
    }

    @Test
    fun `remaining phone OTP goes to the ordinary OTP screen`() {
        val destination = listOf(Requirement.PHONE_OTP).nextNetworkedIdentityDestination(
            ApplicationProvider.getApplicationContext()
        )
        assertThat(destination).isEqualTo(OTPDestination)
    }

    @Test
    fun `remaining phone number is collected before OTP`() {
        val destination = listOf(Requirement.PHONE_NUMBER, Requirement.PHONE_OTP).nextNetworkedIdentityDestination(
            ApplicationProvider.getApplicationContext()
        )
        assertThat(destination).isEqualTo(IndividualDestination)
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = InitialLoadingDestination.ROUTE.route) {
                composable(InitialLoadingDestination.ROUTE.route) { }
                composable(ConsentDestination.ROUTE.route) { }
                composable(NetworkedIdentityDestination.ROUTE.route) { }
                composable(DocWarmupDestination.ROUTE.route) { }
                composable(SelfieWarmupDestination.ROUTE.route) { }
            }
        }
        composeRule.waitForIdle()
        Scenario(navController).block()
    }

    private data class Scenario(val navController: NavHostController)
}
