package com.stripe.android.identity.navigation

import android.content.Context
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.Requirement.Companion.nextDestination

internal object NetworkedIdentityDestination : IdentityTopLevelDestination() {
    val ROUTE = object : DestinationRoute() {
        override val routeBase = "NetworkedIdentity"
    }

    override val destinationRoute = ROUTE
}

internal fun List<Requirement>.nextNetworkedIdentityDestination(context: Context): IdentityTopLevelDestination =
    when {
        isNotEmpty() && all { it == Requirement.PHONE_OTP } -> OTPDestination
        Requirement.PHONE_NUMBER in this && all { it == Requirement.PHONE_NUMBER || it == Requirement.PHONE_OTP } ->
            IndividualDestination
        else -> nextDestination(context)
    }
