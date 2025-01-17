package com.asfoundation.wallet.wallet_blocked

import com.asfoundation.wallet.wallets.usecases.GetWalletInfoUseCase
import io.reactivex.Single
import java.util.concurrent.TimeUnit

class WalletBlockedInteract(
    private val getWalletInfoUseCase: GetWalletInfoUseCase
) {

  fun isWalletBlocked(): Single<Boolean> {
    return getWalletInfoUseCase(null, cached = false, updateFiat = false)
        .map { walletInfo -> walletInfo.blocked }
        .onErrorReturn { false }
        .delay(1, TimeUnit.SECONDS)
  }
}