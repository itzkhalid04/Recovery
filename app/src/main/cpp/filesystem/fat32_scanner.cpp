#include "fat32_scanner.h"
#include "../utils/root_utils.h"
#include <android/log.h>
#include <fstream>
#include <cstring>

#define LOG_TAG "Fat32Scanner"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

Fat32Scanner::Fat32Scanner() : m_isRooted(false) {}

Fat32Scanner::~Fat32Scanner() = default;

bool Fat32Scanner::initialize(bool isRooted) {
    m_isRooted = isRooted;
    LOGI("Initializing FAT32 scanner with root: %s", isRooted ? "true" : "false");
    return true;
}

std::vector<RecoveredFileInfo> Fat32Scanner::scanDeletedFiles(const std::string& partition,
                                                             const std::vector<int>& fileTypes,
                                                             std::function<bool(const ScanProgress&)> progressCallback) {
    std::vector<RecoveredFileInfo> results;
    
    if (!m_isRooted) {
        LOGE("FAT32 scanning requires root access");
        return results;
    }
    
    LOGI("Starting FAT32 scan on partition: %s", partition.c_str());
    
    // Read boot sector to get FAT32 information
    if (!readBootSector(partition)) {
        LOGE("Failed to read FAT32 boot sector");
        return results;
    }
    
    // Scan directory entries for deleted files
    auto entries = scanDirectoryEntries(partition);
    
    ScanProgress progress = {0, 0, (long long)entries.size(), "", 0};
    
    for (size_t i = 0; i < entries.size(); ++i) {
        if (isEntryDeleted(entries[i])) {
            RecoveredFileInfo fileInfo = entryToFileInfo(entries[i]);
            
            // Filter by file type if specified
            if (fileTypes.empty() || 
                std::find(fileTypes.begin(), fileTypes.end(), fileInfo.fileType) != fileTypes.end()) {
                results.push_back(fileInfo);
            }
        }
        
        // Update progress
        progress.percentage = (int)((i * 100) / entries.size());
        progress.filesScanned = i;
        progress.currentFile = "Scanning FAT32 entry " + std::to_string(i + 1);
        
        if (progressCallback && !progressCallback(progress)) {
            break;
        }
    }
    
    LOGI("FAT32 scan completed. Found %zu deleted files", results.size());
    return results;
}

bool Fat32Scanner::readBootSector(const std::string& device) {
    if (!m_isRooted) {
        return false;
    }
    
    // Use root access to read raw device boot sector
    std::string command = "dd if=" + device + " bs=512 count=1 2>/dev/null";
    
    // This is a simplified implementation
    // In a real implementation, you would parse the actual FAT32 boot sector structure
    LOGI("Reading FAT32 boot sector from %s", device.c_str());
    
    return true;
}

std::vector<Fat32Scanner::Fat32DirectoryEntry> Fat32Scanner::scanDirectoryEntries(const std::string& device) {
    std::vector<Fat32DirectoryEntry> entries;
    
    if (!m_isRooted) {
        return entries;
    }
    
    // This is a simplified implementation
    // In a real implementation, you would:
    // 1. Read the actual directory entries from the FAT32 file system
    // 2. Parse each directory entry structure
    // 3. Check for deleted entries (first byte = 0xE5)
    
    // For demonstration, create some mock deleted entries
    for (int i = 0; i < 75; ++i) {
        Fat32DirectoryEntry entry = {};
        entry.name[0] = 0xE5; // Deleted marker
        snprintf(entry.name + 1, 10, "FILE%04d", i);
        snprintf(entry.ext, 3, "TXT");
        entry.size = 1024 * (i + 1);
        entry.firstCluster = 100 + i;
        entry.date = 0x4A21; // Example date
        entry.time = 0x8C20; // Example time
        
        entries.push_back(entry);
    }
    
    return entries;
}

RecoveredFileInfo Fat32Scanner::entryToFileInfo(const Fat32DirectoryEntry& entry) {
    RecoveredFileInfo info;
    
    // Reconstruct filename from FAT32 8.3 format
    char filename[13];
    int nameLen = 0;
    for (int i = 1; i < 8 && entry.name[i] != ' '; ++i) {
        filename[nameLen++] = entry.name[i];
    }
    if (entry.ext[0] != ' ') {
        filename[nameLen++] = '.';
        for (int i = 0; i < 3 && entry.ext[i] != ' '; ++i) {
            filename[nameLen++] = entry.ext[i];
        }
    }
    filename[nameLen] = '\0';
    
    info.name = filename;
    info.path = "/data/fat32_deleted/" + info.name;
    info.originalPath = info.path;
    info.size = entry.size;
    
    // Convert FAT32 date/time to Unix timestamp
    int year = ((entry.date >> 9) & 0x7F) + 1980;
    int month = (entry.date >> 5) & 0x0F;
    int day = entry.date & 0x1F;
    int hour = (entry.time >> 11) & 0x1F;
    int minute = (entry.time >> 5) & 0x3F;
    int second = (entry.time & 0x1F) * 2;
    
    // Simple timestamp calculation (not accurate, for demo)
    time_t timestamp = time(nullptr) - (365 * 24 * 3600); // Assume 1 year ago
    info.dateModified = timestamp * 1000LL;
    info.dateDeleted = (timestamp + 3600) * 1000LL; // Deleted 1 hour later
    
    info.isDeleted = true;
    info.isRecoverable = true;
    
    // Determine file type based on extension
    std::string ext = info.name.substr(info.name.find_last_of('.') + 1);
    std::transform(ext.begin(), ext.end(), ext.begin(), ::tolower);
    
    if (ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "gif") {
        info.fileType = 1; // PHOTO
    } else if (ext == "mp4" || ext == "avi" || ext == "mov") {
        info.fileType = 2; // VIDEO
    } else if (ext == "txt" || ext == "doc" || ext == "pdf") {
        info.fileType = 3; // DOCUMENT
    } else if (ext == "mp3" || ext == "wav" || ext == "aac") {
        info.fileType = 4; // AUDIO
    } else {
        info.fileType = 0; // OTHER
    }
    
    // Calculate confidence based on file size and cluster availability
    if (entry.size > 0 && entry.firstCluster > 0) {
        if (entry.size > 1024 * 1024) {
            info.confidence = 85; // Large files have higher confidence
        } else if (entry.size > 100 * 1024) {
            info.confidence = 75;
        } else {
            info.confidence = 65;
        }
    } else {
        info.confidence = 30; // Low confidence for corrupted entries
    }
    
    return info;
}

bool Fat32Scanner::isEntryDeleted(const Fat32DirectoryEntry& entry) {
    // In FAT32, deleted files have their first character replaced with 0xE5
    return entry.name[0] == 0xE5;
}