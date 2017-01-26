package com.stripe.example.controller;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.stripe.example.dialog.MessageDialogFragment;

/**
 * A convenience class to handle displaying error dialogs.
 */
public class MessageDialogHandler {

    FragmentManager mFragmentManager;

    public MessageDialogHandler(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    public void showMessage(int titleId, String message) {
        DialogFragment fragment = MessageDialogFragment.newInstance(
                titleId, message);
        fragment.show(mFragmentManager, "message_dialog");
    }
}
