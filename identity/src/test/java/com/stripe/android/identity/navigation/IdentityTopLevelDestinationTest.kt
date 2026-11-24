package com.stripe.android.identity.navigation

import androidx.core.os.bundleOf
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.networking.models.CollectedDataParam
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdentityTopLevelDestinationTest {

    private class IdentityTopLevelDestinationWithArgs(
        arg1: String,
        arg2: Int,
        arg3: CollectedDataParam.Type
    ) : IdentityTopLevelDestination() {
        override val destinationRoute = ROUTE_WITH_ARGS
        override val routeWithArgs = destinationRoute.withParams(
            ARG1 to arg1,
            ARG2 to arg2,
            ARG3 to arg3,
        )
    }

    private class IdentityTopLevelDestinationWithoutArgs : IdentityTopLevelDestination() {
        override val destinationRoute = ROUTE_WITHOUT_ARGS
    }

    @Test
    fun testRouteWithArgumentsIsCalculatedCorrectly() {
        val arg1 = "arg1"
        val arg2 = 123
        val arg3 = CollectedDataParam.Type.IDCARD
        val testDestination = IdentityTopLevelDestinationWithArgs(
            arg1,
            arg2,
            arg3
        )

        assertThat(testDestination.destinationRoute.route).isEqualTo(
            "TestRoute?arg1={arg1}&arg2={arg2}&arg3={arg3}"
        )

        assertThat(testDestination.routeWithArgs).isEqualTo(
            "TestRoute?arg1=$arg1&arg2=$arg2&arg3=$arg3"
        )
    }

    @Test
    fun testRouteWithoutArgumentsIsCalculatedCorrectly() {
        val testDestination = IdentityTopLevelDestinationWithoutArgs()

        assertThat(testDestination.destinationRoute.route).isEqualTo(
            "TestRoute"
        )

        assertThat(testDestination.routeWithArgs).isEqualTo(
            "TestRoute"
        )
    }

    @Test
    fun `navigateTo from an upload error removes the error from the back stack`() {
        // Given an upload error
        val navController = navControllerOnError(isUploadError = true)

        // When the retry navigates to the next screen
        navController.navigateTo(ConfirmationDestination)

        // Then the navigation pops the error
        val navOptions = navOptionsOfNavigation(navController, ConfirmationDestination)
        assertThat(navOptions.popUpToRoute).isEqualTo(ErrorDestination.ROUTE.route)
        assertThat(navOptions.isPopUpToInclusive()).isTrue()
    }

    @Test
    fun `navigateTo from another error keeps the error in the back stack`() {
        // Given an error that is not an upload error
        val navController = navControllerOnError(isUploadError = false)

        // When the flow navigates to another screen
        navController.navigateTo(ConfirmationDestination)

        // Then the navigation pops nothing
        assertThat(navOptionsOfNavigation(navController, ConfirmationDestination).popUpToRoute)
            .isNull()
    }

    @Test
    fun `navigateTo from an upload error removes the error when the destination's pop target is missing`() {
        // Given an upload error, and a back stack without the next screen's pop target
        val navController = navControllerOnError(isUploadError = true)
        whenever(navController.getBackStackEntry(IndividualDestination.ROUTE.route))
            .thenThrow(IllegalArgumentException("No destination with route Individual"))

        // When the retry navigates to that screen
        navController.navigateTo(IndividualDestination)

        // Then the navigation pops the error
        val navOptions = navOptionsOfNavigation(navController, IndividualDestination)
        assertThat(navOptions.popUpToRoute).isEqualTo(ErrorDestination.ROUTE.route)
        assertThat(navOptions.isPopUpToInclusive()).isTrue()
    }

    @Test
    fun `navigateTo from an upload error uses the destination's pop target when it is in the back stack`() {
        // Given an upload error, and a back stack that has the next screen's pop target
        val navController = navControllerOnError(isUploadError = true)
        whenever(navController.getBackStackEntry(DocWarmupDestination.ROUTE.route)).thenReturn(mock())

        // When the retry navigates to that screen
        navController.navigateTo(SelfieWarmupDestination)

        // Then the navigation pops up to that target, which also removes the error
        val navOptions = navOptionsOfNavigation(navController, SelfieWarmupDestination)
        assertThat(navOptions.popUpToRoute).isEqualTo(DocWarmupDestination.ROUTE.route)
        assertThat(navOptions.isPopUpToInclusive()).isFalse()
    }

    private fun navControllerOnError(isUploadError: Boolean): NavController {
        val errorEntry = mock<NavBackStackEntry> {
            on { arguments }.thenReturn(
                bundleOf(ErrorDestination.ARG_IS_UPLOAD_ERROR to isUploadError)
            )
        }
        return mock {
            on { currentBackStackEntry }.thenReturn(errorEntry)
        }
    }

    private fun navOptionsOfNavigation(
        navController: NavController,
        destination: IdentityTopLevelDestination
    ): NavOptions {
        val builder = argumentCaptor<NavOptionsBuilder.() -> Unit>()
        verify(navController).navigate(eq(destination.routeWithArgs), builder.capture())
        return navOptions(builder.firstValue)
    }

    private companion object {
        const val ROUTE_BASE = "TestRoute"
        const val ARG1 = "arg1"
        const val ARG2 = "arg2"
        const val ARG3 = "arg3"

        val ROUTE_WITH_ARGS = object : IdentityTopLevelDestination.DestinationRoute() {
            override val routeBase = ROUTE_BASE
            override val arguments = listOf(
                navArgument(ARG1) {
                    type = NavType.StringType
                },
                navArgument(ARG2) {
                    type = NavType.IntType
                },
                navArgument(ARG3) {
                    type = NavType.EnumType(CollectedDataParam.Type::class.java)
                },
            )
        }

        val ROUTE_WITHOUT_ARGS = object : IdentityTopLevelDestination.DestinationRoute() {
            override val routeBase = ROUTE_BASE
        }
    }
}
