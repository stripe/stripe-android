package com.stripe.android.identity.navigation

import android.content.Context
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.stripe.android.identity.R

internal class ErrorDestination(
    errorTitle: String,
    errorContent: String,
    backButtonText: String,
    backButtonDestination: String = UNEXPECTED_ROUTE,
    shouldFail: Boolean = false
) : IdentityTopLevelDestination() {

    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_ERROR_TITLE to errorTitle,
        ARG_ERROR_CONTENT to errorContent,
        ARG_GO_BACK_BUTTON_DESTINATION to backButtonDestination.toRouteBase(),
        ARG_GO_BACK_BUTTON_TEXT to backButtonText,
        ARG_SHOULD_FAIL to shouldFail
    )

    internal companion object {
        const val ERROR = "Error"
        const val ARG_ERROR_TITLE = "errorTitle"
        const val ARG_ERROR_CONTENT = "errorContent"

        // if set, shows go_back button, clicking it would navigate to the destination.
        const val ARG_GO_BACK_BUTTON_TEXT = "goBackButtonText"
        const val ARG_GO_BACK_BUTTON_DESTINATION = "goBackButtonDestination"

        // if set to true, clicking bottom button and pressBack would end flow with Failed
        const val ARG_SHOULD_FAIL = "shouldFail"

        // Indicates the server returns a requirementError that doesn't match with current route.
        //  E.g ConsentScreen->DocSelectScreen could only have BIOMETRICCONSENT error but not IDDOCUMENTFRONT error.
        // If this happens, set the back button destination to [DEFAULT_BACK_BUTTON_DESTINATION]
        const val UNEXPECTED_ROUTE = "UnexpectedRoute"

        val TAG = ErrorDestination::class.java.simpleName

        fun errorTitle(backStackEntry: NavBackStackEntry) =
            backStackEntry.getStringArgument(ARG_ERROR_TITLE)

        fun errorContent(backStackEntry: NavBackStackEntry) =
            backStackEntry.getStringArgument(ARG_ERROR_CONTENT)

        fun backButtonText(backStackEntry: NavBackStackEntry) =
            backStackEntry.getStringArgument(ARG_GO_BACK_BUTTON_TEXT)

        fun backButtonDestination(backStackEntry: NavBackStackEntry) =
            backStackEntry.getStringArgument(ARG_GO_BACK_BUTTON_DESTINATION)

        fun shouldFail(backStackEntry: NavBackStackEntry) =
            backStackEntry.getBooleanArgument(ARG_SHOULD_FAIL)

        val ROUTE = object : DestinationRoute() {
            override val routeBase = ERROR
            override val arguments = listOf(
                navArgument(ARG_ERROR_TITLE) {
                    type = NavType.StringType
                },
                navArgument(ARG_ERROR_CONTENT) {
                    type = NavType.StringType
                },
                navArgument(ARG_GO_BACK_BUTTON_TEXT) {
                    type = NavType.StringType
                },
                navArgument(ARG_GO_BACK_BUTTON_DESTINATION) {
                    type = NavType.StringType
                },
                navArgument(ARG_SHOULD_FAIL) {
                    type = NavType.BoolType
                }
            )
        }
    }
}

internal fun Context.finalErrorDestination(): ErrorDestination =
    ErrorDestination(
        errorTitle = getString(R.string.stripe_error),
        errorContent = getString(R.string.stripe_unexpected_error_try_again),
        backButtonText = getString(R.string.stripe_go_back),
        shouldFail = true
    )
