package com.asfoundation.wallet.ui.backup.entry

import com.asfoundation.wallet.ui.backup.entry.BackupWalletFragment.Companion.PARAM_WALLET_ADDR
import com.asfoundation.wallet.util.CurrencyFormatUtils
import com.asfoundation.wallet.wallets.usecases.GetWalletInfoUseCase
import dagger.Module
import dagger.Provides
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

@Module
class BackupWalletModule {

  @Provides
  fun providesBackupWalletPresenter(fragment: BackupWalletFragment,
                                    getWalletInfoUseCase: GetWalletInfoUseCase,
                                    currencyFormatUtils: CurrencyFormatUtils,
                                    data: BackupWalletData,
                                    navigator: BackupWalletNavigator): BackupWalletPresenter {
    return BackupWalletPresenter(fragment as BackupWalletFragmentView, data, getWalletInfoUseCase,
        navigator, currencyFormatUtils, CompositeDisposable(), Schedulers.io(),
        AndroidSchedulers.mainThread())
  }

  @Provides
  fun providesBackupWalletData(fragment: BackupWalletFragment): BackupWalletData {
    fragment.requireArguments()
        .apply {
          return BackupWalletData(getString(PARAM_WALLET_ADDR)!!)
        }
  }

  @Provides
  fun providesBackupNavigator(fragment: BackupWalletFragment): BackupWalletNavigator {
    return BackupWalletNavigator(fragment.requireFragmentManager())
  }
}