package com.stripe.android.view;

import android.support.annotation.NonNull;

public interface AuthActivityStarter<StartDataType> {
    void start(@NonNull StartDataType data);
}
