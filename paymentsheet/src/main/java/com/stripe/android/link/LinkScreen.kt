package com.stripe.android.link

import androidx.core.net.toUri
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlin.collections.List

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

    data object UpdateCard : LinkScreen(
        baseRoute = "updateCard",
        args = listOf(
            navArgument(EXTRA_PAYMENT_DETAILS) { type = NavType.StringType },
            navArgument(EXTRA_IS_BILLING_UPDATE_FLOW) {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) {
        operator fun invoke(paymentDetailsId: String, isBillingDetailsUpdateFlow: Boolean = false) =
            baseRoute.appendParamValues(
                mapOf(
                    EXTRA_PAYMENT_DETAILS to paymentDetailsId,
                    EXTRA_IS_BILLING_UPDATE_FLOW to isBillingDetailsUpdateFlow.toString()
                )
            )
    }

    companion object {
        const val EXTRA_PAYMENT_DETAILS = "payment_details"
        const val EXTRA_IS_BILLING_UPDATE_FLOW = "is_billing_update_flow"
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
