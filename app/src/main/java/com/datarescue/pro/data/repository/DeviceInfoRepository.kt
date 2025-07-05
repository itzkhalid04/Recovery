package com.datarescue.pro.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.datarescue.pro.data.native.NativeFileScanner
import com.datarescue.pro.domain.model.*
import com.topjohnwu.libsu.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceInfoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nativeScanner: NativeFileScanner
) {
    
    suspend fun getDeviceInfo(): DeviceInfo = withContext(Dispatchers.IO) {
        val isRooted = checkRootStatus()
        val rootMethod = if (isRooted) detectRootMethod() else "None"
        val partitions = if (isRooted) nativeScanner.getAvailablePartitions().toList() else emptyList()
        val fsType = getFileSystemType()
        val (totalStorage, freeStorage) = getStorageInfo()
        val capabilities = getDeviceCapabilities(isRooted)
        
        DeviceInfo(
            isRooted = isRooted,
            rootMethod = rootMethod,
            availablePartitions = partitions,
            fileSystemType = fsType,
            totalStorage = totalStorage,
            freeStorage = freeStorage,
            capabilities = capabilities
        )
    }
    
    private suspend fun checkRootStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            // First check with native scanner
            if (nativeScanner.isRootAvailable()) {
                return@withContext true
            }
            
            // Check with libsu
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }
    
    private fun detectRootMethod(): String {
        return when {
            checkForMagisk() -> "Magisk"
            checkForSuperSU() -> "SuperSU"
            checkForKingRoot() -> "KingRoot"
            checkForXposed() -> "Xposed"
            else -> "Unknown"
        }
    }
    
    private fun checkForMagisk(): Boolean {
        val magiskPaths = listOf(
            "/sbin/.magisk",
            "/system/addon.d/99-magisk.sh",
            "/data/adb/magisk",
            "/cache/.disable_magisk"
        )
        
        return magiskPaths.any { File(it).exists() } ||
                isPackageInstalled("com.topjohnwu.magisk")
    }
    
    private fun checkForSuperSU(): Boolean {
        val superSUPaths = listOf(
            "/system/app/SuperSU.apk",
            "/system/app/SuperSU",
            "/system/xbin/daemonsu",
            "/system/etc/init.d/99SuperSUDaemon"
        )
        
        return superSUPaths.any { File(it).exists() } ||
                isPackageInstalled("eu.chainfire.supersu")
    }
    
    private fun checkForKingRoot(): Boolean {
        val kingRootPaths = listOf(
            "/data/data/com.kingroot.kinguser",
            "/system/app/Kinguser.apk",
            "/system/bin/kingo"
        )
        
        return kingRootPaths.any { File(it).exists() } ||
                isPackageInstalled("com.kingroot.kinguser")
    }
    
    private fun checkForXposed(): Boolean {
        val xposedPaths = listOf(
            "/system/framework/XposedBridge.jar",
            "/system/bin/app_process_xposed"
        )
        
        return xposedPaths.any { File(it).exists() } ||
                isPackageInstalled("de.robv.android.xposed.installer")
    }
    
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getFileSystemType(): String {
        return try {
            val dataDir = Environment.getDataDirectory()
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    // Modern Android typically uses F2FS or EXT4
                    if (File("/sys/fs/f2fs").exists()) "F2FS" else "EXT4"
                }
                else -> "EXT4"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getStorageInfo(): Pair<Long, Long> {
        return try {
            val dataDir = Environment.getDataDirectory()
            val stat = StatFs(dataDir.path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            Pair(totalBytes, freeBytes)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }
    
    private fun getDeviceCapabilities(isRooted: Boolean): DeviceCapabilities {
        return DeviceCapabilities(
            canAccessSystemPartition = isRooted,
            canPerformDeepScan = isRooted,
            canRecoverDeletedFiles = true, // Always possible to some extent
            canAccessRawDevice = isRooted,
            supportedFileSystems = if (isRooted) {
                listOf("EXT4", "F2FS", "FAT32", "NTFS")
            } else {
                listOf("User accessible areas only")
            }
        )
    }
}