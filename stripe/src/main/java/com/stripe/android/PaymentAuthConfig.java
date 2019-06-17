package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization;
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeLabelCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeTextBoxCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization;
import com.stripe.android.stripe3ds2.init.ui.TextBoxCustomization;
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization;
import com.stripe.android.stripe3ds2.init.ui.UiCustomization;
import com.stripe.android.utils.ObjectUtils;

import java.util.Objects;

/**
 * Configuration for authentication mechanisms via {@link PaymentController}
 */
public final class PaymentAuthConfig {

    @Nullable
    private static PaymentAuthConfig sInstance;

    @NonNull
    private static final PaymentAuthConfig DEFAULT = new PaymentAuthConfig.Builder()
            .set3ds2Config(new Stripe3ds2Config.Builder().build())
            .build();

    public static void init(@NonNull PaymentAuthConfig config) {
        sInstance = config;
    }

    @NonNull
    public static PaymentAuthConfig get() {
        return sInstance != null ? sInstance : DEFAULT;
    }

    @VisibleForTesting
    static void reset() {
        sInstance = null;
    }

    @NonNull final Stripe3ds2Config stripe3ds2Config;

    private PaymentAuthConfig(@NonNull Builder builder) {
        stripe3ds2Config = builder.mStripe3ds2Config;
    }

    public static final class Builder {
        private Stripe3ds2Config mStripe3ds2Config;

        @NonNull
        public Builder set3ds2Config(@NonNull Stripe3ds2Config stripe3ds2Config) {
            this.mStripe3ds2Config = stripe3ds2Config;
            return this;
        }

        @NonNull
        public PaymentAuthConfig build() {
            return new PaymentAuthConfig(this);
        }
    }

    public static final class Stripe3ds2Config {
        static final int DEFAULT_TIMEOUT = 5;

        final int timeout;
        @NonNull final ThreeDS2UiCustomization uiCustomization;

        private Stripe3ds2Config(@NonNull Builder builder) {
            timeout = builder.mTimeout;
            uiCustomization = Objects.requireNonNull(builder.mUiCustomization);
        }

        public static final class Builder {
            private int mTimeout = DEFAULT_TIMEOUT;
            private ThreeDS2UiCustomization mUiCustomization = new ThreeDS2UiCustomization();

            @NonNull
            public Builder setTimeout(int timeout) {
                this.mTimeout = timeout;
                return this;
            }

            @NonNull
            public Builder setUiCustomization(@NonNull ThreeDS2UiCustomization uiCustomization) {
                this.mUiCustomization = uiCustomization;
                return this;
            }

            @NonNull
            public Stripe3ds2Config build() {
                return new Stripe3ds2Config(this);
            }
        }
    }

    /**
     * Customization for 3DS2 buttons
     */
    public static final class ThreeDS2ButtonCustomization {

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

    /**
     * Customization for 3DS2 labels
     */
    public static final class ThreeDS2LabelCustomization {

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

    /**
     * Customization for 3DS2 text entry
     */
    public static final class ThreeDS2TextBoxCustomization {

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

    /**
     * Customization for the 3DS2 toolbar
     */
    public static final class ThreeDS2ToolbarCustomization {

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

    /**
     * Customizations for the 3DS2 UI
     */
    public static final class ThreeDS2UiCustomization {

        @NonNull private final UiCustomization mUiCustomization;

        public ThreeDS2UiCustomization() {
            mUiCustomization = new StripeUiCustomization();
        }

        @NonNull
        public UiCustomization getUiCustomization() {
            return mUiCustomization;
        }

        /**
         * The type of button for which customization can be set
         */
        public enum ButtonType {SUBMIT, CONTINUE, NEXT, CANCEL, RESEND}

        @NonNull
        private UiCustomization.ButtonType getUiButtonType(@NonNull ButtonType buttonType)
                throws InvalidInputException {
            if (buttonType == ButtonType.NEXT) {
                return UiCustomization.ButtonType.NEXT;
            } else if (buttonType == ButtonType.CANCEL) {
                return UiCustomization.ButtonType.CANCEL;
            } else if (buttonType == ButtonType.SUBMIT) {
                return UiCustomization.ButtonType.SUBMIT;
            } else if (buttonType == ButtonType.RESEND) {
                return UiCustomization.ButtonType.RESEND;
            } else if (buttonType == ButtonType.CONTINUE) {
                return UiCustomization.ButtonType.CONTINUE;
            } else {
                throw new InvalidInputException(new RuntimeException("Invalid Button Type"));
            }
        }

        /**
         * Set the customization for a particular button
         *
         * @param buttonCustomization The button customization data
         * @param buttonType The type of button to customize
         * @throws InvalidInputException If any customization data is invalid
         */
        public void setButtonCustomization(@NonNull ThreeDS2ButtonCustomization buttonCustomization,
                                           @NonNull ButtonType buttonType)
                throws InvalidInputException {
            mUiCustomization.setButtonCustomization(buttonCustomization.mButtonCustomization,
                    getUiButtonType(buttonType));
        }

        /**
         * Set the customization data for the 3DS2 toolbar
         *
         * @param toolbarCustomization Toolbar customization data
         * @throws InvalidInputException If any customization data is invalid
         */
        public void setToolbarCustomization(@NonNull ThreeDS2ToolbarCustomization toolbarCustomization)
                throws InvalidInputException {
            mUiCustomization.setToolbarCustomization(toolbarCustomization.mToolbarCustomization);
        }

        /**
         * Set the 3DS2 label customization
         *
         * @param labelCustomization Label customization data
         * @throws InvalidInputException If any customization data is invalid
         */
        public void setLabelCustomization(@NonNull ThreeDS2LabelCustomization labelCustomization)
                throws InvalidInputException {
            mUiCustomization.setLabelCustomization(labelCustomization.mLabelCustomization);
        }

        /**
         * Set the 3DS2 text box customization
         *
         * @param textBoxCustomization Text box customization data
         * @throws InvalidInputException If any customization data is invalid
         */
        public void setTextBoxCustomization(@NonNull ThreeDS2TextBoxCustomization textBoxCustomization)
                throws InvalidInputException {
            mUiCustomization.setTextBoxCustomization(textBoxCustomization.mTextBoxCustomization);
        }

        /**
         * Get the customization set for a given button type
         *
         * @param buttonType The button type to get the set customization for
         * @return The customization for the given button type if any or null
         * @throws InvalidInputException If buttonType is invalid
         */
        @Nullable
        public ThreeDS2ButtonCustomization getButtonCustomization(@NonNull ButtonType buttonType)
                throws InvalidInputException {
            final ButtonCustomization buttonCustomization =
                    mUiCustomization.getButtonCustomization(getUiButtonType(buttonType));
            if (buttonCustomization == null) {
                return null;
            } else {
                return new ThreeDS2ButtonCustomization(buttonCustomization);
            }
        }

        /**
         * Get the set 3DS2 toolbar customization
         *
         * @return The toolbar customization if set or null
         */
        @Nullable
        public ThreeDS2ToolbarCustomization getToolbarCustomization() {
            final ToolbarCustomization toolbarCustomization =
                    mUiCustomization.getToolbarCustomization();
            if (toolbarCustomization == null) {
                return null;
            } else {
                return new ThreeDS2ToolbarCustomization(toolbarCustomization);
            }
        }

        /**
         * Get the set 3DS2 label customization
         *
         * @return The label customization if set or null
         */
        @Nullable
        public ThreeDS2LabelCustomization getLabelCustomization() {
            final LabelCustomization labelCustomization = mUiCustomization.getLabelCustomization();
            if (labelCustomization == null) {
                return null;
            } else {
                return new ThreeDS2LabelCustomization(labelCustomization);
            }
        }

        /**
         * Get the set 3DS2 text box customization
         *
         * @return The text box customization if set or null
         */
        @Nullable
        public ThreeDS2TextBoxCustomization getTextBoxCustomization() {
            final TextBoxCustomization textBoxCustomization =
                    mUiCustomization.getTextBoxCustomization();
            if (textBoxCustomization == null) {
                return null;
            } else {
                return new ThreeDS2TextBoxCustomization(textBoxCustomization);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof ThreeDS2UiCustomization &&
                    typedEquals((ThreeDS2UiCustomization) obj));
        }

        private boolean typedEquals(@NonNull ThreeDS2UiCustomization uiCustomization) {
            return ObjectUtils.equals(mUiCustomization, uiCustomization.mUiCustomization);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(mUiCustomization);
        }
    }
}
