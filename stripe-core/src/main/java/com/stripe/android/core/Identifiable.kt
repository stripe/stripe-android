package com.stripe.android.core

import androidx.annotation.RestrictTo
import java.io.Serializable
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Identifiable : Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Identifiable(): Identifiable {
    return Identifiable(UUID.randomUUID())
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Identifiable(id: UUID): Identifiable {
    return UuidIdentifiable(id)
}

@JvmInline
private value class UuidIdentifiable(private val id: UUID) : Identifiable
