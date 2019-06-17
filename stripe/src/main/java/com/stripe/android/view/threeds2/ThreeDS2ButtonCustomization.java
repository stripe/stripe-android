package com.stripe.android.view.threeds2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization;
import com.stripe.android.utils.ObjectUtils;

/**
 * Customization for 3DS2 buttons
 */
public class ThreeDS2ButtonCustomization {

    @NonNull final ButtonCustomization mButtonCustomization;

    public ThreeDS2ButtonCustomization() {
        mButtonCustomization = new StripeButtonCustomization();
    }

    ThreeDS2ButtonCustomization(@NonNull ButtonCustomization buttonCustomization) {
        mButtonCustomization = buttonCustomization;
    }

    /**
     * Set the button's background color
     *
     * @param hexColor The button's background color in the format #RRGGBB or #AARRGGBB
     * @throws InvalidInputException If the color cannot be parsed
     */
    public void setBackgroundColor(@NonNull String hexColor) throws InvalidInputException {
        mButtonCustomization.setBackgroundColor(hexColor);
    }

    /**
     * Set the corner radius of the button
     *
     * @param cornerRadius The radius of the button in pixels
     * @throws InvalidInputException If the corner radius is less than 0
     */
    public void setCornerRadius(int cornerRadius) throws InvalidInputException {
        mButtonCustomization.setCornerRadius(cornerRadius);
    }

    /**
     * Get the button's background color
     *
     * @return The set background color or null if not set
     */
    @Nullable
    public String getBackgroundColor() {
        return mButtonCustomization.getBackgroundColor();
    }

    /**
     * Get the button's set corner radius
     *
     * @return The set corner radius in pixels
     */
    public int getCornerRadius() {
        return mButtonCustomization.getCornerRadius();
    }

    /**
     * Set the button's text font
     *
     * @param fontName The name of the font for the button's text. If not found, default system
     *         font used
     * @throws InvalidInputException If font name is null or empty
     */
    public void setTextFontName(@NonNull String fontName) throws InvalidInputException {
        mButtonCustomization.setTextFontName(fontName);
    }

    /**
     * Set the button's text color
     *
     * @param hexColor The button's text color in the format #RRGGBB or #AARRGGBB
     * @throws InvalidInputException If the color cannot be parsed
     */
    public void setTextColor(@NonNull String hexColor) throws InvalidInputException {
        mButtonCustomization.setTextColor(hexColor);
    }

    /**
     * Set the button's text size
     *
     * @param fontSize The size of the font in scaled-pixels (sp)
     * @throws InvalidInputException If the font size is 0 or less
     */
    public void setTextFontSize(int fontSize) throws InvalidInputException {
        mButtonCustomization.setTextFontSize(fontSize);
    }

    /**
     * Get the set button's text font name
     *
     * @return The the font name or null if not set
     */
    @Nullable
    public String getTextFontName() {
        return mButtonCustomization.getTextFontName();
    }

    /**
     * Get the button's text color
     *
     * @return The button's text color or null if not set
     */
    @Nullable
    public String getTextColor() {
        return mButtonCustomization.getTextColor();
    }

    /**
     * Get the button's font size
     *
     * @return The button's font size in scaled-pixels (sp)
     */
    public int getTextFontSize() {
        return mButtonCustomization.getTextFontSize();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof ThreeDS2ButtonCustomization &&
                typedEquals((ThreeDS2ButtonCustomization) obj));
    }

    private boolean typedEquals(@NonNull ThreeDS2ButtonCustomization uiCustomization) {
        return ObjectUtils.equals(mButtonCustomization, uiCustomization.mButtonCustomization);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mButtonCustomization);
    }
}
