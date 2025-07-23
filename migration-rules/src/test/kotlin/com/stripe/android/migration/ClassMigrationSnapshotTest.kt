package com.stripe.android.migration

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.github.detekt.test.utils.compileContentForTest
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class ClassMigrationSnapshotTest {
    
    @Test
    fun `snapshot test for ClassMigration rule`() {
        // Load input file
        val inputFile = File("src/test/resources/ClassMigrationInput.kt")
        val inputCode = inputFile.readText()
        
        // Load expected output file
        val expectedFile = File("src/test/resources/ClassMigrationExpected.kt")
        val expectedCode = expectedFile.readText()
        
        val rule = ClassMigration(TestConfig("autoCorrect" to true))
        val ktFile = compileContentForTest(inputCode)
        rule.visitFile(ktFile)
        
        val correctedCode = ktFile.text

        // Compare the autocorrected code with expected output
        assertEquals(expectedCode.trim(), correctedCode.trim())
    }
}