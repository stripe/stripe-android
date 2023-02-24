package com.stripe.android.identity.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.networking.models.CollectedDataParam
import org.junit.Test
import org.junit.runner.RunWith
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
