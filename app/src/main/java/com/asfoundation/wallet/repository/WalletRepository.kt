package com.asfoundation.wallet.repository

import android.content.SharedPreferences
import com.asfoundation.wallet.analytics.AnalyticsSetup
import com.asfoundation.wallet.entity.Wallet
import com.asfoundation.wallet.service.AccountKeystoreService
import io.reactivex.*

class WalletRepository(private val preferencesRepositoryType: PreferencesRepositoryType,
                       private val accountKeystoreService: AccountKeystoreService,
                       private val analyticsSetUp: AnalyticsSetup) : WalletRepositoryType {

  override fun fetchWallets(): Single<Array<Wallet>> {
    return accountKeystoreService.fetchAccounts()
  }

  override fun findWallet(address: String): Single<Wallet> {
    return fetchWallets().flatMap { accounts ->
      for (wallet in accounts) {
        if (wallet.sameAddress(address)) {
          return@flatMap Single.just(wallet)
        }
      }
      null
    }
  }

  override fun createWallet(password: String): Single<Wallet> {
    return accountKeystoreService.createAccount(password)
  }

  override fun restoreKeystoreToWallet(store: String, password: String,
                                       newPassword: String): Single<Wallet> {
    return accountKeystoreService.restoreKeystore(store, password, newPassword)
  }

  override fun restorePrivateKeyToWallet(privateKey: String?, newPassword: String): Single<Wallet> {
    return accountKeystoreService.restorePrivateKey(privateKey, newPassword)
  }

  override fun exportWallet(address: String, password: String,
                            newPassword: String?): Single<String> {
    return accountKeystoreService.exportAccount(address, password, newPassword)
  }

  override fun deleteWallet(address: String, password: String): Completable {
    return accountKeystoreService.deleteAccount(address, password)
  }

  override fun setDefaultWallet(address: String): Completable {
    return Completable.fromAction {
      analyticsSetUp.setUserId(address)
      preferencesRepositoryType.setCurrentWalletAddress(address)
    }
  }

  override fun getDefaultWallet(): Single<Wallet> {
    return Single.fromCallable { getDefaultWalletAddress() }
        .flatMap { address -> findWallet(address) }
  }

  private fun getDefaultWalletAddress(): String {
    val currentWalletAddress = preferencesRepositoryType.getCurrentWalletAddress()
    return currentWalletAddress ?: throw WalletNotFoundException()
  }

  private fun emitWalletAddress(emitter: ObservableEmitter<String>) {
    try {
      val walletAddress = getDefaultWalletAddress()
      emitter.onNext(walletAddress)
    } catch (e: WalletNotFoundException) {
      emitter.tryOnError(e)
    }
  }

  override fun observeDefaultWallet(): Observable<Wallet> {
    return Observable.create(
        ObservableOnSubscribe { emitter: ObservableEmitter<String> ->
          val listener =
              SharedPreferences.OnSharedPreferenceChangeListener { _, key: String ->
                if (key == SharedPreferencesRepository.CURRENT_ACCOUNT_ADDRESS_KEY) {
                  emitWalletAddress(emitter)
                }
              }
          emitter.setCancellable { preferencesRepositoryType.removeChangeListener(listener) }
          emitWalletAddress(emitter)
          preferencesRepositoryType.addChangeListener(listener)
        } as ObservableOnSubscribe<String>)
        .flatMapSingle { address -> findWallet(address) }
  }
}