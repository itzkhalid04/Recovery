#include <jni.h>
#include "include/native_scanner.h"
#include <android/log.h>
#include <memory>
#include <vector>

#define LOG_TAG "JNIBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::unique_ptr<NativeScanner> g_scanner;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_initializeNative(JNIEnv *env, jobject thiz, jboolean isRooted) {
    LOGI("Initializing native scanner with root: %s", isRooted ? "true" : "false");
    
    g_scanner = std::make_unique<NativeScanner>();
    return g_scanner->initialize(isRooted);
}

JNIEXPORT jboolean JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_isRootAvailable(JNIEnv *env, jobject thiz) {
    if (!g_scanner) {
        g_scanner = std::make_unique<NativeScanner>();
    }
    return g_scanner->isRootAvailable();
}

JNIEXPORT jobjectArray JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_getAvailablePartitions(JNIEnv *env, jobject thiz) {
    if (!g_scanner) {
        return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
    }
    
    auto partitions = g_scanner->getAvailablePartitions();
    
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(partitions.size(), stringClass, nullptr);
    
    for (size_t i = 0; i < partitions.size(); ++i) {
        jstring partition = env->NewStringUTF(partitions[i].c_str());
        env->SetObjectArrayElement(result, i, partition);
        env->DeleteLocalRef(partition);
    }
    
    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_startDeepScan(JNIEnv *env, jobject thiz, 
                                                                    jstring partition, 
                                                                    jintArray fileTypes) {
    if (!g_scanner) {
        LOGE("Scanner not initialized");
        return env->NewObjectArray(0, env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile"), nullptr);
    }
    
    const char* partitionStr = env->GetStringUTFChars(partition, nullptr);
    
    jint* fileTypesArray = env->GetIntArrayElements(fileTypes, nullptr);
    jsize fileTypesLength = env->GetArrayLength(fileTypes);
    
    std::vector<int> fileTypeVector;
    for (int i = 0; i < fileTypesLength; ++i) {
        fileTypeVector.push_back(fileTypesArray[i]);
    }
    
    auto results = g_scanner->startDeepScan(partitionStr, fileTypeVector, nullptr);
    
    // Convert results to Java objects
    jclass fileClass = env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile");
    jmethodID constructor = env->GetMethodID(fileClass, "<init>", 
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JJJIZZI)V");
    
    jobjectArray resultArray = env->NewObjectArray(results.size(), fileClass, nullptr);
    
    for (size_t i = 0; i < results.size(); ++i) {
        const auto& file = results[i];
        
        jstring name = env->NewStringUTF(file.name.c_str());
        jstring path = env->NewStringUTF(file.path.c_str());
        jstring originalPath = env->NewStringUTF(file.originalPath.c_str());
        
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
        
        env->SetObjectArrayElement(resultArray, i, fileObj);
        
        env->DeleteLocalRef(name);
        env->DeleteLocalRef(path);
        env->DeleteLocalRef(originalPath);
        env->DeleteLocalRef(fileObj);
    }
    
    env->ReleaseStringUTFChars(partition, partitionStr);
    env->ReleaseIntArrayElements(fileTypes, fileTypesArray, 0);
    
    return resultArray;
}

JNIEXPORT jobjectArray JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_startQuickScan(JNIEnv *env, jobject thiz, jintArray fileTypes) {
    if (!g_scanner) {
        LOGE("Scanner not initialized");
        return env->NewObjectArray(0, env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile"), nullptr);
    }
    
    jint* fileTypesArray = env->GetIntArrayElements(fileTypes, nullptr);
    jsize fileTypesLength = env->GetArrayLength(fileTypes);
    
    std::vector<int> fileTypeVector;
    for (int i = 0; i < fileTypesLength; ++i) {
        fileTypeVector.push_back(fileTypesArray[i]);
    }
    
    auto results = g_scanner->startQuickScan(fileTypeVector, nullptr);
    
    // Convert results to Java objects (same as deep scan)
    jclass fileClass = env->FindClass("com/datarescue/pro/data/native/NativeRecoverableFile");
    jmethodID constructor = env->GetMethodID(fileClass, "<init>", 
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JJJIZZI)V");
    
    jobjectArray resultArray = env->NewObjectArray(results.size(), fileClass, nullptr);
    
    for (size_t i = 0; i < results.size(); ++i) {
        const auto& file = results[i];
        
        jstring name = env->NewStringUTF(file.name.c_str());
        jstring path = env->NewStringUTF(file.path.c_str());
        jstring originalPath = env->NewStringUTF(file.originalPath.c_str());
        
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
        
        env->SetObjectArrayElement(resultArray, i, fileObj);
        
        env->DeleteLocalRef(name);
        env->DeleteLocalRef(path);
        env->DeleteLocalRef(originalPath);
        env->DeleteLocalRef(fileObj);
    }
    
    env->ReleaseIntArrayElements(fileTypes, fileTypesArray, 0);
    
    return resultArray;
}

JNIEXPORT jboolean JNICALL
Java_com_datarescue_pro_data_native_NativeFileScanner_recoverFile(JNIEnv *env, jobject thiz, 
                                                                  jstring sourcePath, 
                                                                  jstring outputPath) {
    if (!g_scanner) {
        LOGE("Scanner not initialized");
        return false;
    }
    
    const char* sourceStr = env->GetStringUTFChars(sourcePath, nullptr);
    const char* outputStr = env->GetStringUTFChars(outputPath, nullptr);
    
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
Java_com_datarescue_pro_data_native_NativeFileScanner_stopScan(JNIEnv *env, jobject thiz) {
    if (g_scanner) {
        g_scanner->stopScan();
    }
}

} // extern "C"