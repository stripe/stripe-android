package com.stripe.android.identity.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.networking.models.Requirement

internal abstract class IdentityTopLevelDestination(
    val shouldPopUpToDocSelection: Boolean = false
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
     * Route with arguments, filled with actual toString value of arguments as follows:
     *   routeBase?argName1=arg1StringValue&argName2=arg2StringValue
     */
    abstract val routeWithArgs: String
}

internal fun String.toRouteBase() = substringBefore('?')

internal fun IdentityTopLevelDestination.DestinationRoute.withParams(
    vararg params: Pair<String, Any>
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
        if (destination.shouldPopUpToDocSelection) {
            popUpTo(DocSelectionDestination.ROUTE.route) { inclusive = false }
        }
    }
}

internal fun String.routeToScreenName(): String = when (this) {
    ConsentDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_CONSENT
    DocSelectionDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_DOC_SELECT
    IDScanDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_LIVE_CAPTURE_ID
    PassportScanDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_LIVE_CAPTURE_PASSPORT
    DriverLicenseScanDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_LIVE_CAPTURE_DRIVER_LICENSE
    IDUploadDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_FILE_UPLOAD_ID
    PassportUploadDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_FILE_UPLOAD_PASSPORT
    DriverLicenseUploadDestination.ROUTE.route ->
        IdentityAnalyticsRequestFactory.SCREEN_NAME_FILE_UPLOAD_DRIVER_LICENSE
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
    else ->
        throw IllegalArgumentException("Invalid route: $this")
}

internal fun String.routeToRequirement(): List<Requirement> = when (this) {
    ConsentDestination.ROUTE.route ->
        listOf(Requirement.BIOMETRICCONSENT)
    DocSelectionDestination.ROUTE.route ->
        listOf(Requirement.IDDOCUMENTTYPE)
    IDUploadDestination.ROUTE.route ->
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    PassportUploadDestination.ROUTE.route ->
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    DriverLicenseUploadDestination.ROUTE.route ->
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    IDScanDestination.ROUTE.route ->
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    PassportScanDestination.ROUTE.route ->
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    DriverLicenseScanDestination.ROUTE.route ->
        listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)
    SelfieDestination.ROUTE.route ->
        listOf(Requirement.FACE)
    IndividualDestination.ROUTE.route ->
        listOf(Requirement.NAME, Requirement.DOB, Requirement.ADDRESS, Requirement.IDNUMBER)
    else ->
        emptyList()
}
