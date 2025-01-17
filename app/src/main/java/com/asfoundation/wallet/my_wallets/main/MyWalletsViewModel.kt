package com.asfoundation.wallet.my_wallets.main

import com.asfoundation.wallet.base.Async
import com.asfoundation.wallet.base.BaseViewModel
import com.asfoundation.wallet.base.SideEffect
import com.asfoundation.wallet.base.ViewState
import com.asfoundation.wallet.home.usecases.ObserveDefaultWalletUseCase
import com.asfoundation.wallet.ui.balance.BalanceInteractor
import com.asfoundation.wallet.ui.balance.BalanceVerificationModel
import com.asfoundation.wallet.ui.wallets.WalletsInteract
import com.asfoundation.wallet.ui.wallets.WalletsModel
import com.asfoundation.wallet.wallets.domain.WalletInfo
import com.asfoundation.wallet.wallets.usecases.ObserveWalletInfoUseCase
import io.reactivex.schedulers.Schedulers

object MyWalletsSideEffect : SideEffect

data class MyWalletsState(
    val walletsAsync: Async<WalletsModel> = Async.Uninitialized,
    val walletVerifiedAsync: Async<BalanceVerificationModel> = Async.Uninitialized,
    val walletInfoAsync: Async<WalletInfo> = Async.Uninitialized,
    val backedUpOnceAsync: Async<Boolean> = Async.Uninitialized,
) : ViewState

class MyWalletsViewModel(
    private val balanceInteractor: BalanceInteractor,
    private val walletsInteract: WalletsInteract,
    private val observeWalletInfoUseCase: ObserveWalletInfoUseCase,
    private val observeDefaultWalletUseCase: ObserveDefaultWalletUseCase
) : BaseViewModel<MyWalletsState, MyWalletsSideEffect>(initialState()) {

  companion object {
    fun initialState(): MyWalletsState {
      return MyWalletsState()
    }
  }

  init {
    observeCurrentWallet()
  }

  /**
   * Flushing Asyncs means we want to induce loading, this makes sense in the case of changing
   * active wallet, but not when we just enter the screen and we simply want to keep the contents
   * up-to-date.
   */
  fun refreshData(flushAsync: Boolean) {
    fetchWallets(flushAsync)
    fetchWalletVerified(flushAsync)
    fetchWalletInfo(flushAsync)
    observeHasBackedUpWallet(flushAsync)
  }

  private fun observeCurrentWallet() {
    observeDefaultWalletUseCase()
        .doOnNext { wallet ->
          val currentWalletAddress = state.walletInfoAsync()?.wallet
          if (currentWalletAddress == null || currentWalletAddress != wallet.address) {
            refreshData(flushAsync = true)
          }
        }
        .repeatableScopedSubscribe("ObserveCurrentWallet") { e -> e.printStackTrace() }
  }

  private fun fetchWallets(flushAsync: Boolean) {
    val retainValue = if (flushAsync) null else MyWalletsState::walletsAsync
    walletsInteract.observeWalletsModel()
        .subscribeOn(Schedulers.io())
        .asAsyncToState(retainValue) { wallet -> copy(walletsAsync = wallet) }
        .repeatableScopedSubscribe(MyWalletsState::walletsAsync.name) { e ->
          e.printStackTrace()
        }
  }

  private fun fetchWalletVerified(flushAsync: Boolean) {
    val retainValue = if (flushAsync) null else MyWalletsState::walletVerifiedAsync
    balanceInteractor.observeCurrentWalletVerified()
        .subscribeOn(Schedulers.io())
        .asAsyncToState(retainValue) { verification -> copy(walletVerifiedAsync = verification) }
        .repeatableScopedSubscribe(MyWalletsState::walletVerifiedAsync.name) { e ->
          e.printStackTrace()
        }
  }

  private fun fetchWalletInfo(flushAsync: Boolean) {
    val retainValue = if (flushAsync) null else MyWalletsState::walletInfoAsync
    observeWalletInfoUseCase(null, update = true, updateFiat = true)
        .asAsyncToState(retainValue) { balance -> copy(walletInfoAsync = balance) }
        .repeatableScopedSubscribe(MyWalletsState::walletInfoAsync.name) { e ->
          e.printStackTrace()
        }
  }

  private fun observeHasBackedUpWallet(flushAsync: Boolean) {
    val retainValue = if (flushAsync) null else MyWalletsState::backedUpOnceAsync
    balanceInteractor.observeBackedUpOnce()
        .asAsyncToState(retainValue) { backedUpOnce -> copy(backedUpOnceAsync = backedUpOnce) }
        .repeatableScopedSubscribe(MyWalletsState::backedUpOnceAsync.name) { e ->
          e.printStackTrace()
        }
  }
}