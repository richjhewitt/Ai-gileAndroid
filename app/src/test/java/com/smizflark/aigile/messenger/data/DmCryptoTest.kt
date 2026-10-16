package com.smizflark.aigile.messenger.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.fail
import org.junit.Test

class DmCryptoTest {
    private val a = DmCrypto.newKeyPair()
    private val b = DmCrypto.newKeyPair()
    private val aPub = DmCrypto.publicB64(a)
    private val bPub = DmCrypto.publicB64(b)

    @Test fun bothSidesDeriveTheSameKey() =
        assertArrayEquals(DmCrypto.sharedKey(a, bPub), DmCrypto.sharedKey(b, aPub))

    @Test fun sealedMessageOpensOnTheOtherSideAndHidesTheText() {
        val sealed = DmCrypto.seal(DmCrypto.sharedKey(a, bPub), "chat1", DmCrypto.fingerprint(aPub), "hello 👋")
        assertFalse(sealed.contains("hello"))
        assertEquals("hello 👋", DmCrypto.open(DmCrypto.sharedKey(b, aPub), "chat1", DmCrypto.fingerprint(aPub), sealed))
    }

    @Test fun movedOrWrongKeyFailsToOpen() {
        val key = DmCrypto.sharedKey(a, bPub)
        val sealed = DmCrypto.seal(key, "chat1", "fp", "secret")
        for (bad in listOf({ DmCrypto.open(key, "chat2", "fp", sealed) }, { DmCrypto.open(key, "chat1", "other", sealed) },
            { DmCrypto.open(DmCrypto.sharedKey(a, DmCrypto.publicB64(DmCrypto.newKeyPair())), "chat1", "fp", sealed) })) {
            try { bad(); fail("opened") } catch (_: Exception) { }
        }
    }

    @Test fun safetyNumberMatchesOnBothPhonesAndChangesWithAKey() {
        val n = DmCrypto.safetyNumber(aPub, bPub)
        assertEquals(n, DmCrypto.safetyNumber(bPub, aPub))
        assertEquals(6, n.split(' ').size)
        assertNotEquals(n, DmCrypto.safetyNumber(aPub, DmCrypto.publicB64(DmCrypto.newKeyPair())))
    }
}
