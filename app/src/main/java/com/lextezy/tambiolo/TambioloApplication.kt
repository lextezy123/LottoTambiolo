package com.lextezy.tambiolo

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.lextezy.tambiolo.data.AppPrefs
import com.lextezy.tambiolo.data.BillingManager
import com.lextezy.tambiolo.data.ResultsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
        MobileAds.initialize(this) {}
    }
}
