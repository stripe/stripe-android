package com.stripe.android.stripe3ds2.init.ui;

import androidx.annotation.NonNull;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;

// NOTE: Copied from reference app spec

/**
 * The TextBoxCustomization class shall provide methods for the app to pass text box customization
 * parameters to the 3DS2 SDK.
 *
 * The methods that are inherited from the Customization class can be used to set the properties of
 * user-entered text in text boxes.
 */
public interface TextBoxCustomization extends Customization {
    /**
     * @param borderWidth Width (integer value) of the text box border.
     */
    void setBorderWidth(int borderWidth) throws InvalidInputException;

    /**
     * The border must exist before this method is called.
     *
     * @return the width of the text box border.
     */
    int getBorderWidth();

    /**
     * @param hexColorCode Color code in Hex format for the border of the text box (e.g. "#999999").
     */
    void setBorderColor(@NonNull String hexColorCode) throws InvalidInputException;

    /**
     * The SDK implementer shall ensure that the border exists before this method is called.
     * @return the hex color code (as a string) of the text box border.
     */
    String getBorderColor();

    void setCornerRadius(int cornerRadius) throws InvalidInputException;

    int getCornerRadius();

    /**
     * @param hexColorCode Color code in Hex format for the color of the hint text (e.g. "#999999").
     */
    void setHintTextColor(@NonNull String hexColorCode) throws InvalidInputException;

    String getHintTextColor();
}
