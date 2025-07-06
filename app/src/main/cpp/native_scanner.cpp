#include "include/native_scanner.h"
#include "filesystem/ext4_scanner.h"
#include "filesystem/f2fs_scanner.h"
#include "filesystem/fat32_scanner.h"
#include "recovery/file_carver.h"
#include "recovery/signature_detector.h"
#include "utils/root_utils.h"
#include "utils/disk_utils.h"
#include <android/log.h>
#include <unistd.h>
#include <sys/stat.h>
#include <dirent.h>
#include <fstream>
#include <algorithm>
#include <chrono>
#include <memory>
#include <cstring>
#include <ctime>

#define LOG_TAG "DataRescueNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Forward declarations for filesystem scanner interface
class FileSystemScanner {
public:
    virtual ~FileSystemScanner() = default;
    virtual bool initialize(bool isRooted) = 0;
    virtual std::vector<RecoveredFileInfo> scanDeletedFiles(
        const std::string& partition,
        const std::vector<int>& fileTypes,
        std::function<bool(const ScanProgress&)> progressCallback
    ) = 0;
};

// Wrapper classes for filesystem scanners
class Ext4ScannerWrapper : public FileSystemScanner {
private:
    std::unique_ptr<Ext4Scanner> scanner;
public:
    Ext4ScannerWrapper() : scanner(std::make_unique<Ext4Scanner>()) {}
    
    bool initialize(bool isRooted) override {
        return scanner->initialize(isRooted);
    }
    
    std::vector<RecoveredFileInfo> scanDeletedFiles(
        const std::string& partition,
        const std::vector<int>& fileTypes,
        std::function<bool(const ScanProgress&)> progressCallback
    ) override {
        return scanner->scanDeletedFiles(partition, fileTypes, progressCallback);
    }
};

class F2fsScannerWrapper : public FileSystemScanner {
private:
    std::unique_ptr<F2fsScanner> scanner;
public:
    F2fsScannerWrapper() : scanner(std::make_unique<F2fsScanner>()) {}
    
    bool initialize(bool isRooted) override {
        return scanner->initialize(isRooted);
    }
    
    std::vector<RecoveredFileInfo> scanDeletedFiles(
        const std::string& partition,
        const std::vector<int>& fileTypes,
        std::function<bool(const ScanProgress&)> progressCallback
    ) override {
        return scanner->scanDeletedFiles(partition, fileTypes, progressCallback);
    }
};

class Fat32ScannerWrapper : public FileSystemScanner {
private:
    std::unique_ptr<Fat32Scanner> scanner;
public:
    Fat32ScannerWrapper() : scanner(std::make_unique<Fat32Scanner>()) {}
    
    bool initialize(bool isRooted) override {
        return scanner->initialize(isRooted);
    }
    
    std::vector<RecoveredFileInfo> scanDeletedFiles(
        const std::string& partition,
        const std::vector<int>& fileTypes,
        std::function<bool(const ScanProgress&)> progressCallback
    ) override {
        return scanner->scanDeletedFiles(partition, fileTypes, progressCallback);
    }
};

NativeScanner::NativeScanner() : m_isRooted(false), m_shouldStop(false) {
    m_signatureDetector = std::make_unique<SignatureDetector>();
    m_fileCarver = std::make_unique<FileCarver>();
}

NativeScanner::~NativeScanner() = default;

bool NativeScanner::initialize(bool isRooted) {
    m_isRooted = isRooted;
    
    if (m_isRooted) {
        LOGI("Initializing with root access");
        if (!RootUtils::checkRootAccess()) {
            LOGE("Root access verification failed");
            return false;
        }
    } else {
        LOGI("Initializing without root access");
    }

    // Initialize file system scanner based on detected file system
    std::string fsType = DiskUtils::getFileSystemType("/data");
    LOGI("Detected file system: %s", fsType.c_str());

    if (fsType == "ext4") {
        m_fsScanner = std::make_unique<Ext4ScannerWrapper>();
    } else if (fsType == "f2fs") {
        m_fsScanner = std::make_unique<F2fsScannerWrapper>();
    } else {
        m_fsScanner = std::make_unique<Fat32ScannerWrapper>();
    }

    return m_fsScanner->initialize(m_isRooted);
}

std::vector<RecoveredFileInfo> NativeScanner::startDeepScan(const std::string& partition,
                                                           const std::vector<int>& fileTypes,
                                                           bool (*progressCallback)(const ScanProgress&)) {
    std::vector<RecoveredFileInfo> results;
    m_shouldStop = false;
    
    LOGI("Starting deep scan on partition: %s", partition.c_str());
    
    auto startTime = std::chrono::steady_clock::now();
    
    if (m_isRooted) {
        // Root mode: Direct file system analysis
        results = m_fsScanner->scanDeletedFiles(partition, fileTypes, [&](const ScanProgress& progress) {
            if (progressCallback) {
                return progressCallback(progress) && !m_shouldStop;
            }
            return !m_shouldStop;
        });
        
        // Add file carving results
        auto carvedFiles = m_fileCarver->carveFiles(partition, fileTypes, [&](const ScanProgress& progress) {
            if (progressCallback) {
                return progressCallback(progress) && !m_shouldStop;
            }
            return !m_shouldStop;
        });
        
        results.insert(results.end(), carvedFiles.begin(), carvedFiles.end());
    } else {
        // Non-root mode: Scan accessible areas
        results = scanAccessibleAreas(fileTypes, progressCallback);
    }
    
    auto endTime = std::chrono::steady_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
    
    LOGI("Deep scan completed. Found %zu files in %lld ms", results.size(), (long long)duration.count());
    
    return results;
}

std::vector<RecoveredFileInfo> NativeScanner::startQuickScan(const std::vector<int>& fileTypes,
                                                            bool (*progressCallback)(const ScanProgress&)) {
    std::vector<RecoveredFileInfo> results;
    m_shouldStop = false;
    
    LOGI("Starting quick scan");
    
    // Quick scan focuses on recently deleted files and cache areas
    std::vector<std::string> quickScanPaths = {
        "/data/data",
        "/sdcard/.trash",
        "/sdcard/Android/data",
        "/sdcard/.cache",
        "/data/local/tmp"
    };
    
    ScanProgress progress = {0, 0, 0, "", 0};
    long long totalPaths = quickScanPaths.size();
    
    for (size_t i = 0; i < quickScanPaths.size() && !m_shouldStop; ++i) {
        progress.percentage = (int)((i * 100) / totalPaths);
        progress.currentFile = quickScanPaths[i];
        
        if (progressCallback && !progressCallback(progress)) {
            break;
        }
        
        auto pathResults = scanDirectory(quickScanPaths[i], fileTypes, 3);
        results.insert(results.end(), pathResults.begin(), pathResults.end());
    }
    
    LOGI("Quick scan completed. Found %zu files", results.size());
    
    return results;
}

std::vector<RecoveredFileInfo> NativeScanner::scanAccessibleAreas(const std::vector<int>& fileTypes,
                                                                  bool (*progressCallback)(const ScanProgress&)) {
    std::vector<RecoveredFileInfo> results;
    
    std::vector<std::string> scanPaths = {
        "/sdcard",
        "/storage/emulated/0",
        "/data/media/0",
        "/sdcard/Android/data",
        "/sdcard/DCIM/.thumbnails",
        "/sdcard/WhatsApp/Media/.Statuses",
        "/sdcard/Telegram/.cache",
        "/sdcard/Instagram/.cache"
    };
    
    ScanProgress progress = {0, 0, 0, "", 0};
    long long totalPaths = scanPaths.size();
    
    for (size_t i = 0; i < scanPaths.size() && !m_shouldStop; ++i) {
        progress.percentage = (int)((i * 100) / totalPaths);
        progress.currentFile = scanPaths[i];
        progress.filesScanned = results.size();
        
        if (progressCallback && !progressCallback(progress)) {
            break;
        }
        
        auto pathResults = scanDirectory(scanPaths[i], fileTypes, 5);
        results.insert(results.end(), pathResults.begin(), pathResults.end());
    }
    
    return results;
}

std::vector<RecoveredFileInfo> NativeScanner::scanDirectory(const std::string& path,
                                                           const std::vector<int>& fileTypes,
                                                           int maxDepth,
                                                           int currentDepth) {
    std::vector<RecoveredFileInfo> results;
    
    if (currentDepth >= maxDepth || m_shouldStop) {
        return results;
    }
    
    DIR* dir = opendir(path.c_str());
    if (!dir) {
        return results;
    }
    
    struct dirent* entry;
    while ((entry = readdir(dir)) != nullptr && !m_shouldStop) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
            continue;
        }
        
        std::string fullPath = path + "/" + entry->d_name;
        struct stat fileStat;
        
        if (stat(fullPath.c_str(), &fileStat) == 0) {
            if (S_ISREG(fileStat.st_mode)) {
                // Regular file
                RecoveredFileInfo fileInfo = analyzeFile(fullPath, fileStat);
                if (shouldIncludeFile(fileInfo, fileTypes)) {
                    results.push_back(fileInfo);
                }
            } else if (S_ISDIR(fileStat.st_mode)) {
                // Directory - recurse
                auto subResults = scanDirectory(fullPath, fileTypes, maxDepth, currentDepth + 1);
                results.insert(results.end(), subResults.begin(), subResults.end());
            }
        }
    }
    
    closedir(dir);
    return results;
}

RecoveredFileInfo NativeScanner::analyzeFile(const std::string& path, const struct stat& fileStat) {
    RecoveredFileInfo info;
    info.path = path;
    info.originalPath = path;
    info.name = path.substr(path.find_last_of('/') + 1);
    info.size = fileStat.st_size;
    info.dateModified = fileStat.st_mtime * 1000LL; // Convert to milliseconds
    info.dateDeleted = 0;
    info.isDeleted = false;
    
    // Determine file type from extension and signature
    info.fileType = m_signatureDetector->detectFileType(path);
    
    // Calculate confidence based on various factors
    info.confidence = calculateConfidence(path, fileStat);
    
    // Check if file is recoverable
    info.isRecoverable = isFileRecoverable(path, fileStat);
    
    return info;
}

int NativeScanner::calculateConfidence(const std::string& path, const struct stat& fileStat) {
    int confidence = 50; // Base confidence
    
    // File size factor
    if (fileStat.st_size > 50 * 1024 * 1024) confidence += 25; // > 50MB
    else if (fileStat.st_size > 10 * 1024 * 1024) confidence += 20; // > 10MB
    else if (fileStat.st_size > 1024 * 1024) confidence += 15; // > 1MB
    
    // Recency factor
    time_t now = time(nullptr);
    long daysSinceModified = (now - fileStat.st_mtime) / (24 * 60 * 60);
    
    if (daysSinceModified < 1) confidence += 25;
    else if (daysSinceModified < 7) confidence += 20;
    else if (daysSinceModified < 30) confidence += 15;
    
    // Location factor
    std::string lowerPath = path;
    std::transform(lowerPath.begin(), lowerPath.end(), lowerPath.begin(), ::tolower);
    
    if (lowerPath.find("cache") != std::string::npos ||
        lowerPath.find("tmp") != std::string::npos ||
        lowerPath.find("temp") != std::string::npos) {
        confidence += 15;
    }
    
    // File integrity check
    if (access(path.c_str(), R_OK) == 0 && fileStat.st_size > 0) {
        confidence += 10;
    }
    
    return std::min(confidence, 100);
}

bool NativeScanner::isFileRecoverable(const std::string& path, const struct stat& fileStat) {
    // Check if file is accessible
    if (access(path.c_str(), R_OK) != 0) {
        return false;
    }
    
    // Check if file has content
    if (fileStat.st_size == 0) {
        return false;
    }
    
    // Check if file is in a recoverable location
    std::string lowerPath = path;
    std::transform(lowerPath.begin(), lowerPath.end(), lowerPath.begin(), ::tolower);
    
    std::vector<std::string> recoverableKeywords = {
        "cache", "tmp", "temp", "trash", "recycle", "deleted", ".thumbnails"
    };
    
    for (const auto& keyword : recoverableKeywords) {
        if (lowerPath.find(keyword) != std::string::npos) {
            return true;
        }
    }
    
    // Check if file was recently modified (potential for recovery)
    time_t now = time(nullptr);
    long daysSinceModified = (now - fileStat.st_mtime) / (24 * 60 * 60);
    
    return daysSinceModified < 30; // Files modified within 30 days
}

bool NativeScanner::shouldIncludeFile(const RecoveredFileInfo& fileInfo, const std::vector<int>& fileTypes) {
    if (fileTypes.empty()) {
        return true; // Include all types if no filter specified
    }
    
    return std::find(fileTypes.begin(), fileTypes.end(), fileInfo.fileType) != fileTypes.end();
}

bool NativeScanner::recoverFile(const RecoveredFileInfo& fileInfo, const std::string& outputPath) {
    LOGI("Recovering file: %s to %s", fileInfo.path.c_str(), outputPath.c_str());
    
    std::ifstream source(fileInfo.path, std::ios::binary);
    if (!source.is_open()) {
        LOGE("Failed to open source file: %s", fileInfo.path.c_str());
        return false;
    }
    
    std::ofstream dest(outputPath, std::ios::binary);
    if (!dest.is_open()) {
        LOGE("Failed to create destination file: %s", outputPath.c_str());
        return false;
    }
    
    dest << source.rdbuf();
    
    bool success = source.good() && dest.good();
    
    source.close();
    dest.close();
    
    if (success) {
        LOGI("Successfully recovered file: %s", fileInfo.name.c_str());
    } else {
        LOGE("Failed to recover file: %s", fileInfo.name.c_str());
    }
    
    return success;
}

void NativeScanner::stopScan() {
    m_shouldStop = true;
    LOGI("Scan stop requested");
}

bool NativeScanner::isRootAvailable() {
    return RootUtils::checkRootAccess();
}

std::vector<std::string> NativeScanner::getAvailablePartitions() {
    return DiskUtils::getAvailablePartitions();
}