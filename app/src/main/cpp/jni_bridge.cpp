#include <jni.h>
#include "include/native_scanner.h"
#include <android/log.h>
#include <memory>
#include <vector>
#include <string>

#define LOG_TAG "JNIBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::unique_ptr<NativeScanner> g_scanner;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_initializeNative(JNIEnv *, jobject, jboolean isRooted) {
    LOGI("Initializing native scanner with root: %s", isRooted ? "true" : "false");
    
    g_scanner = std::make_unique<NativeScanner>();
    if (g_scanner) {
        return g_scanner->initialize(isRooted);
    }
    return false;
}

JNIEXPORT jboolean JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_isRootAvailable(JNIEnv *, jobject) {
    if (!g_scanner) {
        g_scanner = std::make_unique<NativeScanner>();
    }
    if (g_scanner) {
        return g_scanner->isRootAvailable();
    }
    return false;
}

JNIEXPORT jobjectArray JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_getAvailablePartitions(JNIEnv *env, jobject) {
    if (!g_scanner) {
        jclass stringClass = env->FindClass("java/lang/String");
        return env->NewObjectArray(0, stringClass, nullptr);
    }
    
    auto partitions = g_scanner->getAvailablePartitions();
    
    jclass stringClass = env->FindClass("java/lang/String");
    if (!stringClass) {
        LOGE("Failed to find String class");
        return env->NewObjectArray(0, stringClass, nullptr);
    }
    
    jobjectArray result = env->NewObjectArray(partitions.size(), stringClass, nullptr);
    if (!result) {
        LOGE("Failed to create object array");
        return env->NewObjectArray(0, stringClass, nullptr);
    }
    
    for (size_t i = 0; i < partitions.size(); ++i) {
        jstring partition = env->NewStringUTF(partitions[i].c_str());
        if (partition) {
            env->SetObjectArrayElement(result, i, partition);
            env->DeleteLocalRef(partition);
        }
    }
    
    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_startDeepScan(JNIEnv *env, jobject, 
                                                                    jstring partition, 
                                                                    jintArray fileTypes) {
    if (!g_scanner) {
        LOGE("Scanner not initialized");
        jclass fileClass = env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile");
        return env->NewObjectArray(0, fileClass, nullptr);
    }
    
    const char* partitionStr = env->GetStringUTFChars(partition, nullptr);
    if (!partitionStr) {
        LOGE("Failed to get partition string");
        jclass fileClass = env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile");
        return env->NewObjectArray(0, fileClass, nullptr);
    }
    
    jint* fileTypesArray = env->GetIntArrayElements(fileTypes, nullptr);
    jsize fileTypesLength = env->GetArrayLength(fileTypes);
    
    if (!fileTypesArray) {
        env->ReleaseStringUTFChars(partition, partitionStr);
        LOGE("Failed to get file types array");
        jclass fileClass = env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile");
        return env->NewObjectArray(0, fileClass, nullptr);
    }
    
    std::vector<int> fileTypeVector;
    for (int i = 0; i < fileTypesLength; ++i) {
        fileTypeVector.push_back(fileTypesArray[i]);
    }
    
    auto results = g_scanner->startDeepScan(partitionStr, fileTypeVector, nullptr);
    
    // Convert results to Java objects
    jclass fileClass = env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile");
    if (!fileClass) {
        env->ReleaseStringUTFChars(partition, partitionStr);
        env->ReleaseIntArrayElements(fileTypes, fileTypesArray, 0);
        LOGE("Failed to find NativeRecoverableFile class");
        return env->NewObjectArray(0, env->FindClass("java/lang/Object"), nullptr);
    }
    
    jmethodID constructor = env->GetMethodID(fileClass, "<init>", 
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JJJIZZI)V");
    if (!constructor) {
        env->ReleaseStringUTFChars(partition, partitionStr);
        env->ReleaseIntArrayElements(fileTypes, fileTypesArray, 0);
        LOGE("Failed to find constructor");
        return env->NewObjectArray(0, fileClass, nullptr);
    }
    
    jobjectArray resultArray = env->NewObjectArray(results.size(), fileClass, nullptr);
    if (!resultArray) {
        env->ReleaseStringUTFChars(partition, partitionStr);
        env->ReleaseIntArrayElements(fileTypes, fileTypesArray, 0);
        LOGE("Failed to create result array");
        return env->NewObjectArray(0, fileClass, nullptr);
    }
    
    for (size_t i = 0; i < results.size(); ++i) {
        const auto& file = results[i];
        
        jstring name = env->NewStringUTF(file.name.c_str());
        jstring path = env->NewStringUTF(file.path.c_str());
        jstring originalPath = env->NewStringUTF(file.originalPath.c_str());
        
        if (!name || !path || !originalPath) {
            if (name) env->DeleteLocalRef(name);
            if (path) env->DeleteLocalRef(path);
            if (originalPath) env->DeleteLocalRef(originalPath);
            continue;
        }
        
        jobject fileObj = env->NewObject(fileClass, constructor,
            name, path, originalPath,
            (jlong)file.size,
            (jlong)file.dateModified,
            (jlong)file.dateDeleted,
            (jint)file.fileType,
            (jboolean)file.isDeleted,
            (jboolean)file.isRecoverable,
            (jint)file.confidence
        );
        
        if (fileObj) {
            env->SetObjectArrayElement(resultArray, i, fileObj);
            env->DeleteLocalRef(fileObj);
        }
        
        env->DeleteLocalRef(name);
        env->DeleteLocalRef(path);
        env->DeleteLocalRef(originalPath);
    }
    
    env->ReleaseStringUTFChars(partition, partitionStr);
    env->ReleaseIntArrayElements(fileTypes, fileTypesArray, 0);
    
    return resultArray;
}

JNIEXPORT jobjectArray JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_startQuickScan(JNIEnv *env, jobject, jintArray fileTypes) {
    if (!g_scanner) {
        LOGE("Scanner not initialized");
        jclass fileClass = env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile");
        return env->NewObjectArray(0, fileClass, nullptr);
    }
    
    jint* fileTypesArray = env->GetIntArrayElements(fileTypes, nullptr);
    jsize fileTypesLength = env->GetArrayLength(fileTypes);
    
    if (!fileTypesArray) {
        LOGE("Failed to get file types array");
        jclass fileClass = env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile");
        return env->NewObjectArray(0, fileClass, nullptr);
    }
    
    std::vector<int> fileTypeVector;
    for (int i = 0; i < fileTypesLength; ++i) {
        fileTypeVector.push_back(fileTypesArray[i]);
    }
    
    auto results = g_scanner->startQuickScan(fileTypeVector, nullptr);
    
    // Convert results to Java objects (same as deep scan)
    jclass fileClass = env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile");
    if (!fileClass) {
        env->ReleaseIntArrayElements(fileTypes, fileTypesArray, 0);
        LOGE("Failed to find NativeRecoverableFile class");
        return env->NewObjectArray(0, env->FindClass("java/lang/Object"), nullptr);
    }
    
    jmethodID constructor = env->GetMethodID(fileClass, "<init>", 
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JJJIZZI)V");
    if (!constructor) {
        env->ReleaseIntArrayElements(fileTypes, fileTypesArray, 0);
        LOGE("Failed to find constructor");
        return env->NewObjectArray(0, fileClass, nullptr);
    }
    
    jobjectArray resultArray = env->NewObjectArray(results.size(), fileClass, nullptr);
    if (!resultArray) {
        env->ReleaseIntArrayElements(fileTypes, fileTypesArray, 0);
        LOGE("Failed to create result array");
        return env->NewObjectArray(0, fileClass, nullptr);
    }
    
    for (size_t i = 0; i < results.size(); ++i) {
        const auto& file = results[i];
        
        jstring name = env->NewStringUTF(file.name.c_str());
        jstring path = env->NewStringUTF(file.path.c_str());
        jstring originalPath = env->NewStringUTF(file.originalPath.c_str());
        
        if (!name || !path || !originalPath) {
            if (name) env->DeleteLocalRef(name);
            if (path) env->DeleteLocalRef(path);
            if (originalPath) env->DeleteLocalRef(originalPath);
            continue;
        }
        
        jobject fileObj = env->NewObject(fileClass, constructor,
            name, path, originalPath,
            (jlong)file.size,
            (jlong)file.dateModified,
            (jlong)file.dateDeleted,
            (jint)file.fileType,
            (jboolean)file.isDeleted,
            (jboolean)file.isRecoverable,
            (jint)file.confidence
        );
        
        if (fileObj) {
            env->SetObjectArrayElement(resultArray, i, fileObj);
            env->DeleteLocalRef(fileObj);
        }
        
        env->DeleteLocalRef(name);
        env->DeleteLocalRef(path);
        env->DeleteLocalRef(originalPath);
    }
    
    env->ReleaseIntArrayElements(fileTypes, fileTypesArray, 0);
    
    return resultArray;
}

JNIEXPORT jboolean JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_recoverFile(JNIEnv *env, jobject, 
                                                                  jstring sourcePath, 
                                                                  jstring outputPath) {
    if (!g_scanner) {
        LOGE("Scanner not initialized");
        return false;
    }
    
    const char* sourceStr = env->GetStringUTFChars(sourcePath, nullptr);
    const char* outputStr = env->GetStringUTFChars(outputPath, nullptr);
    
    if (!sourceStr || !outputStr) {
        if (sourceStr) env->ReleaseStringUTFChars(sourcePath, sourceStr);
        if (outputStr) env->ReleaseStringUTFChars(outputPath, outputStr);
        LOGE("Failed to get path strings");
        return false;
    }
    
    // Create a RecoveredFileInfo from the source path
    RecoveredFileInfo fileInfo;
    fileInfo.path = sourceStr;
    fileInfo.name = "recovered_file";
    
    bool result = g_scanner->recoverFile(fileInfo, outputStr);
    
    env->ReleaseStringUTFChars(sourcePath, sourceStr);
    env->ReleaseStringUTFChars(outputPath, outputStr);
    
    return result;
}

JNIEXPORT void JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_stopScan(JNIEnv *, jobject) {
    if (g_scanner) {
        g_scanner->stopScan();
    }
}

} // extern "C"