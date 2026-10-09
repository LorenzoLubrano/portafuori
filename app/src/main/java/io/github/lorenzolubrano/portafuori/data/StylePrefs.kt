package io.github.lorenzolubrano.portafuori.data

import android.content.Context
import androidx.core.content.edit

/** Synchronous copy of the style for the first frame, the widget and the notification: DataStore is asynchronous. */
object StylePrefs {
    // the same file MainActivity uses for its copy of the theme
    private const val FILE = "ui"
    private const val KEY = "style"

    fun cached(context: Context): StyleId =
        StyleId.parse(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null))

    fun remember(context: Context, id: StyleId) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getString(KEY, null) != id.name) prefs.edit { putString(KEY, id.name) }
    }
}
