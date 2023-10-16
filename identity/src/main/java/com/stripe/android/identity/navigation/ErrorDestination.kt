package com.stripe.android.identity.navigation

import android.content.Context
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.Requirement

internal class ErrorDestination(
    errorTitle: String,
    errorContent: String,
    continueButtonText: String = UNSET_BUTTON_TEXT,
    continueButtonRequirement: Requirement? = null,
    backButtonText: String,
    backButtonDestination: String = UNEXPECTED_ROUTE,
    shouldFail: Boolean = false
) : IdentityTopLevelDestination() {

    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_ERROR_TITLE to errorTitle,
        ARG_ERROR_CONTENT to errorContent,
        ARG_CONTINUE_BUTTON_TEXT to continueButtonText,
        ARG_CONTINUE_BUTTON_REQUIREMENT to (continueButtonRequirement ?: UNSET_BUTTON_REQUIREMENT),
        ARG_GO_BACK_BUTTON_DESTINATION to backButtonDestination.toRouteBase(),
        ARG_GO_BACK_BUTTON_TEXT to backButtonText,
        ARG_SHOULD_FAIL to shouldFail
    )

    internal companion object {
        const val ERROR = "Error"
        const val ARG_ERROR_TITLE = "errorTitle"
        const val ARG_ERROR_CONTENT = "errorContent"

        // if set, shows continue button, clickign it would clear the requirement
        const val ARG_CONTINUE_BUTTON_TEXT = "continueButtonText"
        const val ARG_CONTINUE_BUTTON_REQUIREMENT = "continueButtonRequirement"

        // if set, shows go_back button, clicking it would navigate to the destination.
        const val ARG_GO_BACK_BUTTON_TEXT = "goBackButtonText"
        const val ARG_GO_BACK_BUTTON_DESTINATION = "goBackButtonDestination"

        // if set to true, clicking bottom button and pressBack would end flow with Failed
        const val ARG_SHOULD_FAIL = "shouldFail"

        // Indicates the server returns a requirementError that doesn't match with current route.
        //  E.g ConsentScreen->DocSelectScreen could only have BIOMETRICCONSENT error but not IDDOCUMENTFRONT error.
        // If this happens, set the back button destination to [DEFAULT_BACK_BUTTON_DESTINATION]
        const val UNEXPECTED_ROUTE = "UnexpectedRoute"

        const val UNSET_BUTTON_TEXT = "unset"
        const val UNSET_BUTTON_REQUIREMENT = "unset"

        const val TAG = "ErrorDestination"

        fun errorTitle(backStackEntry: NavBackStackEntry) =
            backStackEntry.getStringArgument(ARG_ERROR_TITLE)

        fun errorContent(backStackEntry: NavBackStackEntry) =
            backStackEntry.getStringArgument(ARG_ERROR_CONTENT)

        fun continueButtonContext(backStackEntry: NavBackStackEntry): Pair<String, Requirement>? {
            val continueButtonText = backStackEntry.getStringArgument(ARG_CONTINUE_BUTTON_TEXT)
            val continueButtonRequirement: Requirement? =
                backStackEntry.getStringArgument(ARG_CONTINUE_BUTTON_REQUIREMENT)
                    .let { requirementString ->
                        if (requirementString == UNSET_BUTTON_REQUIREMENT) {
                            null
                        } else {
                            Requirement.valueOf(requirementString)
                        }
                    }
            return if (continueButtonText != UNSET_BUTTON_TEXT && continueButtonRequirement != null) {
                (continueButtonText to continueButtonRequirement)
            } else {
                null
            }
        }

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
                },
                navArgument(ARG_CONTINUE_BUTTON_TEXT) {
                    type = NavType.StringType
                },
                navArgument(ARG_CONTINUE_BUTTON_REQUIREMENT) {
                    type = NavType.StringType
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
