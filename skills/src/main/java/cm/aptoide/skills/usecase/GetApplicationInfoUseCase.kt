package cm.aptoide.skills.usecase

import cm.aptoide.skills.model.ApplicationInfo
import cm.aptoide.skills.repository.LocalApplicationsRepository

class GetApplicationInfoUseCase(
    private val localApplicationsRepository: LocalApplicationsRepository) {
  operator fun invoke(packageName: String): ApplicationInfo {
    return ApplicationInfo(packageName, localApplicationsRepository.getApplicationName(packageName),
        localApplicationsRepository.getApplicationIcon(packageName))
  }
}
