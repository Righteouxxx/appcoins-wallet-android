package com.asfoundation.wallet.wallet_verification

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.asf.wallet.R
import com.asfoundation.wallet.wallet_verification.intro.WalletVerificationIntroFragment

class WalletVerificationActivityNavigator(private val context: Context,
                                          private val fragmentManager: FragmentManager) {

  fun navigateToInitialWalletVerification() {
    fragmentManager.beginTransaction()
        .replace(R.id.fragment_container,
            WalletVerificationIntroFragment.newInstance())
        .commit()
  }

}
