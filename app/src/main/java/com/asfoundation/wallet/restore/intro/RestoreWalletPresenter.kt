package com.asfoundation.wallet.restore.intro

import android.os.Bundle
import com.appcoins.wallet.commons.Logger
import com.asfoundation.wallet.base.RxSchedulers
import com.asfoundation.wallet.billing.analytics.WalletsAnalytics
import com.asfoundation.wallet.billing.analytics.WalletsEventSender
import com.asfoundation.wallet.onboarding.use_cases.SetOnboardingCompletedUseCase
import com.asfoundation.wallet.util.RestoreError
import com.asfoundation.wallet.util.RestoreErrorType
import com.asfoundation.wallet.wallets.WalletModel
import com.asfoundation.wallet.wallets.usecases.UpdateWalletInfoUseCase
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

class RestoreWalletPresenter(private val view: RestoreWalletView,
                             private val disposable: CompositeDisposable,
                             private val navigator: RestoreWalletNavigator,
                             private val restoreWalletInteractor: RestoreWalletInteractor,
                             private val updateWalletInfoUseCase: UpdateWalletInfoUseCase,
                             private val walletsEventSender: WalletsEventSender,
                             private val logger: Logger,
                             private val setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase,
                             private val rxSchedulers: RxSchedulers) {

  companion object {
    private const val KEYSTORE = "keystore"
  }

  fun present(savedInstanceState: Bundle?) {
    savedInstanceState?.let { view.setKeystore(it.getString(KEYSTORE, "")) }
    handleRestoreFromString()
    handleRestoreFromFile()
    handleFileChosen()
    handleOnPermissionsGiven()
  }

  private fun handleOnPermissionsGiven() {
    disposable.add(view.onPermissionsGiven()
        .flatMapCompletable { navigator.launchFileIntent(restoreWalletInteractor.getPath()) }
        .doOnError { view.showSnackBarError() }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun handleFileChosen() {
    disposable.add(view.onFileChosen()
        .doOnNext { view.showWalletRestoreAnimation() }
        .flatMapSingle { restoreWalletInteractor.readFile(it) }
        .observeOn(rxSchedulers.computation)
        .flatMapSingle { fetchWalletModel(it) }
        .flatMapSingle { setDefaultWallet(it) }
        .observeOn(rxSchedulers.main)
        .doOnNext { handleWalletModel(it) }
        .subscribe({}, {
          logger.log("RestoreWalletPresenter", it)
          view.hideAnimation()
          view.showError(RestoreErrorType.INVALID_KEYSTORE)
        })
    )
  }

  private fun handleRestoreFromFile() {
    disposable.add(view.restoreFromFileClick()
        .doOnNext { view.askForReadPermissions() }
        .doOnNext {
          walletsEventSender.sendWalletRestoreEvent(WalletsAnalytics.ACTION_IMPORT_FROM_FILE,
              WalletsAnalytics.STATUS_SUCCESS)
        }
        .doOnError { t ->
          walletsEventSender.sendWalletRestoreEvent(WalletsAnalytics.ACTION_IMPORT_FROM_FILE,
              WalletsAnalytics.STATUS_FAIL, t.message)
        }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun handleRestoreFromString() {
    disposable.add(view.restoreFromStringClick()
        .doOnNext {
          view.hideKeyboard()
          view.showWalletRestoreAnimation()
        }
        .observeOn(rxSchedulers.computation)
        .flatMapSingle { fetchWalletModel(it) }
        .observeOn(rxSchedulers.main)
        .doOnNext { handleWalletModel(it) }
        .doOnError { t ->
          walletsEventSender.sendWalletRestoreEvent(WalletsAnalytics.ACTION_IMPORT,
              WalletsAnalytics.STATUS_FAIL, t.message)
        }
        .subscribe())
  }

  private fun setDefaultWallet(model: WalletModel): Single<WalletModel> {
    if (model.error.hasError) return Single.just(model)
    // Retrieves overall balance to update it right as we import it
    return Completable.mergeArray(restoreWalletInteractor.setDefaultWallet(model.address),
        updateWalletInfoUseCase(model.address, updateFiat = true))
        .andThen(Completable.fromAction { setOnboardingCompletedUseCase() })
        .andThen(Single.just(model))
  }

  private fun handleWalletModel(walletModel: WalletModel) {
    if (walletModel.error.hasError) {
      view.hideAnimation()
      if (walletModel.error.type == RestoreErrorType.INVALID_PASS) {
        navigator.navigateToPasswordView(walletModel.keystore)
        walletsEventSender.sendWalletRestoreEvent(WalletsAnalytics.ACTION_IMPORT,
            WalletsAnalytics.STATUS_SUCCESS)
      } else {
        view.showError(walletModel.error.type)
        walletsEventSender.sendWalletRestoreEvent(WalletsAnalytics.ACTION_IMPORT,
            WalletsAnalytics.STATUS_FAIL, walletModel.error.type.toString())
      }
    } else {
      view.showWalletRestoredAnimation()
      walletsEventSender.sendWalletRestoreEvent(WalletsAnalytics.ACTION_IMPORT,
          WalletsAnalytics.STATUS_SUCCESS)
    }
  }

  private fun fetchWalletModel(key: String): Single<WalletModel> {
    return if (restoreWalletInteractor.isKeystore(key)) restoreWalletInteractor.restoreKeystore(key)
    else {
      if (key.length == 64) restoreWalletInteractor.restorePrivateKey(key)
      else Single.just(WalletModel(RestoreError(RestoreErrorType.INVALID_PRIVATE_KEY)))
    }
  }

  fun onSaveInstanceState(outState: Bundle, keystore: String) {
    outState.putString(KEYSTORE, keystore)
  }

  fun stop() = disposable.clear()
}
