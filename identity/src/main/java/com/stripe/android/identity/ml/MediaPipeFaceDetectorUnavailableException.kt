package com.stripe.android.identity.ml

internal class MediaPipeFaceDetectorUnavailableException(
    cause: Throwable
) : IllegalStateException("MediaPipe face detector unavailable", cause)
