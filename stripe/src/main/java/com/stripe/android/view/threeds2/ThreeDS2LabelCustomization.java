package com.stripe.android.view.threeds2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeLabelCustomization;
import com.stripe.android.utils.ObjectUtils;

/**
 * Customization for 3DS2 labels
 */
public class ThreeDS2LabelCustomization {

    @NonNull final LabelCustomization mLabelCustomization;

    public ThreeDS2LabelCustomization() {
        mLabelCustomization = new StripeLabelCustomization();
    }

    ThreeDS2LabelCustomization(@NonNull LabelCustomization labelCustomization) {
        mLabelCustomization = labelCustomization;
    }

    /**
     * Set the text color for heading labels
     *
     * @param hexColor The heading labels's text color in the format #RRGGBB or #AARRGGBB
     * @throws InvalidInputException If the color cannot be parsed
     */
    public void setHeadingTextColor(@NonNull String hexColor) throws InvalidInputException {
        mLabelCustomization.setHeadingTextColor(hexColor);
    }

    /**
     * Set the heading label's font
     *
     * @param fontName The name of the font for heading labels. Defaults to system font if not found
     * @throws InvalidInputException If the font name is null or empty
     */
    public void setHeadingTextFontName(@NonNull String fontName) throws InvalidInputException {
        mLabelCustomization.setHeadingTextFontName(fontName);
    }

    /**
     * Set the heading label's text size
     *
     * @param fontSize The size of the heading label in scaled-pixels (sp).
     * @throws InvalidInputException If the font size is 0 or less
     */
    public void setHeadingTextFontSize(int fontSize) throws InvalidInputException {
        mLabelCustomization.setHeadingTextFontSize(fontSize);
    }

    /**
     * Get the heading label's text color
     *
     * @return the heading label's text color if set or null
     */
    @Nullable
    public String getHeadingTextColor() {
        return mLabelCustomization.getHeadingTextColor();
    }

    /**
     * Get the heading label's font name
     *
     * @return The headling label's font name if set or null
     */
    @Nullable
    public String getHeadingTextFontName() {
        return mLabelCustomization.getHeadingTextFontName();
    }

    /**
     * Get the heading label's font size
     *
     * @return The heading label's font size in scaled-pixels (sp)
     */
    public int getHeadingTextFontSize() {
        return mLabelCustomization.getHeadingTextFontSize();
    }

    /**
     * Set the label's font
     *
     * @param fontName The name of the font. Defaults to system font if not found
     * @throws InvalidInputException If the font name is null or empty
     */
    public void setTextFontName(@NonNull String fontName) throws InvalidInputException {
        mLabelCustomization.setTextFontName(fontName);
    }

    /**
     * Set the label's text color
     *
     * @param hexColor The labels's text color in the format #RRGGBB or #AARRGGBB
     * @throws InvalidInputException If the color cannot be parsed
     */
    public void setTextColor(@NonNull String hexColor) throws InvalidInputException {
        mLabelCustomization.setTextColor(hexColor);
    }

    /**
     * Set the label's text size
     *
     * @param fontSize The label's font size in scaled-pixels (sp)
     * @throws InvalidInputException If the font size is 0 or less
     */
    public void setTextFontSize(int fontSize) throws InvalidInputException {
        mLabelCustomization.setTextFontSize(fontSize);
    }

    /**
     * Get the label's font name
     *
     * @return The label's font name if set or null
     */
    @Nullable
    public String getTextFontName() {
        return mLabelCustomization.getTextFontName();
    }

    /**
     * Get the label's text color
     *
     * @return The label's text color if set or null
     */
    @Nullable
    public String getTextColor() {
        return mLabelCustomization.getTextColor();
    }

    /**
     * Get the label's text size
     *
     * @return The font size of the label in scaled-pixels (sp)
     */
    public int getTextFontSize() {
        return mLabelCustomization.getTextFontSize();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof ThreeDS2LabelCustomization &&
                typedEquals((ThreeDS2LabelCustomization) obj));
    }

    private boolean typedEquals(@NonNull ThreeDS2LabelCustomization uiCustomization) {
        return ObjectUtils.equals(mLabelCustomization, uiCustomization.mLabelCustomization);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mLabelCustomization);
    }
}
