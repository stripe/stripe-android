package com.stripe.android.stripe3ds2.init.ui;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;

// NOTE: Copied from reference app spec

/**
 * The ToolbarCustomization class shall provide methods for the app to pass toolbar customization
 * parameters to the 3DS2 SDK.
 *
 * The methods that are inherited from the Customization class can be used to work with toolbar
 * labels.
 */
public interface ToolbarCustomization extends Customization {
    /**
     * @param hexColorCode Color code in Hex format (e.g. "#999999").
     */
    void setBackgroundColor(String hexColorCode) throws InvalidInputException;

    /**
     * @param hexColorCode Color code in Hex format (e.g. "#999999").
     */
    void setStatusBarColor(String hexColorCode) throws InvalidInputException;

    /**
     * @param headerText Text for the header.
     */
    void setHeaderText(String headerText) throws InvalidInputException;

    /**
     * @param buttonText Text for the button. For example, "Cancel".
     */
    void setButtonText(String buttonText) throws InvalidInputException;

    String getBackgroundColor();

    String getStatusBarColor();

    String getHeaderText();

    String getButtonText();
}
