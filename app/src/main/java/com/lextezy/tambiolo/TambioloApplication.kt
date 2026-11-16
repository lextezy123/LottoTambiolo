package com.lextezy.tambiolo

import android.app.Application
import com.lextezy.tambiolo.data.AppPrefs
import com.lextezy.tambiolo.data.BillingManager
import com.lextezy.tambiolo.data.ResultAlerts
import com.lextezy.tambiolo.data.ResultsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TambioloApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var prefs: AppPrefs
        private set
    lateinit var results: ResultsRepository
        private set
    lateinit var billing: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = AppPrefs(this)
        results = ResultsRepository(this)
        billing = BillingManager(this, prefs, appScope)
        billing.connect()
        // Ads start from MainActivity, after the ad consent check (see AdsConsent).
        // Keep the hourly results check registered while alerts are on.
        appScope.launch {
            if (prefs.alertsEnabled.first()) ResultAlerts.schedule(this@TambioloApplication)
        }
    }
}
