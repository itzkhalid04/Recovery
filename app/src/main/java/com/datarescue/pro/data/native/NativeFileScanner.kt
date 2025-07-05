package com.datarescue.pro.data.native

import android.util.Log

class NativeFileScanner {
    
    companion object {
        private const val TAG = "NativeFileScanner"
        
        init {
            try {
                System.loadLibrary("datarescue_native")
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
    }
    
    external fun initializeNative(isRooted: Boolean): Boolean
    external fun isRootAvailable(): Boolean
    external fun getAvailablePartitions(): Array<String>
    external fun startDeepScan(partition: String, fileTypes: IntArray): Array<NativeRecoverableFile>
    external fun startQuickScan(fileTypes: IntArray): Array<NativeRecoverableFile>
    external fun recoverFile(sourcePath: String, outputPath: String): Boolean
    external fun stopScan()
}

data class NativeRecoverableFile(
    val name: String,
    val path: String,
    val originalPath: String,
    val size: Long,
    val dateModified: Long,
    val dateDeleted: Long,
    val fileType: Int,
    val isDeleted: Boolean,
    val isRecoverable: Boolean,
    val confidence: Int
)