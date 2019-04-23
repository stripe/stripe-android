package com.stripe.android.view.i18n;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A class that provides a {@link ErrorMessageTranslator} for translating server-provided error
 * messages, as defined in https://stripe.com/docs/api/errors.
 *
 * See {@link com.stripe.android.view.PaymentMethodsActivity} for example usage.
 *
 * To use a custom {@link ErrorMessageTranslator} in your app,
 * override {@link Application#onCreate()} in your app's Application subclass and call
 * {@link #setErrorMessageTranslator(ErrorMessageTranslator)}.
 *
 * <pre>
 * <code>
 * public class MyApp extends Application {
 *   public void onCreate() {
 *     super.onCreate();
 *     TranslatorManager.setErrorMessageTranslator(new MyErrorMessageTranslator());
 *   }
 * }
 * </code>
 * </pre>
 */
public class TranslatorManager {
    @NonNull
    private static final ErrorMessageTranslator DEFAULT_ERROR_MESSAGE_TRANSLATOR =
            new ErrorMessageTranslator.Default();

    @Nullable
    private static ErrorMessageTranslator mErrorMessageTranslator;

    private TranslatorManager() {
    }

    @NonNull
    public static ErrorMessageTranslator getErrorMessageTranslator() {
        return mErrorMessageTranslator != null ?
                mErrorMessageTranslator : DEFAULT_ERROR_MESSAGE_TRANSLATOR;
    }

    public static void setErrorMessageTranslator(
            @Nullable ErrorMessageTranslator errorMessageTranslator) {
        mErrorMessageTranslator = errorMessageTranslator;
    }
}
