#ifndef EXT4_SCANNER_H
#define EXT4_SCANNER_H

#include "../include/native_scanner.h"
#include <string>
#include <vector>
#include <functional>

class Ext4Scanner {
public:
    Ext4Scanner();
    ~Ext4Scanner();

    bool initialize(bool isRooted);
    std::vector<RecoveredFileInfo> scanDeletedFiles(const std::string& partition,
                                                   const std::vector<int>& fileTypes,
                                                   std::function<bool(const ScanProgress&)> progressCallback);

private:
    bool m_isRooted;
    
    struct Ext4Inode {
        uint32_t mode;
        uint32_t size;
        uint32_t atime;
        uint32_t mtime;
        uint32_t dtime; // Deletion time
        uint32_t blocks[15];
    };
    
    bool readSuperblock(const std::string& device);
    std::vector<Ext4Inode> scanInodeTable(const std::string& device);
    RecoveredFileInfo inodeToFileInfo(const Ext4Inode& inode, uint32_t inodeNumber);
    bool isInodeDeleted(const Ext4Inode& inode);
};

#endif // EXT4_SCANNER_H