package com.stripe.android.core.model

/**
 * Common-source replacement for `java.io.Serializable`.
 *
 * This is only for legacy Android/JVM interoperability points that still rely
 * on Java serialization, such as Parcel write/readSerializable paths. It is not
 * related to `kotlinx.serialization.Serializable`.
 */
expect interface CommonJavaSerializable
