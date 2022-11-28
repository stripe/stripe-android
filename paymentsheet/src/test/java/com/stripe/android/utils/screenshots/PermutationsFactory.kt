package com.stripe.android.utils.screenshots

object PermutationsFactory {

    fun <T> create(
        vararg options: Array<out T>,
    ): List<List<T>> {
        @Suppress("UNCHECKED_CAST")
        return (options.toSet()).fold(listOf(listOf())) { acc, set ->
            acc.flatMap { list -> set.map { element -> list + element } }
        }
    }
}
