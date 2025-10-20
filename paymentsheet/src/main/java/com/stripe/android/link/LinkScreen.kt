package com.stripe.android.link

import androidx.core.net.toUri
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.stripe.android.link.LinkScreen.UpdateCard.BillingDetailsUpdateFlow

internal sealed class LinkScreen(
    protected val baseRoute: String,
    private val args: List<NamedNavArgument> = emptyList()
) {

    val route: String by lazy {
        val placeholders = args.map { it.name }.associateWith { "{$it}" }
        baseRoute.appendParamValues(placeholders)
    }

    data object Loading : LinkScreen("loading")
    data object Verification : LinkScreen("verification")
    data object Wallet : LinkScreen("wallet")
    data object PaymentMethod : LinkScreen("paymentMethod")
    data object SignUp : LinkScreen("signUp")
    data object OAuthConsent : LinkScreen("oauthConsent")

    data object UpdateCard : LinkScreen(
        baseRoute = "updateCard",
        args = listOf(
            navArgument(EXTRA_PAYMENT_DETAILS) {
                type = NavType.StringType
            },
            navArgument(EXTRA_IS_BILLING_UPDATE_FLOW) {
                type = NavType.BoolType
                defaultValue = false
            },
            navArgument(EXTRA_BILLING_UPDATE_CVC) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        operator fun invoke(
            paymentDetailsId: String,
            billingDetailsUpdateFlow: BillingDetailsUpdateFlow?
        ): String {
            return baseRoute.appendParamValues(
                mapOf(
                    EXTRA_PAYMENT_DETAILS to paymentDetailsId,
                    EXTRA_IS_BILLING_UPDATE_FLOW to (billingDetailsUpdateFlow != null).toString(),
                    EXTRA_BILLING_UPDATE_CVC to billingDetailsUpdateFlow?.cvc
                )
            )
        }

        /**
         * Data class representing billing details update flow configuration.
         * When null, it indicates this is not a billing details update flow.
         * When present, it contains optional CVC collected from the wallet screen.
         */
        data class BillingDetailsUpdateFlow(
            val cvc: String? = null
        )
    }

    companion object {
        const val EXTRA_PAYMENT_DETAILS = "payment_details"
        const val EXTRA_IS_BILLING_UPDATE_FLOW = "is_billing_update_flow"
        const val EXTRA_BILLING_UPDATE_CVC = "billing_update_cvc"

        /**
         * Extracts and rebuilds the BillingDetailsUpdateFlow object from navigation arguments.
         * Returns null if not a billing details update flow.
         */
        fun NavBackStackEntry.billingDetailsUpdateFlow(): BillingDetailsUpdateFlow? {
            val arguments = arguments ?: return null
            val isBillingDetailsUpdateFlow = arguments
                .getString(EXTRA_IS_BILLING_UPDATE_FLOW).toBoolean()
            if (!isBillingDetailsUpdateFlow) {
                return null
            }
            val cvc = arguments.getString(EXTRA_BILLING_UPDATE_CVC)
            return BillingDetailsUpdateFlow(cvc = cvc)
        }
    }
}

private fun String.appendParamValues(params: Map<String, String?>): String {
    if (params.isEmpty()) return this
    val uriBuilder = toUri().buildUpon()
    params.forEach { (key, value) ->
        if (value != null) uriBuilder.appendQueryParameter(key, value)
    }
    return uriBuilder.build().toString()
}
