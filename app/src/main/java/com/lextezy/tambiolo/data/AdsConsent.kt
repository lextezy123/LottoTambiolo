package com.lextezy.tambiolo.data

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ad consent (Google's User Messaging Platform).
 * Users in Europe and the UK must be asked before ads load; everyone else sees nothing.
 * Ads start only after this has run. The consent message itself is created in the
 * AdMob console under Privacy & messaging.
 */
object AdsConsent {
    private val started = AtomicBoolean(false)
    private val _adsReady = MutableStateFlow(false)

    /** True once the ads SDK may load ads. The banner waits for this. */
    val adsReady: StateFlow<Boolean> = _adsReady

    fun gather(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Shows the consent form only where the law requires one.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> startAds(activity) }
            },
            { _ ->
                // Couldn't reach the consent service (offline, or no message set up in AdMob yet).
                // Google serves only limited ads to regulated users without a consent record.
                startAds(activity)
            },
        )
        // Consent from an earlier session is still valid, so don't wait for the network.
        if (info.canRequestAds()) startAds(activity)
    }

    private fun startAds(context: Context) {
        if (!started.compareAndSet(false, true)) return
        MobileAds.initialize(context.applicationContext) {}
        _adsReady.value = true
    }

    /** True for users who must be able to change their ad privacy choice later (Europe, UK). */
    fun privacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> }
    }
}
