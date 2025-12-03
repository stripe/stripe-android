package com.stripe.android.paymentsheet

import androidx.annotation.RestrictTo

/**
 * Marks card funding filtering API as being in private preview.
 * This feature allows filtering which card funding types (credit, debit, prepaid) are accepted.
 *
 * Note: This is a client-side solution. Card funding filtering is not currently supported in Link.
 * The backend performs the final validation.
 */
@RequiresOptIn(
    message = "This card funding filtering API is in private preview and may change without notice.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class CardFundingFilteringPrivatePreview
