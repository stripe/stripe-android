package com.stripe;

import android.content.Context;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Validation {
    public final boolean isValid;
    public final Set<StripeError> errors;

    protected Validation() {
        this(new StripeError[]{});
    }

    protected Validation(StripeError... errors) {
        this.isValid = errors.length == 0;
        this.errors = new HashSet<StripeError>(Arrays.asList(errors));
    }

    protected Validation(Validation ... validations) {
        Set<StripeError> errorList = new HashSet<StripeError>();
        boolean isValid = true;
        for (Validation validation : validations) {
            if (!validation.isValid) {
                isValid = false;
            }
            errorList.addAll(validation.errors);
        }

        this.isValid = isValid;
        this.errors = errorList;
    }

    public String getLocalizedErrors(Context context) {
        Set<String> errors = new HashSet<String>();
        for (StripeError error : this.errors) {
            errors.add(error.getLocalizedString(context));
        }
        return TextUtils.join("\n", errors);
    }

}
