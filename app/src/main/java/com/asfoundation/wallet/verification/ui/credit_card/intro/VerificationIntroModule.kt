package com.asfoundation.wallet.verification.ui.credit_card.intro

import com.adyen.checkout.redirect.RedirectComponent
import com.appcoins.wallet.bdsbilling.WalletService
import com.asfoundation.wallet.billing.adyen.AdyenErrorCodeMapper
import com.asfoundation.wallet.billing.adyen.AdyenPaymentInteractor
import com.appcoins.wallet.commons.Logger
import com.asfoundation.wallet.support.SupportInteractor
import com.asfoundation.wallet.verification.repository.VerificationRepository
import com.asfoundation.wallet.verification.ui.credit_card.VerificationAnalytics
import com.asfoundation.wallet.verification.ui.credit_card.WalletVerificationInteractor
import dagger.Module
import dagger.Provides
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

@Module
class VerificationIntroModule {

  @Provides
  fun providesWalletVerificationIntroNavigator(
      fragment: VerificationIntroFragment): VerificationIntroNavigator {
    return VerificationIntroNavigator(fragment.requireFragmentManager())
  }

  @Provides
  fun providesWalletVerificationIntroPresenter(fragment: VerificationIntroFragment,
                                               navigator: VerificationIntroNavigator,
                                               logger: Logger,
                                               interactor: VerificationIntroInteractor,
                                               data: VerificationIntroData,
                                               analytics: VerificationAnalytics): VerificationIntroPresenter {
    return VerificationIntroPresenter(fragment as VerificationIntroView,
        CompositeDisposable(), navigator, logger, AndroidSchedulers.mainThread(),
        Schedulers.io(), interactor, AdyenErrorCodeMapper(), data, analytics)
  }

  @Provides
  fun provideWalletVerificationIntroInteractor(verificationRepository: VerificationRepository,
                                               adyenPaymentInteractor: AdyenPaymentInteractor,
                                               walletService: WalletService,
                                               supportInteractor: SupportInteractor,
                                               walletVerificationInteractor: WalletVerificationInteractor): VerificationIntroInteractor {
    return VerificationIntroInteractor(verificationRepository, adyenPaymentInteractor,
        walletService, supportInteractor, walletVerificationInteractor)
  }

  @Provides
  fun providesVerificationIntroData(
      fragment: VerificationIntroFragment): VerificationIntroData {
    return VerificationIntroData(RedirectComponent.getReturnUrl(fragment.context!!))
  }

}