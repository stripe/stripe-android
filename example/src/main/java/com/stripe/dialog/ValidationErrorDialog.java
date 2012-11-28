package com.stripe.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.stripe.R;

public class ValidationErrorDialog {

    AlertDialog dialog;

    public ValidationErrorDialog(Context context, String error) {
        String title = context.getResources().getString(R.string.validationErrors);

        dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(error)
                .setPositiveButton("OK", new DismissOnClickListener())
                .create();
    }

    public void show() {
        dialog.show();
    }

    private class DismissOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
        }
    }
}
