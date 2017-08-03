package com.stripe.android.view;

/**
 * Class used to encapsulate the functionality of "backing up" via the delete/backspace key
 * from one text field to the previous. We use this to simulate multiple fields being all part
 * of the same EditText, so a delete key entry from field N+1 deletes the last character in
 * field N. Each BackUpFieldDeleteListener is attached to the N+1 field, from which it gets
 * its {@link #onDeleteEmpty()} call, and given a reference to the N field, upon which
 * it will be acting.
 */
class BackUpFieldDeleteListener implements StripeEditText.DeleteEmptyListener {

    private StripeEditText backUpTarget;

    BackUpFieldDeleteListener(StripeEditText backUpTarget) {
        this.backUpTarget = backUpTarget;
    }

    @Override
    public void onDeleteEmpty() {
        String fieldText = backUpTarget.getText().toString();
        if (fieldText.length() > 1) {
            backUpTarget.setText(
                    fieldText.substring(0, fieldText.length() - 1));
        }
        backUpTarget.requestFocus();
        backUpTarget.setSelection(backUpTarget.length());
    }
}
