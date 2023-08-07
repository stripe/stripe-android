package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution

internal class InstitutionUnplannedDowntimeError(
    val institution: FinancialConnectionsInstitution,
    val showManualEntry: Boolean,
    stripeException: StripeException
) : FinancialConnectionsError(
    name = "InstitutionUnplannedDowntimeError",
    stripeException = stripeException
)
