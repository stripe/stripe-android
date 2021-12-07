package com.stripe.android.stripecardscan.cardimageverification

import androidx.annotation.Keep
import com.stripe.android.stripecardscan.framework.time.seconds

@Keep
object CardImageVerificationConfig {

    /**
     * The duration after which the scan will reset if no card is visible.
     */
    @JvmStatic
    var NO_CARD_VISIBLE_DURATION_MILLIS = 5.seconds.inMilliseconds.toInt()

    /**
     * The maximum duration for which to search for both a card number and good verification images.
     */
    @JvmStatic
    var OCR_AND_CARD_SEARCH_DURATION_MILLIS = 10.seconds.inMilliseconds.toInt()

    /**
     * The maximum duration for which to search for a card number after the verification images
     * have been satisfied.
     */
    @JvmStatic
    var OCR_ONLY_SEARCH_DURATION_MILLIS = 10.seconds.inMilliseconds.toInt()

    /**
     * The maximum duration for which to search for good verification images after the card number
     * has been found.
     */
    @JvmStatic
    var CARD_ONLY_SEARCH_DURATION_MILLIS = 5.seconds.inMilliseconds.toInt()

    /**
     * Once this number of frames with matching card numbers are found, stop looking for card
     * numbers.
     */
    @JvmStatic
    var DESIRED_OCR_AGREEMENT = 3

    /**
     * Once this number of frames with a clearly centered card are found, stop looking for images
     * with clearly centered cards.
     */
    @JvmStatic
    var DESIRED_CARD_COUNT = 5

    /**
     * Display the wrong card notification to the user for this duration.
     */
    @JvmStatic
    var WRONG_CARD_DURATION_MILLIS = 2.seconds.inMilliseconds.toInt()

    /**
     * The maximum number of saved frames per type to use.
     */
    @JvmStatic
    var MAX_SAVED_FRAMES_PER_TYPE = 6

    /**
     * The maximum number of frames to process
     */
    @JvmStatic
    var MAX_COMPLETION_LOOP_FRAMES = 5
}
