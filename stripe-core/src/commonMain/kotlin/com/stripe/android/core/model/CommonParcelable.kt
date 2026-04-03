package com.stripe.android.core.model

/**
 * Common-source replacement for `android.os.Parcelable`.
 *
 * Shared models implement `CommonParcelable` so they can stay Android-free in
 * `commonMain`. Android provides the real `Parcelable` actual; non-Android
 * targets can provide an empty actual when they are added.
 */
expect interface CommonParcelable
