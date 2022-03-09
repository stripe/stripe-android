package com.stripe.android.stripecardscan.framework.api

internal sealed class NetworkResult<Success, Error>(open val responseCode: Int) {

    data class Success<Success>(override val responseCode: Int, val body: Success) :
        NetworkResult<Success, Nothing>(responseCode)

    data class Error<Error>(override val responseCode: Int, val error: Error) :
        NetworkResult<Nothing, Error>(responseCode)

    data class Exception(override val responseCode: Int, val exception: Throwable) :
        NetworkResult<Nothing, Nothing>(responseCode)
}
