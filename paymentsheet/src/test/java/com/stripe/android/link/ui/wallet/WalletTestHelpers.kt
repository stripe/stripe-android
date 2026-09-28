package com.stripe.android.link.ui.wallet

import java.time.Year

internal fun getTwoDigitFutureYear() = (Year.now().value + 1).toString().takeLast(2)
