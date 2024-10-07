package com.stripe.android.common.validation

internal object CustomerSessionClientSecretValidator {
    private const val EPHEMERAL_KEY_SECRET_PREFIX = "ek_"
    private const val CUSTOMER_SESSION_CLIENT_SECRET_KEY_PREFIX = "cuss_"

    sealed interface Result {
        data object Valid : Result

        sealed interface Error : Result {
            data object Empty : Result

            data object LegacyEphemeralKey : Result

            data object UnknownKey : Result
        }
    }

    fun validate(customerSessionClientSecret: String): Result {
        return when {
            customerSessionClientSecret.isBlank() ->
                Result.Error.Empty
            customerSessionClientSecret.startsWith(EPHEMERAL_KEY_SECRET_PREFIX) ->
                Result.Error.LegacyEphemeralKey
            !customerSessionClientSecret.startsWith(CUSTOMER_SESSION_CLIENT_SECRET_KEY_PREFIX) ->
                Result.Error.UnknownKey
            else -> Result.Valid
        }
    }
}
