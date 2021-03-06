package com.sudoajay.historycachecleaner.activity.main.root

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.sudoajay.historycachecleaner.activity.main.MainActivityViewModel
import com.sudoajay.historycachecleaner.activity.main.database.App
import com.sudoajay.historycachecleaner.helper.DeleteCache
import com.sudoajay.historycachecleaner.helper.storagePermission.AndroidSdCardPermission
import eu.chainfire.libsuperuser.Shell
import java.io.File
import java.util.*


class RootManager(private var viewModel: MainActivityViewModel, var context: Context) {

    var TAG = "RootManagerTAG"
    private val SU_BINARY_DIRS = arrayOf(
        "/system/bin",
        "/system/sbin",
        "/system/xbin",
        "/vendor/bin",
        "/sbin"
    )

    private val cachePath = "/cache"
    private val codeCache = "/code_cache"


    fun hasRootedPermission(): Boolean {
        return Shell.SU.available()
    }

    fun wasRooted(): Boolean {
        var hasRooted = false
        for (path in SU_BINARY_DIRS) {
            val su = File("$path/su")
            if (su.exists()) {
                hasRooted = true
                break
            } else {
                hasRooted = false
            }
        }
        return hasRooted
    }

//    fun removeApps(appsToRemove: List<App>) {
//        CoroutineScope(Dispatchers.IO).launch {
//            var successfully = true
//            for (app in appsToRemove) {
//                if (isRootAccessAlreadyObtained(context)) {
//                    var result: Boolean
//                    if (app.isSystemApp) {
//                        result = uninstallSystemApp(app.path)
//                        if (!result) result = uninstallSystemAppAlternativeMethod(app.packageName)
//                    } else result = uninstallUserApp(app.packageName)
//                    if (!result) successfully = false
//                } else {
//                    if (app.isUserApp)
//                        uninstallUserAppUnRooted(app.packageName)
//                    else
//                        viewModel.successfullyAppRemoved.postValue(false)
//
//                }
//            }
//            if (isRootAccessAlreadyObtained(context))
//                viewModel.successfullyAppRemoved.postValue(successfully)
//
//        }
//    }


    fun removeCacheFolderUnRoot(selectedList: MutableList<App>) {
        selectedList.forEach {
            DeleteCache.deleteWithFile(File(RootManager.getInternalCachePath(context) + it.packageName + cachePath))
            DeleteCache.deleteWithFile(File(RootManager.getInternalCachePath(context) + it.packageName + codeCache))

            DeleteCache.deleteWithFile(File(RootManager.getExternalCachePath(context) + it.packageName + cachePath))

            DeleteCache.deleteWithFile(File(RootManager.getSdCardCachePath(context) + it.packageName + cachePath))
        }
        Log.e(TAG , "Done File deleted with Un root ")
    }

    fun removeCacheFolderRoot(selectedList: MutableList<App>) {
        selectedList.forEach {
            executeCommandSH("rm  -rf %s".format(getInternalCachePath(context) + it.packageName + cachePath))
            executeCommandSH("rm  -rf %s".format(getInternalCachePath(context) + it.packageName + codeCache))

            executeCommandSH("rm  -rf %s".format(getExternalCachePath(context) + it.packageName + cachePath))

            executeCommandSH("rm  -rf %s".format(getSdCardCachePath(context) + it.packageName + cachePath))
        }
        Log.e(TAG , "Done File deleted with root ")
    }
    private fun uninstallSystemApp(appApk: String): Boolean {
        executeCommandSU("mount -o rw,remount /system")
        executeCommandSU("rm $appApk")
        executeCommandSU("mount -o ro,remount /system")
        return checkUninstallSuccessful(appApk)
    }

    private fun uninstallSystemAppAlternativeMethod(packageName: String): Boolean {
        val commandOutput = executeCommandSU("pm uninstall --user 0 $packageName")
        return checkCommandSuccesfull(commandOutput)
    }

    private fun uninstallUserApp(packageName: String): Boolean {
        val commandOutput = executeCommandSU("pm uninstall $packageName")
        return checkCommandSuccesfull(commandOutput)
    }

    private fun uninstallUserAppUnRooted(packageName: String) {
        val packageURI = Uri.parse("package:$packageName")
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI)
        uninstallIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        context.startActivity(uninstallIntent)
    }

    private fun executeCommandSU(command: String): String {
        val stdout: List<String> = ArrayList()
        val stderr: List<String> = ArrayList()
        try {
            Shell.Pool.SU.run(command, stdout, stderr, true)
        } catch (e: Shell.ShellDiedException) {
            e.printStackTrace()
        }
        val stringBuilder = StringBuilder()
        for (line in stdout) {
            Log.e(TAG, "  here - $line")
            stringBuilder.append(line).append("\n")
        }
        return stringBuilder.toString()
    }

    private fun executeCommandSH(command: String): String {
        val stdout: List<String> = ArrayList()
        val stderr: List<String> = ArrayList()
        try {
            Shell.Pool.SH.run(command, stdout, stderr, true)
        } catch (e: Shell.ShellDiedException) {
            e.printStackTrace()
        }
        val stringBuilder = StringBuilder()
        for (line in stdout) {
            stringBuilder.append(line).append("\n")
        }
        return stringBuilder.toString()
    }

    private fun checkUninstallSuccessful(appApk: String): Boolean {
        val output = executeCommandSH("ls $appApk")
        return output.trim { it <= ' ' }.isEmpty()
    }

    private fun checkCommandSuccesfull(commandOutput: String?): Boolean {
        Log.e(TAG, commandOutput.toString() + " ---- ")
        return commandOutput != null && commandOutput.toLowerCase(Locale.ROOT).contains("success")
    }


    fun rebootDevice(): String {
        return executeCommandSU("reboot")
    }

    companion object {

        fun getExternalCachePath(context: Context): String =
            context.externalCacheDir!!.absolutePath.toString().substringBefore(context.packageName)

        fun getInternalCachePath(context: Context): String =
            context.cacheDir.absolutePath.toString().substringBefore(context.packageName)

        fun getSdCardCachePath(context: Context): String =
            AndroidSdCardPermission.getSdCardPath(context) + "Android/data/"

    }
}