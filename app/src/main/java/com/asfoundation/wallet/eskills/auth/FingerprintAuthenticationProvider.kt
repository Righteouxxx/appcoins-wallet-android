package com.asfoundation.wallet.eskills.auth

import android.content.Context
import android.content.Intent
import cm.aptoide.skills.interfaces.ExternalAuthenticationProvider
import com.asfoundation.wallet.fingerprint.FingerprintPreferencesRepositoryContract
import com.asfoundation.wallet.ui.AuthenticationPromptActivity

class FingerprintAuthenticationProvider(
    private val fingerprintPreferences: FingerprintPreferencesRepositoryContract) :
    ExternalAuthenticationProvider {
  override fun hasAuthenticationPermission(): Boolean {
    return fingerprintPreferences.hasAuthenticationPermission()
  }

  override fun getAuthenticationIntent(context: Context): Intent {
    return AuthenticationPromptActivity.newIntent(context)
        .apply { this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP }
  }
}
