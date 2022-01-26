package com.stripe.android.stripecardscan.cardscan

import androidx.annotation.Keep
import com.stripe.android.camera.framework.time.seconds

@Keep
object CardScanConfig {

    /**
     * The duration after which the scan will reset if no card is visible.
     */
    @JvmStatic
    var NO_CARD_VISIBLE_DURATION_MILLIS = 5.seconds.inMilliseconds.toInt()

    /**
     * The maximum duration for which to search for a card number.
     */
    @JvmStatic
    var OCR_SEARCH_DURATION_MILLIS = 10.seconds.inMilliseconds.toInt()

    /**
     * Once this number of frames with matching card numbers are found, stop looking for card
     * numbers.
     */
    @JvmStatic
    var DESIRED_OCR_AGREEMENT = 3

    /**
     * The maximum number of saved frames per type to use.
     */
    @JvmStatic
    var MAX_SAVED_FRAMES_PER_TYPE = 6
}
