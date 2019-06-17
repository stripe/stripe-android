package com.stripe.android.view.threeds2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.init.ui.StripeTextBoxCustomization;
import com.stripe.android.stripe3ds2.init.ui.TextBoxCustomization;
import com.stripe.android.utils.ObjectUtils;

/**
 * Customization for 3DS2 text entry
 */
public class ThreeDS2TextBoxCustomization {

    @NonNull final TextBoxCustomization mTextBoxCustomization;

    public ThreeDS2TextBoxCustomization() {
        mTextBoxCustomization = new StripeTextBoxCustomization();
    }

    ThreeDS2TextBoxCustomization(@NonNull TextBoxCustomization textBoxCustomization) {
        mTextBoxCustomization = textBoxCustomization;
    }

    /**
     * Set the width of the border around the text entry box
     *
     * @param borderWidth Width of the border in pixels
     * @throws InvalidInputException If the border width is less than 0
     */
    public void setBorderWidth(int borderWidth) throws InvalidInputException {
        mTextBoxCustomization.setBorderWidth(borderWidth);
    }

    /**
     * Get the set border width
     *
     * @return The border width in pixels
     */
    public int getBorderWidth() {
        return mTextBoxCustomization.getBorderWidth();
    }

    /**
     * Set the color of the border around the text entry box
     *
     * @param hexColor The border's color in the format #RRGGBB or #AARRGGBB
     * @throws InvalidInputException If the color cannot be parsed
     */
    public void setBorderColor(@NonNull String hexColor) throws InvalidInputException {
        mTextBoxCustomization.setBorderColor(hexColor);
    }

    /**
     * Get the border's color
     *
     * @return The border's color if set or null
     */
    @Nullable
    public String getBorderColor() {
        return mTextBoxCustomization.getBorderColor();
    }

    /**
     * Set the corner radius of the text entry box
     *
     * @param cornerRadius The corner radius in pixels
     * @throws InvalidInputException If the corner radius is less than 0
     */
    public void setCornerRadius(int cornerRadius) throws InvalidInputException {
        mTextBoxCustomization.setCornerRadius(cornerRadius);
    }

    /**
     * Get the text entry box's corner radius
     *
     * @return The corner radius in pixels
     */
    public int getCornerRadius() {
        return mTextBoxCustomization.getCornerRadius();
    }

    /**
     * Set the font for text entry
     *
     * @param fontName The name of the font. The system default is used if not found.
     * @throws InvalidInputException If the font name is null or empty.
     */
    public void setTextFontName(@NonNull String fontName) throws InvalidInputException {
        mTextBoxCustomization.setTextFontName(fontName);
    }

    /**
     * Set the text color for text entry
     *
     * @param hexColor The text color in the format #RRGGBB or #AARRGGBB
     * @throws InvalidInputException If the color cannot be parsed
     */
    public void setTextColor(@NonNull String hexColor) throws InvalidInputException {
        mTextBoxCustomization.setTextColor(hexColor);
    }

    /**
     * Set the text entry font size
     *
     * @param fontSize The font size in scaled-pixels (sp)
     * @throws InvalidInputException If the font size is 0 or less
     */
    public void setTextFontSize(int fontSize) throws InvalidInputException {
        mTextBoxCustomization.setTextFontSize(fontSize);
    }

    /**
     * Get the text entry box's font name
     *
     * @return The name of the font if set or null
     */
    @Nullable
    public String getTextFontName() {
        return mTextBoxCustomization.getTextFontName();
    }

    /**
     * Get the text entry box's text color
     *
     * @return The color of the text if set or null
     */
    @Nullable
    public String getTextColor() {
        return mTextBoxCustomization.getTextColor();
    }

    /**
     * Get the text entry box's font size
     *
     * @return The font size in scaled-pixels (sp)
     */
    public int getTextFontSize() {
        return mTextBoxCustomization.getTextFontSize();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof ThreeDS2TextBoxCustomization &&
                typedEquals((ThreeDS2TextBoxCustomization) obj));
    }

    private boolean typedEquals(@NonNull ThreeDS2TextBoxCustomization uiCustomization) {
        return ObjectUtils.equals(mTextBoxCustomization, uiCustomization.mTextBoxCustomization);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mTextBoxCustomization);
    }
}
