package com.stripe.android.utils.screenshots

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object PermutationsFactory {

    fun <T : Any> create(kClass: KClass<T>): List<T> {
        val className = kClass.simpleName
        val constructor = kClass.primaryConstructor ?: error(
            message = "No constructor found for $className"
        )

        val paramClasses = constructor.parameters.map { param ->
            val name = param.type.toString()

            try {
                Class.forName(name)
            } catch (e: ClassNotFoundException) {
                error("$className can only have enums as constructor arguments, not $name")
            }
        }

        require(paramClasses.all { it.isEnum }) {
            val offendingClass = paramClasses
                .filter { !it.isEnum }
                .joinToString(separator = ", ") { it.name }
            "$className can only have enums as constructor arguments, not $offendingClass"
        }

        // We get a list of lists, where each list is the enum's cases
        val enumParams = paramClasses.map { paramClass ->
            paramClass.enumConstants.toList()
        }

        val permutations = createPermutations(enumParams)

        val testConfigs = permutations.map { permutation ->
            constructor.call(*permutation.toTypedArray())
        }

        return testConfigs
    }
}

private fun createPermutations(elements: List<List<*>>): List<List<*>> {
    return (elements.toSet()).fold(listOf(listOf<Any?>())) { acc, set ->
        acc.flatMap { list -> set.map { element -> list + element } }
    }
}
