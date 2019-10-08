package com.stripe.android

import com.stripe.android.model.SetupIntent

class SetupIntentResult internal constructor(
    setupIntent: SetupIntent,
    @Outcome outcome: Int = 0
) : StripeIntentResult<SetupIntent>(setupIntent, outcome)
