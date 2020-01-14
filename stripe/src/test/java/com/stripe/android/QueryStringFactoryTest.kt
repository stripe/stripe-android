package com.stripe.android

import java.net.URLDecoder
import kotlin.test.Test
import kotlin.test.assertEquals

class QueryStringFactoryTest {

    private val factory = QueryStringFactory()

    @Test
    fun create_withSimpleParams() {
        val queryString = factory.create(
            mapOf(
                "color" to "blue"
            )
        )
        assertEquals(
            "color=blue",
            queryString
        )
    }

    @Test
    fun create_withComplexParams() {
        val queryString = factory.create(
            mapOf(
                "colors" to listOf("blue", "green"),
                "empty_list" to emptyList<String>(),
                "empty_map" to mapOf<String, String>(),
                "person" to mapOf(
                    "age" to 45,
                    "city" to "San Francisco",
                    "wishes" to emptyList<String>(),
                    "friends" to listOf("Alice", "Bob")
                )
            )
        )
        assertEquals(
            "colors[]=blue&colors[]=green&empty_list=&person[age]=45&person[city]=San Francisco&person[wishes]=&person[friends][]=Alice&person[friends][]=Bob",
            URLDecoder.decode(queryString, Charsets.UTF_8.name())
        )
    }
}
