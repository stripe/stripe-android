package com.stripe.android.view.threeds2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException;
import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization;
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization;
import com.stripe.android.stripe3ds2.init.ui.TextBoxCustomization;
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization;
import com.stripe.android.stripe3ds2.init.ui.UiCustomization;
import com.stripe.android.utils.ObjectUtils;


/**
 * Customizations for the 3DS2 UI
 */
public class ThreeDS2UiCustomization {

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
