package demo

import com.natpryce.hamkrest.and
import org.junit.Test
import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.endsWith
import com.natpryce.hamkrest.startsWith

class HelloTest {
    @Test
    fun simpleTest() {
        assert.that("Hello Hamkrest", startsWith("Hello") and endsWith("Hamkrest"))
    }
}