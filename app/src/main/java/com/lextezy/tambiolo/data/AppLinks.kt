package com.lextezy.tambiolo.data

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

/** Rate, share and in-app review helpers. */
object AppLinks {
    /** Ask for a review after this many finished simulator draws (only once, ever). */
    const val REVIEW_AFTER_DRAWS = 5

    fun storeUrl(context: Context) = "https://play.google.com/store/apps/details?id=${context.packageName}"

    /** Opens the app's Play Store page so the user can rate it. */
    fun openStorePage(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
        try {
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl(context))))
        }
    }

    fun share(context: Context, message: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    /**
     * Google's in-app review card. Google decides whether it actually shows
     * (it limits how often), so this never blocks or errors for the user.
     */
    fun askForReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
        }
    }
}
