package com.stripe.android

import com.stripe.android.model.SetupIntent
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * A model representing the result of a [SetupIntent] confirmation via [Stripe.confirmSetupIntent]
 * or handling of next actions via [Stripe.handleNextActionForSetupIntent].
 */
@Parcelize
@Poko
class SetupIntentResult internal constructor(
    override val intent: SetupIntent,
    @Outcome private val outcomeFromFlow: Int = 0,
    override val failureMessage: String? = null
) : StripeIntentResult<SetupIntent>(outcomeFromFlow)
