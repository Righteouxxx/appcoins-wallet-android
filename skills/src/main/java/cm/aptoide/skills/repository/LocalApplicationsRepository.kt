package cm.aptoide.skills.repository

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

class LocalApplicationsRepository(private val packageManager: PackageManager) {
  fun getApplicationName(packageName: String): String {
    val packageInfo = packageManager.getApplicationInfo(packageName, 0)
    return packageManager.getApplicationLabel(packageInfo)
        .toString()
  }

  fun getApplicationIcon(packageName: String): Drawable {
    return packageManager.getApplicationIcon(packageName)
  }
}
