package com.stripe.android;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class FakeLogger implements Logger {
    @Override
    public void error(@NotNull String msg, @Nullable Throwable t) {

    }

    @Override
    public void info(@NotNull String msg) {

    }
}
