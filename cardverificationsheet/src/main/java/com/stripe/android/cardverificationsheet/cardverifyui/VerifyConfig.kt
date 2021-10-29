package com.stripe.android.cardverificationsheet.cardverifyui

import androidx.annotation.Keep
import com.stripe.android.cardverificationsheet.framework.time.seconds

@Keep
object VerifyConfig {

    /**
     * The maximum duration for which to search for a card number once sufficient verification
     * images have been found.
     */
    @JvmStatic
    var PAN_SEARCH_DURATION = 5.seconds

    /**
     * The maximum duration for which to search for both a card number and good verification images.
     */
    @JvmStatic
    var PAN_AND_CARD_SEARCH_DURATION = 10.seconds

    /**
     * Once this number of frames with matching card numbers are found, stop looking for card
     * numbers.
     */
    @JvmStatic
    var DESIRED_OCR_AGREEMENT = 3

    /**
     * Once this number of frames with matching card numbers are found, reduce the search time to
     * PAN_SEARCH_DURATION.
     */
    @JvmStatic
    var MINIMUM_PAN_AGREEMENT = 2

    /**
     * Once this number of frames with a clearly centered card are found, stop looking for images
     * with clearly centered cards.
     */
    @JvmStatic
    var DESIRED_SIDE_COUNT = 5

    /**
     * Display the wrong card notification to the user for this duration.
     */
    @JvmStatic
    var WRONG_CARD_DURATION = 2.seconds

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
