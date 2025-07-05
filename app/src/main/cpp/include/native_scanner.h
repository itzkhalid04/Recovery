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
    std::unique_ptr<class FileSystemScanner> m_fsScanner;
    std::unique_ptr<class FileCarver> m_fileCarver;
    std::unique_ptr<class SignatureDetector> m_signatureDetector;
};

#endif // NATIVE_SCANNER_H