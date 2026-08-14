package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

/**
 * Renders the redesigned Link DS 3.0 theme in place of the current Link theme. Only affects sessions
 * whose manifest theme is `link_light`, so use an Instant Bank Payments flow to see it.
 *
 * The flag is read when the theme is resolved as the flow launches, so flipping this takes effect on
 * the next session rather than retroactively.
 */
data class LinkDs3Setting(
    override val selectedOption: Boolean = false,
    override val key: String = "link_ds3",
) : Saveable<Boolean>, SingleChoiceSetting<Boolean>(
    displayName = "Link DS 3.0 theme",
    options = listOf(
        Option("Off", false),
        Option("On", true),
    ),
    selectedOption = selectedOption
) {
    override fun lasRequest(
        body: LinkAccountSessionBody,
    ): LinkAccountSessionBody = body

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
    ): PaymentIntentBody = body

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: Boolean): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }

    override fun convertToValue(value: String): Boolean = value == "true"

    override fun convertToString(value: Boolean): String = value.toString()
}
