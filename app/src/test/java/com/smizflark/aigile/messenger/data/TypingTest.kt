package com.smizflark.aigile.messenger.data

import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TypingTest {
    private var calls = 0
    private val source = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ChatSource::class.java)) { _, m, _ ->
        if (m.name == "typing") calls++
        null
    } as ChatSource
    private val ref = ChatRef(Kind.FRIEND, "f1", "Sam", "", "")

    @Test fun onTellsTheBoard() = runBlocking { sendTyping(true, source, ref); assertEquals(1, calls) }
    @Test fun offNeverCallsTheBoard() = runBlocking { sendTyping(false, source, ref); assertEquals(0, calls) }
}
