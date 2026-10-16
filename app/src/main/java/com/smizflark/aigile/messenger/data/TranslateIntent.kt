package com.smizflark.aigile.messenger.data

import android.content.Intent

/** AII-395: Translate. Which app route a message goes out by, given what the phone can open. */
object TranslateIntent {
    const val GOOGLE_TRANSLATE = "com.google.android.apps.translate"
    const val ACTION_TRANSLATE = "android.intent.action.TRANSLATE"   // Intent.ACTION_TRANSLATE, API 29

    enum class Route { TRANSLATE, PROCESS_TEXT, NONE }

    /** ACTION_TRANSLATE first (API 29+), then PROCESS_TEXT with Google Translate, else nothing (item hidden). */
    fun route(sdk: Int, translateResolves: Boolean, processTextResolves: Boolean): Route = when {
        sdk >= 29 && translateResolves -> Route.TRANSLATE
        processTextResolves -> Route.PROCESS_TEXT
        else -> Route.NONE
    }

    fun translate(text: String): Intent = Intent(ACTION_TRANSLATE).putExtra(Intent.EXTRA_TEXT, text)

    fun processText(text: String): Intent = Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain").setPackage(GOOGLE_TRANSLATE)
        .putExtra(Intent.EXTRA_PROCESS_TEXT, text).putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
}
