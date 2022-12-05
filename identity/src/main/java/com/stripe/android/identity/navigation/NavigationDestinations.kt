package com.stripe.android.identity.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState

internal abstract class IdentityTopLevelDestination {
    /**
     * The route this destination navigates to, used for composable.
     */
    internal abstract class DestinationRoute {
        abstract val routeBase: String
        open val arguments: List<NamedNavArgument> = emptyList()

        /**
         * Route for navigation, built with route base and arguments as follows:
         *   routeBase?argName1={argName1}&argName2={argName2}
         */
        val route: String
            get() {
                val routeBuilder = StringBuilder(routeBase)
                for ((index, argument) in arguments.iterator().withIndex()) {
                    routeBuilder.append(
                        if (index == 0) '?' else '&'
                    ).append("${argument.name}={${argument.name}}")
                }
                return routeBuilder.toString()
            }
    }

    abstract val destinationRoute: DestinationRoute

    abstract val routeWithArgs: String

    /**
     * The action id to navigate to.
     * TODO(ccen): Remove after the nav graph is built with Jetpack compose.
     */
    @get:IdRes
    abstract val destination: Int

    /**
     * The bundle arguments used when navigate to [destination].
     * TODO(ccen): Remove after the nav graph is built with Jetpack compose.
     */
    open val argsBundle: Bundle? = null
}

private fun String.withBracket() = "{$this}"

internal fun IdentityTopLevelDestination.DestinationRoute.withParams(
    vararg params: Pair<String, Any?>
): String {
    var ret = this.route
    params.forEach { (key, value) ->
        ret = ret.replace(key.withBracket(), value?.toString() ?: "")
    }
    return ret
}

internal class CameraPermissionDeniedDestination(
    scanType: CollectedDataParam.Type?,
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_SCAN_TYPE to scanType
    )

    override val destination = R.id.action_camera_permission_denied
    override val argsBundle = scanType?.let {
        bundleOf(
            ARG_SCAN_TYPE to it
        )
    }

    companion object {
        const val CAMERA_PERMISSION_DENIED = "CameraPermissionDenied"
        const val ARG_SCAN_TYPE = "scanType"

        val ROUTE = object : DestinationRoute() {
            override val routeBase = CAMERA_PERMISSION_DENIED
            override val arguments = listOf(
                navArgument(ARG_SCAN_TYPE) {
                    type = NavType.EnumType(CollectedDataParam.Type::class.java)
                }
            )
        }
    }
}

internal class CouldNotCaptureDestination(
    scanType: IdentityScanState.ScanType?,
    requireLiveCapture: Boolean?
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_REQUIRE_LIVE_CAPTURE to requireLiveCapture
    )
    override val destination = R.id.action_global_couldNotCaptureFragment
    override val argsBundle = bundleOf(
        ARG_COULD_NOT_CAPTURE_SCAN_TYPE to scanType
    ).also { bundle ->
        requireLiveCapture?.let {
            bundle.putBoolean(ARG_REQUIRE_LIVE_CAPTURE, it)
        }
    }

    companion object {
        const val COULD_NOT_CAPTURE = "CouldNotCapture"
        const val ARG_COULD_NOT_CAPTURE_SCAN_TYPE = "scanType"
        const val ARG_REQUIRE_LIVE_CAPTURE = "requireLiveCapture"

        val ROUTE = object : DestinationRoute() {
            override val routeBase = COULD_NOT_CAPTURE

            override val arguments = listOf(
                navArgument(ARG_COULD_NOT_CAPTURE_SCAN_TYPE) {
                    type = NavType.EnumType(IdentityScanState.ScanType::class.java)
                },
                navArgument(ARG_REQUIRE_LIVE_CAPTURE) {
                    type = NavType.BoolType
                }
            )
        }
    }
}

internal class ErrorDestination(
    errorTitle: String,
    errorContent: String,
    backButtonText: String,
    cause: Throwable, // TODO(ccen) Remove this param after moving to Jetpack compose
    backButtonDestination: Int = UNEXPECTED_DESTINATION,
    shouldFail: Boolean = false
) : IdentityTopLevelDestination() {
    override val destinationRoute = Route
    override val destination = R.id.action_global_errorFragment
    override val routeWithArgs = destinationRoute.withParams(
        ARG_ERROR_TITLE to errorTitle,
        ARG_ERROR_CONTENT to errorContent,
        ARG_GO_BACK_BUTTON_DESTINATION to backButtonDestination,
        ARG_GO_BACK_BUTTON_TEXT to backButtonText,
        ARG_SHOULD_FAIL to shouldFail,
        ARG_CAUSE to cause
    )

    override val argsBundle = bundleOf(
        ARG_ERROR_TITLE to errorTitle,
        ARG_ERROR_CONTENT to errorContent,
        ARG_GO_BACK_BUTTON_DESTINATION to backButtonDestination,
        ARG_GO_BACK_BUTTON_TEXT to backButtonText,
        ARG_SHOULD_FAIL to shouldFail,
        ARG_CAUSE to cause
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
        const val ARG_CAUSE = "cause"

        // Indicates the server returns a requirementError that doesn't match with current Fragment.
        //  E.g ConsentFragment->DocSelectFragment could only have BIOMETRICCONSENT error but not IDDOCUMENTFRONT error.
        // If this happens, set the back button destination to [DEFAULT_BACK_BUTTON_DESTINATION]
        const val UNEXPECTED_DESTINATION = -1

        val Route = object : DestinationRoute() {
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

internal object ConfirmationDestination : IdentityTopLevelDestination() {
    private const val CONFIRMATION = "Confirmation"
    private val ROUTE = object : DestinationRoute() {
        override val routeBase = CONFIRMATION
    }
    override val destination = R.id.action_global_confirmationFragment
    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.route
}

internal object DocSelectionDestination : IdentityTopLevelDestination() {
    private const val DOC_SELECTION = "DocSelection"
    private val ROUTE = object : DestinationRoute() {
        override val routeBase = DOC_SELECTION
    }
    override val destination = R.id.action_global_docSelectionFragment
    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.route
}

internal object ConsentDestination : IdentityTopLevelDestination() {
    private const val CONSENT = "Consent"
    private val ROUTE = object : DestinationRoute() {
        override val routeBase = CONSENT
    }
    override val destination = R.id.action_global_consentFragment
    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.route
}

/**
 * Navigate to the [IdentityTopLevelDestination] by calling NavController.navigate(fragmentID, bundle).
 * TODO(ccen) change its implementation to NavController.navigate(destination.route) when all fragments are moved.
 */
internal fun NavController.navigateTo(destination: IdentityTopLevelDestination) {
//    navigate(destination.routeWithArgs)
    navigate(
        destination.destination,
        destination.argsBundle
    )
}
