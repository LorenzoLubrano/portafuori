package io.github.lorenzolubrano.portafuori

import android.content.Intent
import io.github.lorenzolubrano.portafuori.data.StyleId

// The same activity for the other styles, each with its style's launch theme in the manifest. When the system
// reopens a task whose process was killed it draws the starting window from the top activity's manifest theme,
// a case setSplashScreenTheme does not cover.
class MainActivityOriginale : MainActivity()
class MainActivityAndroid : MainActivity()

object StyleActivities {
    /** The activity that shows the app in [id]. */
    fun of(id: StyleId): Class<out MainActivity> = when (id) {
        StyleId.LINEE -> MainActivity::class.java
        StyleId.ORIGINALE -> MainActivityOriginale::class.java
        StyleId.ANDROID -> MainActivityAndroid::class.java
    }

    /** A restored activity hands over nothing but itself: its launch intent (a shared calendar) was handled already. */
    fun carriesLaunchIntent(restored: Boolean): Boolean = !restored

    /**
     * Whether the activity [current] reopens the app in [style]. Not while it is already finishing: two quick changes
     * would stack two activities, and the one opening next reads the latest style and moves on by itself.
     */
    fun reopens(current: Class<*>, style: StyleId, finishing: Boolean): Boolean = current != of(style) && !finishing

    /**
     * Whether Back, with nothing left to go back to in the app, sends the app to the background instead of closing it.
     * Android does it by itself only for the launcher's activity (Linee's); the other styles' activities would be
     * closed and reopen from scratch. Not when the app was opened on top of another one: Back returns there.
     */
    fun backKeepsInBackground(current: Class<*>, taskRoot: Boolean): Boolean = current != MainActivity::class.java && taskRoot

    /**
     * Flags for handing a launch over to the style's activity. A file or text opened inside another app's task (a
     * chat) goes on top of that app, which stays: clearing down to an older activity of ours would close it. Anywhere
     * else, and for the app's own ways in (launcher, widget, notification), it comes back to the activity already
     * there instead of stacking a copy. With every activity relinquishing the task's identity, [taskRoot] is false
     * exactly when another app sits below. A shared file stays readable either way.
     */
    fun forwardFlags(action: String?, launchFlags: Int, taskRoot: Boolean): Int {
        val read = launchFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION
        val insideAnotherApp = (action == Intent.ACTION_SEND || action == Intent.ACTION_VIEW) && !taskRoot
        return read or Intent.FLAG_ACTIVITY_SINGLE_TOP or if (insideAnotherApp) 0 else Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}
