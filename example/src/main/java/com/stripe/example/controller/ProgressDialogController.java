package com.stripe.example.controller;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;

import com.stripe.example.R;
import com.stripe.example.dialog.ProgressDialogFragment;

/**
 * Created by mrmcduff on 12/5/16.
 */
public class ProgressDialogController {

    private FragmentManager mFragmentManager;
    private ProgressDialogFragment mProgressFragment;

    public ProgressDialogController(@NonNull FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
        mProgressFragment = ProgressDialogFragment.newInstance(R.string.progressMessage);
    }

    public void startProgress() {
        mProgressFragment.show(mFragmentManager, "progress");
    }

    public void finishProgress() {
        mProgressFragment.dismiss();
    }
}
