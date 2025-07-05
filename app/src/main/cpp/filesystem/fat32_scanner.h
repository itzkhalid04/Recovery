#ifndef FAT32_SCANNER_H
#define FAT32_SCANNER_H

#include "../include/native_scanner.h"
#include <string>
#include <vector>
#include <functional>

class Fat32Scanner {
public:
    Fat32Scanner();
    ~Fat32Scanner();

    bool initialize(bool isRooted);
    std::vector<RecoveredFileInfo> scanDeletedFiles(const std::string& partition,
                                                   const std::vector<int>& fileTypes,
                                                   std::function<bool(const ScanProgress&)> progressCallback);

private:
    bool m_isRooted;
    
    struct Fat32DirectoryEntry {
        char name[8];       // 8-character filename
        char ext[3];        // 3-character extension
        uint8_t attr;       // File attributes
        uint8_t reserved;   // Reserved
        uint8_t createTimeTenth; // Creation time (tenths of second)
        uint16_t createTime;     // Creation time
        uint16_t createDate;     // Creation date
        uint16_t accessDate;     // Last access date
        uint16_t firstClusterHigh; // High 16 bits of first cluster
        uint16_t time;      // Last write time
        uint16_t date;      // Last write date
        uint16_t firstCluster; // Low 16 bits of first cluster
        uint32_t size;      // File size in bytes
    };
    
    bool readBootSector(const std::string& device);
    std::vector<Fat32DirectoryEntry> scanDirectoryEntries(const std::string& device);
    RecoveredFileInfo entryToFileInfo(const Fat32DirectoryEntry& entry);
    bool isEntryDeleted(const Fat32DirectoryEntry& entry);
};

#endif // FAT32_SCANNER_H