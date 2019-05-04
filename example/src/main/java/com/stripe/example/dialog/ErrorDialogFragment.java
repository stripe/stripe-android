package com.stripe.example.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.stripe.example.R;

public class ErrorDialogFragment extends DialogFragment {
    public static ErrorDialogFragment newInstance(int titleId, String message) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle args = new Bundle();
        args.putInt("titleId", titleId);
        args.putString("message", message);

        fragment.setArguments(args);

        return fragment;
    }

    public ErrorDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int titleId = getArguments().getInt("titleId");
        String message = getArguments().getString("message");

        return new AlertDialog.Builder(getActivity())
            .setTitle(titleId)
            .setMessage(message)
            .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
            .create();
    }
}
