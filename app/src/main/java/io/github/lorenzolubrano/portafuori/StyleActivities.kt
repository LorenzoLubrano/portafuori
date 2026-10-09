package io.github.lorenzolubrano.portafuori

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
}
