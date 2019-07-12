package com.stripe.example.controller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.stripe.android.model.Card;
import com.stripe.example.R;

import java.util.Map;

/**
 * Controller for the redirect dialog used to direct users out of the application.
 */
public class RedirectDialogController {

    @NonNull private final Activity mActivity;
    @Nullable private AlertDialog mAlertDialog;

    public RedirectDialogController(@NonNull Activity activity) {
        mActivity = activity;
    }

    public void showDialog(@NonNull final String url, @NonNull Map<String, ?> sourceCardData) {
        final String brand = (String) sourceCardData.get("brand");
        mAlertDialog = new AlertDialog.Builder(mActivity)
                .setTitle(mActivity.getString(R.string.authentication_dialog_title))
                .setMessage(mActivity.getString(R.string.authentication_dialog_message,
                        brand, sourceCardData.get("last4")))
                .setIcon(Card.getBrandIcon(brand))
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                        mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))))
                .setNegativeButton(android.R.string.no, null)
                .create();
        mAlertDialog.show();
    }

    public void dismissDialog() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }
}
