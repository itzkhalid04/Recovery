#ifndef FILE_CARVER_H
#define FILE_CARVER_H

#include "../include/native_scanner.h"
#include <string>
#include <vector>
#include <functional>

class FileCarver {
public:
    FileCarver();
    ~FileCarver();

    std::vector<RecoveredFileInfo> carveFiles(const std::string& partition,
                                             const std::vector<int>& fileTypes,
                                             std::function<bool(const ScanProgress&)> progressCallback);

private:
    struct FileSignature {
        std::vector<uint8_t> header;
        std::vector<uint8_t> footer;
        std::string extension;
        int fileType;
        size_t maxSize;
    };
    
    std::vector<FileSignature> m_signatures;
    
    void initializeSignatures();
    std::vector<RecoveredFileInfo> carveBySignature(const std::string& device,
                                                   const FileSignature& signature,
                                                   std::function<bool(const ScanProgress&)> progressCallback);
    bool matchesSignature(const uint8_t* data, const std::vector<uint8_t>& signature);
    RecoveredFileInfo createCarvedFileInfo(const std::string& path, size_t offset, size_t size, int fileType);
};

#endif // FILE_CARVER_H