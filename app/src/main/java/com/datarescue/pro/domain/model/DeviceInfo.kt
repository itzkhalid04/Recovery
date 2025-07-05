package com.datarescue.pro.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeviceInfo(
    val isRooted: Boolean,
    val rootMethod: String,
    val availablePartitions: List<String>,
    val fileSystemType: String,
    val totalStorage: Long,
    val freeStorage: Long,
    val capabilities: DeviceCapabilities
) : Parcelable

@Parcelize
data class DeviceCapabilities(
    val canAccessSystemPartition: Boolean,
    val canPerformDeepScan: Boolean,
    val canRecoverDeletedFiles: Boolean,
    val canAccessRawDevice: Boolean,
    val supportedFileSystems: List<String>
) : Parcelable

enum class RootStatus {
    NOT_ROOTED,
    ROOTED_MAGISK,
    ROOTED_SUPERSU,
    ROOTED_KINGROOT,
    ROOTED_OTHER,
    UNKNOWN
}

enum class ScanMode {
    BASIC,      // Non-root scanning
    ADVANCED,   // Root-based scanning
    DEEP        // Raw device scanning (requires root)
}