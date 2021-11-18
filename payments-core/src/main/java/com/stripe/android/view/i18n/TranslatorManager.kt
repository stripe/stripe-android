package com.stripe.android.view.i18n

import android.app.Application
import com.stripe.android.core.StripeError
import com.stripe.android.view.i18n.TranslatorManager.setErrorMessageTranslator

/**
 * A class that provides a [ErrorMessageTranslator] for translating server-provided error
 * messages, as defined in [Stripe API Errors Reference](https://stripe.com/docs/api/errors).
 *
 * See [com.stripe.android.view.PaymentMethodsActivity] for example usage.
 *
 * To use a custom [ErrorMessageTranslator] in your app, override [Application.onCreate] in
 * your app's Application subclass and call [setErrorMessageTranslator].
 *
 * <pre>
 * public class MyApp extends Application {
 *   public void onCreate() {
 *     super.onCreate();
 *     TranslatorManager.setErrorMessageTranslator(new MyErrorMessageTranslator());
 *   }
 * }
 * </pre>
 */
object TranslatorManager {
    private val DEFAULT_ERROR_MESSAGE_TRANSLATOR = Default()

    private var errorMessageTranslator: ErrorMessageTranslator? = null

    fun getErrorMessageTranslator(): ErrorMessageTranslator {
        return errorMessageTranslator ?: DEFAULT_ERROR_MESSAGE_TRANSLATOR
    }

    fun setErrorMessageTranslator(errorMessageTranslator: ErrorMessageTranslator?) {
        this.errorMessageTranslator = errorMessageTranslator
    }

    private class Default : ErrorMessageTranslator {
        override fun translate(
            httpCode: Int,
            errorMessage: String?,
            stripeError: StripeError?
        ): String {
            return errorMessage.orEmpty()
        }
    }
}
