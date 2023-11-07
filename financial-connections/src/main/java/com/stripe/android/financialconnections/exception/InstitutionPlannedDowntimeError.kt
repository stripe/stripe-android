package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution

internal class InstitutionPlannedDowntimeError(
    val institution: FinancialConnectionsInstitution,
    val showManualEntry: Boolean,
    val isToday: Boolean,
    val backUpAt: Long,
    stripeException: StripeException
) : FinancialConnectionsError(
    name = "InstitutionPlannedDowntimeError",
    stripeException = stripeException,
)
