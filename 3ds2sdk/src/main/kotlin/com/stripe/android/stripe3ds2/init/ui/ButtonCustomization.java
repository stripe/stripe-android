package com.stripe.android.stripe3ds2.init.ui;

import androidx.annotation.NonNull;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;

// NOTE: Copied from reference app spec

/**
 * The ButtonCustomization class shall provide methods for the app to pass button customization
 * parameters to the 3DS2 SDK.
 * <p>
 * The methods that are inherited from the Customization class can be used to work with button
 * labels.
 */
public interface ButtonCustomization extends Customization {
    /**
     * @param hexColorCode Color code in Hex format for the background color of the button
     *                     (e.g. "#RRGGBB" or #AARRGGBB).
     * @throws InvalidInputException if there is an error parsing the hex color code
     */
    void setBackgroundColor(@NonNull String hexColorCode) throws InvalidInputException;

    /**
     * @param cornerRadius the radius in pixels
     */
    void setCornerRadius(int cornerRadius) throws InvalidInputException;

    String getBackgroundColor();

    int getCornerRadius();
}
