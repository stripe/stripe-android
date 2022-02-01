package com.stripe.android.stripe3ds2.init.ui;

import androidx.annotation.NonNull;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;

// NOTE: Copied from reference app spec

/**
 * The Customization class shall serve as a superclass for the {@link ButtonCustomization} class,
 * {@link ToolbarCustomization} class, {@link LabelCustomization} class, and
 * {@link TextBoxCustomization} class. This class shall provide methods to pass UI customization
 * parameters to the 3DS2 SDK.
 */
public interface Customization {

    /**
     * @param fontName Font name for the UI element. If not found, default system font used.
     */
    void setTextFontName(@NonNull String fontName) throws InvalidInputException;

    /**
     * @param hexColorCode Color code in Hex format (e.g. "#RRGGBB" or #AARRGGBB)
     * @throws InvalidInputException if there is an error parsing the hex color code
     */
    void setTextColor(@NonNull String hexColorCode) throws InvalidInputException;

    /**
     * @param fontSize Font size for the UI element in scaled-pixels (sp).
     */
    void setTextFontSize(int fontSize) throws InvalidInputException;

    String getTextFontName();

    String getTextColor();

    int getTextFontSize();
}
