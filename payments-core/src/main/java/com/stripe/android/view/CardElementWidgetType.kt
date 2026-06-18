package com.stripe.android.view

internal enum class CardElementWidgetType(val analyticsValue: String) {
    CardInputWidget("card_input_widget"),
    CardFormView("card_form_view"),
    CardMultilineWidget("card_multiline_widget"),
}
