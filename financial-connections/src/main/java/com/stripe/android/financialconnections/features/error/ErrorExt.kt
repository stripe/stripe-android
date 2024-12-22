package com.stripe.android.financialconnections.features.error

import com.stripe.android.core.exception.APIException

internal fun Throwable.isAttestationError(): Boolean {
    return this is APIException && stripeError?.code == "link_failed_to_attest_request"
}
