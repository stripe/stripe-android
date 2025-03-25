package com.stripe.android.model

/**
 * Flags declared here will be parsed by [com.stripe.android.model.parsers.ElementsSessionJsonParser] and included
 * in the [ElementsSession] object.
 */
internal enum class ElementsSessionFlags(val flagValue: String) {
    ELEMENTS_DISABLE_FC_LITE("elements_disable_fc_lite")
}
