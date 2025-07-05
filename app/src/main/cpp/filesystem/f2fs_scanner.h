#ifndef F2FS_SCANNER_H
#define F2FS_SCANNER_H

#include "../include/native_scanner.h"
#include <string>
#include <vector>
#include <functional>

class F2fsScanner {
public:
    F2fsScanner();
    ~F2fsScanner();

    bool initialize(bool isRooted);
    std::vector<RecoveredFileInfo> scanDeletedFiles(const std::string& partition,
                                                   const std::vector<int>& fileTypes,
                                                   std::function<bool(const ScanProgress&)> progressCallback);

private:
    bool m_isRooted;
    
    struct F2fsNode {
        uint32_t nid;
        uint32_t ino;
        uint32_t flag;
        uint64_t size;
        uint32_t blocks;
        uint64_t atime;
        uint64_t mtime;
        uint64_t ctime;
    };
    
    bool readCheckpoint(const std::string& device);
    std::vector<F2fsNode> scanNodeArea(const std::string& device);
    RecoveredFileInfo nodeToFileInfo(const F2fsNode& node);
    bool isNodeDeleted(const F2fsNode& node);
};

#endif // F2FS_SCANNER_H