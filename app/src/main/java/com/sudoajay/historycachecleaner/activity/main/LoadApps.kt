package com.sudoajay.historycachecleaner.activity.main

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.sudoajay.historycachecleaner.activity.main.database.App
import com.sudoajay.historycachecleaner.activity.main.database.AppRepository
import com.sudoajay.historycachecleaner.helper.FileHelper
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class LoadApps(private val context: Context, private  val appRepository: AppRepository) {
    private lateinit var packageManager: PackageManager

    suspend fun searchInstalledApps() {
        appDatabaseConfiguration(getInstalledApplication(context))

    }

    private fun getInstalledApplication(context: Context): List<ApplicationInfo> {
        packageManager = context.packageManager
        return packageManager.getInstalledApplications(0)
    }

    private suspend fun appDatabaseConfiguration(installedApplicationsInfo: List<ApplicationInfo>) {


        //        Here we Just add default value of install app
        appRepository.setDefaultValueInstall()


//        Here we Just add new Install App Into Data base

        for (applicationInfo in installedApplicationsInfo) {
            val packageName = getApplicationPackageName(applicationInfo)
            if (appRepository.isPresent(packageName) == 0)
                createApp(applicationInfo)
            else
                appRepository.updateInstalledAndCacheByPackage(
                    packageName, FileHelper(context,packageName).fileLength()
                )
        }

//        Here we remove Uninstall App from Data base
        appRepository.removeUninstallAppFromDB()



    }

    private suspend fun createApp(applicationInfo: ApplicationInfo) {

        val label = getApplicationLabel(applicationInfo)
        val sourceDir = getApplicationSourceDir(applicationInfo)
        val packageName = getApplicationPackageName(applicationInfo)
        val icon = getApplicationsIcon(applicationInfo)
        val installedDate = getInstalledDate(packageName)
        val systemApp = isSystemApps(applicationInfo)

        // return size in form of Bytes(Long)
        val cacheSize = FileHelper(context,packageName).fileLength()

        appRepository.insert(
            App(
                null,
                label,
                sourceDir,
                packageName,
                icon,
                installedDate,
                cacheSize,
                systemApp,
                !systemApp,
                isSelected = true,
                isInstalled = true
            )
        )
    }


    private fun isSystemApps(applicationInfo: ApplicationInfo): Boolean =
        applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1


    private fun getApplicationLabel(applicationInfo: ApplicationInfo): String =
        packageManager.getApplicationLabel(applicationInfo) as String


    private fun getApplicationSourceDir(applicationInfo: ApplicationInfo): String =
        applicationInfo.sourceDir


    private fun getApplicationPackageName(applicationInfo: ApplicationInfo): String =
        applicationInfo.packageName


    private fun getApplicationsIcon(applicationInfo: ApplicationInfo): String {
        return try {
            applicationInfo.processName
        } catch (e: PackageManager.NameNotFoundException) {
            "defaultApplicationIcon"
        }
    }

    private fun getInstalledDate(packageName: String): String {
        val installDate: Long? = try {
            packageManager.getPackageInfo(packageName, 0).firstInstallTime
        } catch (e: PackageManager.NameNotFoundException) {
            Calendar.getInstance().timeInMillis
        }
        return convertDateToStringFormat(Date(installDate!!))
    }

    private fun convertDateToStringFormat(date: Date): String {
        val pattern = "yyyy-MM-dd HH:mm:ss"

        val df: DateFormat = SimpleDateFormat(pattern, Locale.getDefault())

        return df.format(date)

    }

}