package com.stripe.android.view;

import android.support.annotation.NonNull;

public interface ActivityStarter<StartDataType> {
    void start(@NonNull StartDataType data);
}
