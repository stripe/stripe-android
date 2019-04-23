package com.stripe.samplestore;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

public class ProgressDialogFragment extends DialogFragment {

    @NonNull
    public static ProgressDialogFragment newInstance(@NonNull String message) {
        ProgressDialogFragment fragment = new ProgressDialogFragment();

        final Bundle args = new Bundle();
        args.putString("message", message);

        fragment.setArguments(args);

        return fragment;
    }

    public ProgressDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getMessage());
        return dialog;
    }

    @Nullable
    private String getMessage() {
        return getArguments() != null ? getArguments().getString("message") : null;
    }
}
