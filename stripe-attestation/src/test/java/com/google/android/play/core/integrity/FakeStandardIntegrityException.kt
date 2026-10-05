package com.google.android.play.core.integrity

/**
 * The real exception has a package-private constructor and always supplies a message. Defining this fake
 * in the same package lets tests construct controlled inputs, including a null message, without reflection
 * or Mockito.
 */
internal class FakeStandardIntegrityException(
    errorCode: Int,
    override val message: String?,
) : StandardIntegrityException(
    errorCode,
    null,
)
