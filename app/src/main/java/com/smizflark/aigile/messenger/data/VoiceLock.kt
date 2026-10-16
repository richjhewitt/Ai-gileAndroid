package com.smizflark.aigile.messenger.data

/** Slide up to lock a voice note (AII-384): the plain rules, apart from the gesture and the recorder. */
object VoiceLock {
    enum class State { HOLD, LOCKED, SENT, DELETED }

    /** How far up (dp) the finger must slide to lock. */
    const val LOCK_DP = 80f

    /** [upDp] is how far above where the press began the finger is (positive = up). */
    fun reached(upDp: Float) = upDp >= LOCK_DP

    /** Finger moved: HOLD locks once it is far enough up; anything else stays as it is. */
    fun onMove(s: State, upDp: Float) = if (s == State.HOLD && reached(upDp)) State.LOCKED else s

    /** Finger came up: a held note is sent (or dropped when cancelling); a locked one keeps going. */
    fun onRelease(s: State, cancelling: Boolean) = if (s != State.HOLD) s else if (cancelling) State.DELETED else State.SENT

    /** The Send button (only meaningful when locked). */
    fun onSend(s: State) = if (s == State.LOCKED) State.SENT else s

    /** The Delete button (only meaningful when locked). */
    fun onDelete(s: State) = if (s == State.LOCKED) State.DELETED else s
}
