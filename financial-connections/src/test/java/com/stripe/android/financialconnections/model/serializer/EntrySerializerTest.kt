@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.model.serializer

import Alignment
import FinancialConnectionsGenericInfoScreen.Body
import FinancialConnectionsGenericInfoScreen.Body.Entry
import Size
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.Json
import kotlin.test.Test

class EntrySerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `test parsing BodyText`() {
        val jsonString = """
            {
                "type": "text",
                "id": "text1",
                "text": "Hello, world!",
                "alignment": "center",
                "size": "medium"
            }
        """.trimIndent()

        val entry = json.decodeFromString<Entry>(jsonString)

        assertTrue(entry is Entry.Text)
        with(entry as Entry.Text) {
            assertEquals("text1", id)
            assertEquals("Hello, world!", text)
            assertEquals(Alignment.Center, alignment)
            assertEquals(Size.Medium, size)
        }
    }

    @Test
    fun `test parsing BodyImage`() {
        val jsonString = """
            {
                "type": "image",
                "id": "image1",
                "image": {
                    "default": "https://example.com/image.jpg"
                },
                "alt": "Example image"
            }
        """.trimIndent()

        val entry = json.decodeFromString<Entry>(jsonString)

        assertTrue(entry is Entry.Image)
        with(entry as Entry.Image) {
            assertEquals("image1", id)
            assertEquals("https://example.com/image.jpg", image.default)
            assertEquals("Example image", alt)
        }
    }

    @Test
    fun `test parsing BodyBullets`() {
        val jsonString = """
            {
                "type": "bullets",
                "id": "bullets1",
                "bullets": [
                    {
                        "id": "bullet1",
                        "title": "First bullet",
                        "content": "This is the first bullet point"
                    },
                    {
                        "id": "bullet2",
                        "title": "Second bullet",
                        "content": "This is the second bullet point",
                        "icon": {
                            "default": "https://example.com/icon.png"
                        }
                    }
                ]
            }
        """.trimIndent()

        val entry = json.decodeFromString<Entry>(jsonString)

        assertTrue(entry is Entry.Bullets)
        with(entry as Entry.Bullets) {
            assertEquals("bullets1", id)
            assertEquals(2, bullets.size)

            with(bullets[0]) {
                assertEquals("bullet1", id)
                assertEquals("First bullet", title)
                assertEquals("This is the first bullet point", content)
                assertEquals(null, icon)
            }

            with(bullets[1]) {
                assertEquals("bullet2", id)
                assertEquals("Second bullet", title)
                assertEquals("This is the second bullet point", content)
                assertEquals("https://example.com/icon.png", icon?.default)
            }
        }
    }

    @Test
    fun `test parsing Body with multiple entry types`() {
        val jsonString = """
        {
          "entries": [
            {
              "type": "text",
              "id": "text1",
              "text": "Welcome to our service!",
              "alignment": "center",
              "size": "medium"
            },
            {
              "type": "image",
              "id": "image1",
              "image": {
                "default": "https://example.com/welcome.jpg"
              },
              "alt": "Welcome image"
            },
            {
              "type": "bullets",
              "id": "bullets1",
              "bullets": [
                {
                  "id": "bullet1",
                  "title": "Easy to use",
                  "content": "Our service is designed for simplicity"
                },
                {
                  "id": "bullet2",
                  "title": "Secure",
                  "content": "Your data is always protected",
                  "icon": {
                    "default": "https://example.com/lock-icon.png"
                  }
                }
              ]
            },
            {
              "type": "unknown_entry",
              "id": "???",
              "unkown_key": "Get started now!"
            }
          ]
        }
        """.trimIndent()

        val body = json.decodeFromString<Body>(jsonString)

        assertEquals(4, body.entries.size)

        // Check (Text)
        assertTrue(body.entries[0] is Entry.Text)
        with(body.entries[0] as Entry.Text) {
            assertEquals("text1", id)
            assertEquals("Welcome to our service!", text)
            assertEquals(Alignment.Center, alignment)
            assertEquals(Size.Medium, size)
        }

        // Check (Image)
        assertTrue(body.entries[1] is Entry.Image)
        with(body.entries[1] as Entry.Image) {
            assertEquals("image1", id)
            assertEquals("https://example.com/welcome.jpg", image.default)
            assertEquals("Welcome image", alt)
        }

        // Check (Bullets)
        assertTrue(body.entries[2] is Entry.Bullets)
        with(body.entries[2] as Entry.Bullets) {
            assertEquals("bullets1", id)
            assertEquals(2, bullets.size)
            with(bullets[0]) {
                assertEquals("bullet1", id)
                assertEquals("Easy to use", title)
                assertEquals("Our service is designed for simplicity", content)
                assertNull(icon)
            }
            with(bullets[1]) {
                assertEquals("bullet2", id)
                assertEquals("Secure", title)
                assertEquals("Your data is always protected", content)
                assertEquals("https://example.com/lock-icon.png", icon?.default)
            }
        }

        // Check (Unknown)
        assertTrue(body.entries[3] is Entry.Unknown)
    }
}
