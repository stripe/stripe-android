package com.stripe.android.identity.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.networking.models.Requirement

internal abstract class IdentityTopLevelDestination(
    val popUpToParam: PopUpToParam? = null
) {
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

    /**
     * Route with arguments,
     * default value should be a string without any arguments:
     *   routeBase
     * overridden value should fill with actual toString value of arguments as follows:
     *   routeBase?argName1=arg1StringValue&argName2=arg2StringValue
     *
     */
    open val routeWithArgs: String
        get() = destinationRoute.route
}

internal data class PopUpToParam(
    val route: String,
    val inclusive: Boolean
)

internal fun String.toRouteBase() = substringBefore('?')

internal fun IdentityTopLevelDestination.DestinationRoute.withParams(
    vararg params: Pair<String, Any?>
): String {
    var ret = this.route
    params.forEach { (key, value) ->
        ret = ret.replace("{$key}", value.toString())
    }
    return ret
}

internal const val EMPTY_STRING = ""
internal const val ZERO = 0

internal fun NavBackStackEntry?.getStringArgument(argName: String) =
    this?.arguments?.getString(argName, EMPTY_STRING) ?: EMPTY_STRING

internal fun NavBackStackEntry?.getIntArgument(argName: String) =
    this?.arguments?.getInt(argName, ZERO) ?: ZERO

internal fun NavBackStackEntry?.getBooleanArgument(argName: String) =
    this?.arguments?.getBoolean(argName, false) ?: false

/**
 * Navigate to the [IdentityTopLevelDestination] by calling NavController.navigate(fragmentID, bundle).
 */
internal fun NavController.navigateTo(destination: IdentityTopLevelDestination) {
    navigate(destination.routeWithArgs) {
        destination.popUpToParam?.let {
            popUpTo(it.route) { inclusive = it.inclusive }
        }
    }
}

internal fun String.routeToScreenName(): String = when (this) {
    ConsentDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_CONSENT
    DocWarmupDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_DOC_WARMUP
    DocumentScanDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_LIVE_CAPTURE
    DocumentUploadDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_FILE_UPLOAD
    SelfieDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_SELFIE
    ConfirmationDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_CONFIRMATION
    CameraPermissionDeniedDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
    ErrorDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
    CouldNotCaptureDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
    IndividualDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_INDIVIDUAL
    CountryNotListedDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_COUNTRY_NOT_LISTED
    IndividualWelcomeDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_INDIVIDUAL_WELCOME
    DebugDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_DEBUG
    OTPDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_PHONE_OTP
    else ->
        throw IllegalArgumentException("Invalid route: $this")
}

internal fun String.routeToRequirement(): List<Requirement> = when (this) {
    ConsentDestination.ROUTE.route ->
        listOf(Requirement.BIOMETRICCONSENT)
    DocumentScanDestination.ROUTE.route ->
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    DocumentUploadDestination.ROUTE.route ->
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    SelfieDestination.ROUTE.route ->
        listOf(Requirement.FACE)
    IndividualDestination.ROUTE.route ->
        listOf(Requirement.NAME, Requirement.DOB, Requirement.ADDRESS, Requirement.IDNUMBER, Requirement.PHONE_NUMBER)
    OTPDestination.ROUTE.route ->
        listOf(Requirement.PHONE_OTP)
    else ->
        emptyList()
}
