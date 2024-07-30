package com.stripe.android.stripe3ds2.init.ui;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;

// NOTE: Copied from reference app spec

/**
 * The UiCustomization class shall provide the functionality required to customize the 3DS2 SDK UI
 * elements. An object of this class holds various UI-related parameters.
 */
public interface UiCustomization {

    enum ButtonType {
        SUBMIT, CONTINUE, NEXT, CANCEL, RESEND, SELECT
    }

    /**
     * The setButtonCustomization method shall accept a ButtonCustomization object along with a
     * predefined button type. The 3DS3 SDK uses this object for customizing buttons.
     *
     * Note: The SDK will maintain a dictionary of buttons passed via this method for use during
     * customization.
     */
    void setButtonCustomization(ButtonCustomization buttonCustomization, ButtonType buttonType)
            throws InvalidInputException;

    /**
     * This method is a variation of the setButtonCustomization method.
     * The setButtonCustomization method shall accept a ButtonCustomization object and an
     * implementer-specific button type. The 3DS2 SDK uses this object for customizing buttons.
     *
     * Note: This method shall be used when the SDK implementer wants to use a button type that is
     * not included in the predefined Enum ButtonType.
     * The SDK implementer shall maintain a dictionary of buttons passed via this method for use
     * during customization.
     *
     * @param buttonCustomization A ButtonCustomization object.
     * @param buttonType          Implementer-specific button type.
     */
    void setButtonCustomization(ButtonCustomization buttonCustomization, String buttonType)
            throws InvalidInputException;

    /**
     * The setToolbarCustomization method shall accept a ToolbarCustomization object. The 3DS SDK
     * uses this object for customizing toolbars.
     */
    void setToolbarCustomization(ToolbarCustomization toolbarCustomization)
            throws InvalidInputException;

    /**
     * The setLabelCustomization method shall accept a LabelCustomization object. The 3DS SDK uses
     * this object for customizing labels.
     *
     * @param labelCustomization A LabelCustomization object.
     */
    void setLabelCustomization(LabelCustomization labelCustomization) throws InvalidInputException;

    /**
     * The setTextBoxCustomization method shall accept a TextBoxCustomization object. The 3DS SDK
     * uses this object for customizing text boxes.
     *
     * @param textBoxCustomization A TextBoxCustomization object.
     */
    void setTextBoxCustomization(TextBoxCustomization textBoxCustomization)
            throws InvalidInputException;

    /**
     * @param hexColorCode Color code in Hex format (e.g. "#999999").
     */
    void setAccentColor(String hexColorCode) throws InvalidInputException;

    /**
     * @return ButtonCustomization object for the specified {@link ButtonType}.
     */
    ButtonCustomization getButtonCustomization(ButtonType buttonType) throws InvalidInputException;

    /**
     * @param buttonType Implementer-specific button type.
     * @return This method returns a ButtonCustomization object.
     */
    ButtonCustomization getButtonCustomization(String buttonType) throws InvalidInputException;

    ToolbarCustomization getToolbarCustomization();

    LabelCustomization getLabelCustomization();

    TextBoxCustomization getTextBoxCustomization();

    String getAccentColor();
}
