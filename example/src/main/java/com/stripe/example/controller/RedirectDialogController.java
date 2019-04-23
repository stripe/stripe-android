package com.stripe.example.controller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.stripe.example.R;

/**
 * Controller for the redirect dialog used to direct users out of the application.
 */
public class RedirectDialogController {

    @NonNull private final Activity mActivity;
    private AlertDialog mAlertDialog;

    public RedirectDialogController(@NonNull Activity activity) {
        mActivity = activity;
    }

    public void showDialog(@NonNull final String url) {
        final View dialogView = mActivity.getLayoutInflater()
                .inflate(R.layout.polling_dialog, null);

        final TextView linkView = dialogView.findViewById(R.id.tv_link_redirect);
        linkView.setText(R.string.verify);
        linkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        mAlertDialog = new AlertDialog.Builder(mActivity)
                .setView(dialogView)
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
