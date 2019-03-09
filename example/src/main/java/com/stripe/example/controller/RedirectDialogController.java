package com.stripe.example.controller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.stripe.example.R;

/**
 * Controller for the redirect dialog used to direct users out of the application.
 */
public class RedirectDialogController {

    @NonNull private final Activity mActivity;
    private AlertDialog mAlertDialog;

    public RedirectDialogController(@NonNull Activity appCompatActivity) {
        mActivity = appCompatActivity;
    }

    public void showDialog(final String url) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        View dialogView = LayoutInflater.from(mActivity).inflate(R.layout.polling_dialog, null);

        TextView linkView = dialogView.findViewById(R.id.tv_link_redirect);
        linkView.setText(R.string.verify);
        linkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                mActivity.startActivity(browserIntent);
            }
        });
        builder.setView(dialogView);

        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    public void dismissDialog() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }
}
