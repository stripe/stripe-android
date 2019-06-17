package com.stripe.android.view.threeds2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization;
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization;
import com.stripe.android.utils.ObjectUtils;

/**
 * Customization for the 3DS2 toolbar
 */
public class ThreeDS2ToolbarCustomization {

    @NonNull final ToolbarCustomization mToolbarCustomization;

    public ThreeDS2ToolbarCustomization() {
        mToolbarCustomization = new StripeToolbarCustomization();
    }

    ThreeDS2ToolbarCustomization(@NonNull ToolbarCustomization toolbarCustomization) {
        mToolbarCustomization = toolbarCustomization;
    }

    /**
     * Set the toolbar's background color
     *
     * @param hexColor The background color in the format #RRGGBB or #AARRGGBB
     * @throws InvalidInputException If the color cannot be parsed
     */
    public void setBackgroundColor(@NonNull String hexColor) throws InvalidInputException {
        mToolbarCustomization.setBackgroundColor(hexColor);
    }

    /**
     * Set the toolbar's title
     *
     * @param headerText The toolbar's title text
     * @throws InvalidInputException if the title is null or empty
     */
    public void setHeaderText(@NonNull String headerText) throws InvalidInputException {
        mToolbarCustomization.setHeaderText(headerText);
    }

    /**
     * Set the toolbar's cancel button text
     *
     * @param buttonText The cancel button's text
     * @throws InvalidInputException If the button text is null or empty
     */
    public void setButtonText(@NonNull String buttonText) throws InvalidInputException {
        mToolbarCustomization.setButtonText(buttonText);
    }

    /**
     * Get the toolbar's background color
     *
     * @return The background color if set or null
     */
    @Nullable
    public String getBackgroundColor() {
        return mToolbarCustomization.getBackgroundColor();
    }

    /**
     * Get the toolbar's title text
     *
     * @return The title text if set or null
     */
    @Nullable
    public String getHeaderText() {
        return mToolbarCustomization.getHeaderText();
    }

    /**
     * Get the toolbar's cancel button text
     *
     * @return The cancel button's text if set or null
     */
    @Nullable
    public String getButtonText() {
        return mToolbarCustomization.getButtonText();
    }

    /**
     * Set the font for the title text
     *
     * @param fontName The name of the font. System default is used if not found
     * @throws InvalidInputException If the font name is null or empty
     */
    public void setTextFontName(@NonNull String fontName) throws InvalidInputException {
        mToolbarCustomization.setTextFontName(fontName);
    }

    /**
     * Set the color of the title text
     *
     * @param hexColor The title's text color in the format #RRGGBB or #AARRGGBB
     * @throws InvalidInputException If the color cannot be parsed
     */
    public void setTextColor(@NonNull String hexColor) throws InvalidInputException {
        mToolbarCustomization.setTextColor(hexColor);
    }

    /**
     * Set the title text's font size
     *
     * @param fontSize The size of the title text in scaled-pixels (sp)
     * @throws InvalidInputException If the font size is 0 or less
     */
    public void setTextFontSize(int fontSize) throws InvalidInputException {
        mToolbarCustomization.setTextFontSize(fontSize);
    }

    /**
     * Get the title text font name
     *
     * @return The font name if set or null
     */
    @Nullable
    public String getTextFontName() {
        return mToolbarCustomization.getTextFontName();
    }

    /**
     * Get the title text color
     *
     * @return The title text color if set or null
     */
    @Nullable
    public String getTextColor() {
        return mToolbarCustomization.getTextColor();
    }

    /**
     * Get the title text font size
     *
     * @return The font size in scaled-pixels (sp)
     */
    public int getTextFontSize() {
        return mToolbarCustomization.getTextFontSize();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof ThreeDS2ToolbarCustomization &&
                typedEquals((ThreeDS2ToolbarCustomization) obj));
    }

    private boolean typedEquals(@NonNull ThreeDS2ToolbarCustomization uiCustomization) {
        return ObjectUtils.equals(mToolbarCustomization, uiCustomization.mToolbarCustomization);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mToolbarCustomization);
    }
}
