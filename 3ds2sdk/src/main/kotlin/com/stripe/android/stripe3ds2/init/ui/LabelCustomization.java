package com.stripe.android.stripe3ds2.init.ui;

import androidx.annotation.NonNull;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;

// NOTE: Copied from reference app spec

/**
 * The LabelCustomization class provides methods for the app to pass label customization parameters
 * to the SDK.
 *
 * The methods that are inherited from the Customization class can be used to work with non-heading
 * labels in the UI.
 */
public interface LabelCustomization extends Customization {
    /**
     * @param hexColorCode Colour code in Hex format (e.g. "#999999").
     */
    void setHeadingTextColor(@NonNull String hexColorCode) throws InvalidInputException;

    /**
     * @param fontName Font type for the heading label text.
     */
    void setHeadingTextFontName(@NonNull String fontName) throws InvalidInputException;

    void setHeadingTextFontSize(int fontSize) throws InvalidInputException;

    /**
     * @return the hex color code of the heading label text
     */
    String getHeadingTextColor();

    String getHeadingTextFontName();

    int getHeadingTextFontSize();
}
