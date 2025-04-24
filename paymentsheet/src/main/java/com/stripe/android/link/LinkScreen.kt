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
        args = listOf(navArgument(EXTRA_PAYMENT_DETAILS) { type = NavType.StringType })
    ) {
        operator fun invoke(paymentDetailsId: String) = baseRoute.appendParamValues(
            mapOf(EXTRA_PAYMENT_DETAILS to paymentDetailsId)
        )
    }

    companion object {
        const val EXTRA_PAYMENT_DETAILS = "payment_details"
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
