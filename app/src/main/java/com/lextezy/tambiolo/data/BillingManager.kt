package com.lextezy.tambiolo.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.lextezy.tambiolo.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Google Play one-time "remove ads" purchase. */
class BillingManager(context: Context, private val prefs: AppPrefs, private val scope: CoroutineScope) {

    private val _price = MutableStateFlow<String?>(null)
    /** Localized price like "₱59.00", or null until Play answers (or if the product isn't set up yet). */
    val price: StateFlow<String?> = _price

    private var product: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                loadProduct()
                restore()
            }
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(Config.REMOVE_ADS_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        ).build()
        client.queryProductDetailsAsync(params) { _, list ->
            product = list.firstOrNull()
            _price.value = product?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    /** Re-checks past purchases, e.g. after reinstalling. */
    fun restore() {
        if (!client.isReady) { connect(); return }
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(purchases)
        }
    }

    /** Returns false if the product isn't available (not set up in Play Console yet, or offline). */
    fun buy(activity: Activity): Boolean {
        val details = product ?: return false
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
        ).build()
        client.launchBillingFlow(activity, params)
        return true
    }

    private fun handle(purchases: List<Purchase>?) {
        val owned = purchases.orEmpty().filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && Config.REMOVE_ADS_PRODUCT_ID in it.products
        }
        if (owned.isEmpty()) return
        scope.launch { prefs.setAdsRemoved(true) }
        owned.filterNot { it.isAcknowledged }.forEach { p ->
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
            ) { }
        }
    }
}
