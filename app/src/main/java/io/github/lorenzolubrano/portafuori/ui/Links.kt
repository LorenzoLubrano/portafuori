package io.github.lorenzolubrano.portafuori.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Hands the URL to the browser. The app itself never goes online. False when nothing can open it. */
fun openLink(context: Context, url: String): Boolean =
    startIfPossible(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))

/** Opens the dialer with the number filled in, never calls. False when there is no phone app (tablets). */
fun dial(context: Context, number: String): Boolean =
    startIfPossible(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))

private fun startIfPossible(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}
