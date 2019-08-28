package com.asfoundation.wallet.ui.gamification

import com.appcoins.wallet.gamification.Gamification
import com.appcoins.wallet.gamification.GamificationScreen
import com.appcoins.wallet.gamification.repository.ForecastBonus
import com.appcoins.wallet.gamification.repository.Levels
import com.appcoins.wallet.gamification.repository.UserStats
import com.asfoundation.wallet.entity.Wallet
import com.asfoundation.wallet.interact.FindDefaultWalletInteract
import com.asfoundation.wallet.service.LocalCurrencyConversionService
import com.asfoundation.wallet.ui.iab.FiatValue
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.math.BigDecimal

class GamificationInteractor(
    private val gamification: Gamification,
    private val defaultWallet: FindDefaultWalletInteract,
    private val conversionService: LocalCurrencyConversionService) {

  fun getLevels(): Single<Levels> {
    return gamification.getLevels()
  }

  fun getUserStatus(): Single<UserStats> {
    return defaultWallet.find()
        .flatMap { gamification.getUserStatus(it.address) }
  }

  fun getEarningBonus(packageName: String, amount: BigDecimal): Single<ForecastBonus> {
    return defaultWallet.find()
        .flatMap { wallet: Wallet ->
          gamification.getEarningBonus(wallet.address, packageName, amount)
              .flatMap { forecastBonus: ForecastBonus ->
                conversionService.localCurrency
                    .flatMap { fiatValue: FiatValue ->
                      gamification.getNextPurchaseBonusFromReferrals(wallet.address)
                          .map {
                            val status = getBonusStatus(forecastBonus, it)
                            val bonus = forecastBonus.amount.multiply(fiatValue.amount)
                                .add(it.amount)
                            ForecastBonus(status, bonus, fiatValue.symbol)
                          }
                    }
              }
        }
  }

  private fun getBonusStatus(forecastBonus: ForecastBonus,
                             referralBonus: ForecastBonus): ForecastBonus.Status {
    return if (forecastBonus.status == ForecastBonus.Status.ACTIVE || referralBonus.status == ForecastBonus.Status.ACTIVE) {
      ForecastBonus.Status.ACTIVE
    } else {
      ForecastBonus.Status.INACTIVE
    }
  }

  fun hasNewLevel(screen: GamificationScreen): Single<Boolean> {
    return defaultWallet.find()
        .flatMap { gamification.hasNewLevel(it.address, screen.toString()) }
  }

  fun levelShown(level: Int, screen: GamificationScreen): Completable {
    return defaultWallet.find()
        .flatMapCompletable { gamification.levelShown(it.address, level, screen.toString()) }
  }

  fun getLastShownLevel(screen: GamificationScreen): Single<Int> {
    return defaultWallet.find()
        .flatMap { gamification.getLastShownLevel(it.address, screen.toString()) }
  }

  fun getAppcToLocalFiat(value: String, scale: Int): Observable<FiatValue> {
    return conversionService.getAppcToLocalFiat(value, scale)
        .onErrorReturn { FiatValue(BigDecimal("-1"), "", "") }
  }
}
