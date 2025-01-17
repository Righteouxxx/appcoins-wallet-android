package cm.aptoide.skills.repository

import android.content.SharedPreferences
import cm.aptoide.skills.model.CachedPayment
import cm.aptoide.skills.model.WalletAddress
import com.google.gson.Gson
import io.reactivex.Single

class SharedPreferencesPaymentLocalStorage(
    private val preferences: SharedPreferences,
    private val mapper: Gson
) : PaymentLocalStorage {
  companion object {
    private const val PREFIX = "PAYMENT_LOCAL_STORAGE_PREFIX_"
  }

  override fun save(cachedPayment: CachedPayment) {
    val editPreferences = preferences.edit()
    editPreferences.putString(getKey(cachedPayment.ticket.walletAddress),
        mapper.toJson(cachedPayment))
    editPreferences.apply()
  }

  override fun get(walletAddress: WalletAddress): Single<CachedPayment> {
    return Single.fromCallable {
      return@fromCallable preferences.getString(getKey(walletAddress), null)
          ?.let { mapper.fromJson(it, CachedPayment::class.java) } ?: throw RuntimeException(
          "Couldn't find any cached payment.")
    }
  }

  private fun getKey(walletAddress: WalletAddress): String {
    return PREFIX + walletAddress.address
  }
}
