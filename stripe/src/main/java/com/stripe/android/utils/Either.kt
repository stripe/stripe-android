package com.stripe.android.utils

sealed class Either<A, B> {
    data class Left<A, B>(val left: A): Either<A, B>()
    data class Right<A, B>(val right: B): Either<A, B>()
}