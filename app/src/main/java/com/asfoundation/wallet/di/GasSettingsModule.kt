package com.asfoundation.wallet.di

import com.asfoundation.wallet.interact.FindDefaultNetworkInteract
import com.asfoundation.wallet.repository.GasPreferenceRepository
import com.asfoundation.wallet.ui.GasSettingsInteractor
import com.asfoundation.wallet.viewmodel.GasSettingsViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
class GasSettingsModule {
  @Provides
  fun provideGasSettingsViewModelFactory(gasSettingsInteractor: GasSettingsInteractor) =
      GasSettingsViewModelFactory(gasSettingsInteractor)

  @Provides
  fun providesGasSettingsInteractor(
      findDefaultNetworkInteract: FindDefaultNetworkInteract,
      gasPreferenceRepository: GasPreferenceRepository): GasSettingsInteractor {
    return GasSettingsInteractor(findDefaultNetworkInteract, gasPreferenceRepository)
  }
}