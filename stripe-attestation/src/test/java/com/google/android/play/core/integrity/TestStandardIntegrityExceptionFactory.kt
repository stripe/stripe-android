package com.google.android.play.core.integrity

internal fun createStandardIntegrityException(errorCode: Int): StandardIntegrityException {
    return StandardIntegrityException(errorCode, null)
}
