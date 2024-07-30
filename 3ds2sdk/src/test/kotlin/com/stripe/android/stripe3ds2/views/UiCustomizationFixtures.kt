package com.stripe.android.stripe3ds2.views

import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeLabelCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization
import com.stripe.android.stripe3ds2.init.ui.UiCustomization

internal object UiCustomizationFixtures {

    private val DEFAULT_TOOLBAR_CUSTOMIZATION: ToolbarCustomization =
        StripeToolbarCustomization()
    private val DEFAULT_CONTINUE_BUTTON_CUSTOMIZATION: ButtonCustomization =
        StripeButtonCustomization()
    private val DEFAULT_CANCEL_BUTTON_CUSTOMIZATION: ButtonCustomization =
        StripeButtonCustomization()
    private val DEFAULT_NEXT_BUTTON_CUSTOMIZATION: ButtonCustomization =
        StripeButtonCustomization()
    private val DEFAULT_SUBMIT_BUTTON_CUSTOMIZATION: ButtonCustomization =
        StripeButtonCustomization()
    private val DEFAULT_RESEND_BUTTON_CUSTOMIZATION: ButtonCustomization =
        StripeButtonCustomization()
    private val DEFAULT_LABEL_CUSTOMIZATION: LabelCustomization =
        StripeLabelCustomization()
    private val DEFAULT_SELECT_BUTTON_CUSTOMIZATION: ButtonCustomization =
        StripeButtonCustomization()

    internal val DEFAULT: StripeUiCustomization =
        StripeUiCustomization()

    init {
        DEFAULT_TOOLBAR_CUSTOMIZATION.buttonText = "CANCEL"
        DEFAULT_TOOLBAR_CUSTOMIZATION.headerText = "AUTHORIZE PAYMENT"
        DEFAULT_TOOLBAR_CUSTOMIZATION.backgroundColor = "#999999"

        DEFAULT_CONTINUE_BUTTON_CUSTOMIZATION.backgroundColor = "#000000"

        DEFAULT_CANCEL_BUTTON_CUSTOMIZATION.backgroundColor = "#000001"

        DEFAULT_NEXT_BUTTON_CUSTOMIZATION.backgroundColor = "#000010"

        DEFAULT_SUBMIT_BUTTON_CUSTOMIZATION.backgroundColor = "#000100"

        DEFAULT_RESEND_BUTTON_CUSTOMIZATION.backgroundColor = "#001000"

        DEFAULT_SELECT_BUTTON_CUSTOMIZATION.backgroundColor = "#010000"

        DEFAULT_LABEL_CUSTOMIZATION.textColor = "#010000"

        DEFAULT.setToolbarCustomization(DEFAULT_TOOLBAR_CUSTOMIZATION)
        DEFAULT.setButtonCustomization(
            DEFAULT_CONTINUE_BUTTON_CUSTOMIZATION,
            UiCustomization.ButtonType.CONTINUE
        )
        DEFAULT.setButtonCustomization(
            DEFAULT_CANCEL_BUTTON_CUSTOMIZATION,
            UiCustomization.ButtonType.CANCEL
        )
        DEFAULT.setButtonCustomization(
            DEFAULT_NEXT_BUTTON_CUSTOMIZATION,
            UiCustomization.ButtonType.NEXT
        )
        DEFAULT.setButtonCustomization(
            DEFAULT_SUBMIT_BUTTON_CUSTOMIZATION,
            UiCustomization.ButtonType.SUBMIT
        )
        DEFAULT.setButtonCustomization(
            DEFAULT_RESEND_BUTTON_CUSTOMIZATION,
            UiCustomization.ButtonType.RESEND
        )
        DEFAULT.setButtonCustomization(
            DEFAULT_SELECT_BUTTON_CUSTOMIZATION,
            UiCustomization.ButtonType.SELECT
        )
        DEFAULT.setLabelCustomization(DEFAULT_LABEL_CUSTOMIZATION)
    }
}
