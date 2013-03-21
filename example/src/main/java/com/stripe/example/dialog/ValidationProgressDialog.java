package com.stripe.example.dialog;

import android.app.ProgressDialog;
import android.content.Context;

public class ValidationProgressDialog  {

    Context context;
    String message;

    private ProgressDialog dialog;

    public ValidationProgressDialog(Context context, String message) {
        this.context = context;
        this.message = message;
    }

    public void show() {
        this.dialog = ProgressDialog.show(context, null, message, true, false);
    }

    public void dismiss() {
        if (this.dialog != null) {
            this.dialog.dismiss();
        }
    }
}
