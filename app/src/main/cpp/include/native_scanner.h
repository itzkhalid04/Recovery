#ifndef NATIVE_SCANNER_H
#define NATIVE_SCANNER_H

#include <jni.h>
#include <string>
#include <vector>
#include <memory>

struct RecoveredFileInfo {
    std::string name;
    std::string path;
    std::string originalPath;
    long long size;
    long long dateModified;
    long long dateDeleted;
    int fileType;
    int confidence;
    bool isDeleted;
    bool isRecoverable;
};

struct ScanProgress {
    int percentage;
    long long filesScanned;
    long long totalFiles;
    std::string currentFile;
    long long timeElapsed;
};

// Forward declarations
class FileSystemScanner;
class FileCarver;
class SignatureDetector;

class NativeScanner {
public:
    NativeScanner();
    ~NativeScanner();

    bool initialize(bool isRooted);
    std::vector<RecoveredFileInfo> startDeepScan(const std::string& partition, 
                                                 const std::vector<int>& fileTypes,
                                                 bool (*progressCallback)(const ScanProgress&));
    std::vector<RecoveredFileInfo> startQuickScan(const std::vector<int>& fileTypes,
                                                  bool (*progressCallback)(const ScanProgress&));
    bool recoverFile(const RecoveredFileInfo& fileInfo, const std::string& outputPath);
    void stopScan();
    bool isRootAvailable();
    std::vector<std::string> getAvailablePartitions();

private:
    bool m_isRooted;
    bool m_shouldStop;
    std::unique_ptr<FileSystemScanner> m_fsScanner;
    std::unique_ptr<FileCarver> m_fileCarver;
    std::unique_ptr<SignatureDetector> m_signatureDetector;
    
    // Private helper methods
    std::vector<RecoveredFileInfo> scanAccessibleAreas(const std::vector<int>& fileTypes,
                                                      bool (*progressCallback)(const ScanProgress&));
    std::vector<RecoveredFileInfo> scanDirectory(const std::string& path,
                                               const std::vector<int>& fileTypes,
                                               int maxDepth,
                                               int currentDepth = 0);
    RecoveredFileInfo analyzeFile(const std::string& path, const struct stat& fileStat);
    int calculateConfidence(const std::string& path, const struct stat& fileStat);
    bool isFileRecoverable(const std::string& path, const struct stat& fileStat);
    bool shouldIncludeFile(const RecoveredFileInfo& fileInfo, const std::vector<int>& fileTypes);
};

#endif // NATIVE_SCANNER_H