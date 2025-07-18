package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko

/**
 * A structure of callbacks used by the Onramp coordinator.
 *
 * @property configurationCallback A callback for when the configuration of the coordinator is completed.
 * @property linkLookupCallback A callback for when a link user lookup has been completed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Poko
class OnrampCallbacks(
    val configurationCallback: OnrampConfigurationCallback,
    val linkLookupCallback: OnrampLinkLookupCallback
)
