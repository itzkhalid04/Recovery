#include "ext4_scanner.h"
#include "../utils/root_utils.h"
#include <android/log.h>
#include <fstream>
#include <cstring>

#define LOG_TAG "Ext4Scanner"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

Ext4Scanner::Ext4Scanner() : m_isRooted(false) {}

Ext4Scanner::~Ext4Scanner() = default;

bool Ext4Scanner::initialize(bool isRooted) {
    m_isRooted = isRooted;
    LOGI("Initializing EXT4 scanner with root: %s", isRooted ? "true" : "false");
    return true;
}

std::vector<RecoveredFileInfo> Ext4Scanner::scanDeletedFiles(const std::string& partition,
                                                            const std::vector<int>& fileTypes,
                                                            std::function<bool(const ScanProgress&)> progressCallback) {
    std::vector<RecoveredFileInfo> results;
    
    if (!m_isRooted) {
        LOGE("EXT4 scanning requires root access");
        return results;
    }
    
    LOGI("Starting EXT4 scan on partition: %s", partition.c_str());
    
    // Read superblock to get file system information
    if (!readSuperblock(partition)) {
        LOGE("Failed to read EXT4 superblock");
        return results;
    }
    
    // Scan inode table for deleted files
    auto inodes = scanInodeTable(partition);
    
    ScanProgress progress = {0, 0, (long long)inodes.size(), "", 0};
    
    for (size_t i = 0; i < inodes.size(); ++i) {
        if (isInodeDeleted(inodes[i])) {
            RecoveredFileInfo fileInfo = inodeToFileInfo(inodes[i], i + 1);
            
            // Filter by file type if specified
            if (fileTypes.empty() || 
                std::find(fileTypes.begin(), fileTypes.end(), fileInfo.fileType) != fileTypes.end()) {
                results.push_back(fileInfo);
            }
        }
        
        // Update progress
        progress.percentage = (int)((i * 100) / inodes.size());
        progress.filesScanned = i;
        progress.currentFile = "Scanning inode " + std::to_string(i + 1);
        
        if (progressCallback && !progressCallback(progress)) {
            break;
        }
    }
    
    LOGI("EXT4 scan completed. Found %zu deleted files", results.size());
    return results;
}

bool Ext4Scanner::readSuperblock(const std::string& device) {
    if (!m_isRooted) {
        return false;
    }
    
    // Use root access to read raw device
    std::string command = "dd if=" + device + " bs=1024 skip=1 count=1 2>/dev/null";
    
    // This is a simplified implementation
    // In a real implementation, you would parse the actual EXT4 superblock structure
    LOGI("Reading EXT4 superblock from %s", device.c_str());
    
    return true;
}

std::vector<Ext4Scanner::Ext4Inode> Ext4Scanner::scanInodeTable(const std::string& device) {
    std::vector<Ext4Inode> inodes;
    
    if (!m_isRooted) {
        return inodes;
    }
    
    // This is a simplified implementation
    // In a real implementation, you would:
    // 1. Read the actual inode table from the raw device
    // 2. Parse each inode structure
    // 3. Check for deleted inodes (dtime != 0)
    
    // For demonstration, create some mock deleted inodes
    for (int i = 0; i < 100; ++i) {
        Ext4Inode inode = {};
        inode.mode = 0x8000; // Regular file
        inode.size = 1024 * (i + 1);
        inode.mtime = time(nullptr) - (i * 3600); // Modified hours ago
        inode.dtime = time(nullptr) - (i * 1800); // Deleted 30 minutes after modification
        
        inodes.push_back(inode);
    }
    
    return inodes;
}

RecoveredFileInfo Ext4Scanner::inodeToFileInfo(const Ext4Inode& inode, uint32_t inodeNumber) {
    RecoveredFileInfo info;
    
    info.name = "deleted_file_" + std::to_string(inodeNumber);
    info.path = "/data/deleted/" + info.name;
    info.originalPath = info.path;
    info.size = inode.size;
    info.dateModified = inode.mtime * 1000LL;
    info.dateDeleted = inode.dtime * 1000LL;
    info.isDeleted = true;
    info.isRecoverable = true;
    
    // Determine file type based on size and other heuristics
    if (info.size > 1024 * 1024) {
        info.fileType = 2; // VIDEO
        info.name += ".mp4";
    } else if (info.size > 100 * 1024) {
        info.fileType = 1; // PHOTO
        info.name += ".jpg";
    } else {
        info.fileType = 3; // DOCUMENT
        info.name += ".txt";
    }
    
    // Calculate confidence based on deletion time
    time_t now = time(nullptr);
    long hoursSinceDeletion = (now - inode.dtime) / 3600;
    
    if (hoursSinceDeletion < 24) {
        info.confidence = 90;
    } else if (hoursSinceDeletion < 168) { // 1 week
        info.confidence = 75;
    } else if (hoursSinceDeletion < 720) { // 1 month
        info.confidence = 60;
    } else {
        info.confidence = 30;
    }
    
    return info;
}

bool Ext4Scanner::isInodeDeleted(const Ext4Inode& inode) {
    // An inode is considered deleted if:
    // 1. It has a deletion time (dtime != 0)
    // 2. It was a regular file (mode indicates file type)
    return inode.dtime != 0 && (inode.mode & 0xF000) == 0x8000;
}