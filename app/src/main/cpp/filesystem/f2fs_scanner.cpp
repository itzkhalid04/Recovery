#include "f2fs_scanner.h"
#include "../utils/root_utils.h"
#include <android/log.h>
#include <ctime>
#include <algorithm>

#define LOG_TAG "F2fsScanner"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

F2fsScanner::F2fsScanner() : m_isRooted(false) {}

F2fsScanner::~F2fsScanner() = default;

bool F2fsScanner::initialize(bool isRooted) {
    m_isRooted = isRooted;
    LOGI("Initializing F2FS scanner with root: %s", isRooted ? "true" : "false");
    return true;
}

std::vector<RecoveredFileInfo> F2fsScanner::scanDeletedFiles(const std::string& partition,
                                                            const std::vector<int>& fileTypes,
                                                            std::function<bool(const ScanProgress&)> progressCallback) {
    std::vector<RecoveredFileInfo> results;
    
    if (!m_isRooted) {
        LOGE("F2FS scanning requires root access");
        return results;
    }
    
    LOGI("Starting F2FS scan on partition: %s", partition.c_str());
    
    if (!readCheckpoint(partition)) {
        LOGE("Failed to read F2FS checkpoint");
        return results;
    }
    
    auto nodes = scanNodeArea(partition);
    
    ScanProgress progress = {0, 0, (long long)nodes.size(), "", 0};
    
    for (size_t i = 0; i < nodes.size(); ++i) {
        if (isNodeDeleted(nodes[i])) {
            RecoveredFileInfo fileInfo = nodeToFileInfo(nodes[i]);
            
            if (fileTypes.empty() || 
                std::find(fileTypes.begin(), fileTypes.end(), fileInfo.fileType) != fileTypes.end()) {
                results.push_back(fileInfo);
            }
        }
        
        progress.percentage = (int)((i * 100) / nodes.size());
        progress.filesScanned = i;
        progress.currentFile = "Scanning F2FS node " + std::to_string(nodes[i].nid);
        
        if (progressCallback && !progressCallback(progress)) {
            break;
        }
    }
    
    LOGI("F2FS scan completed. Found %zu deleted files", results.size());
    return results;
}

bool F2fsScanner::readCheckpoint(const std::string& device) {
    // F2FS checkpoint reading implementation
    LOGI("Reading F2FS checkpoint from %s", device.c_str());
    return true;
}

std::vector<F2fsScanner::F2fsNode> F2fsScanner::scanNodeArea(const std::string&) {
    std::vector<F2fsNode> nodes;
    
    // Mock F2FS nodes for demonstration
    for (int i = 0; i < 50; ++i) {
        F2fsNode node = {};
        node.nid = i + 1000;
        node.ino = i + 1;
        node.flag = 0x1; // Deleted flag
        node.size = 2048 * (i + 1);
        node.mtime = time(nullptr) - (i * 7200); // Modified hours ago
        node.ctime = time(nullptr) - (i * 3600); // Changed hours ago
        
        nodes.push_back(node);
    }
    
    return nodes;
}

RecoveredFileInfo F2fsScanner::nodeToFileInfo(const F2fsNode& node) {
    RecoveredFileInfo info;
    
    info.name = "f2fs_deleted_" + std::to_string(node.nid);
    info.path = "/data/f2fs_deleted/" + info.name;
    info.originalPath = info.path;
    info.size = node.size;
    info.dateModified = node.mtime * 1000LL;
    info.dateDeleted = node.ctime * 1000LL;
    info.isDeleted = true;
    info.isRecoverable = true;
    
    // Determine file type
    if (node.size > 5 * 1024 * 1024) {
        info.fileType = 2; // VIDEO
        info.name += ".mp4";
    } else if (node.size > 500 * 1024) {
        info.fileType = 1; // PHOTO
        info.name += ".jpg";
    } else {
        info.fileType = 4; // AUDIO
        info.name += ".mp3";
    }
    
    // Calculate confidence
    time_t now = time(nullptr);
    long hoursSinceDeletion = (now - node.ctime) / 3600;
    
    if (hoursSinceDeletion < 12) {
        info.confidence = 95;
    } else if (hoursSinceDeletion < 72) {
        info.confidence = 80;
    } else if (hoursSinceDeletion < 336) { // 2 weeks
        info.confidence = 65;
    } else {
        info.confidence = 40;
    }
    
    return info;
}

bool F2fsScanner::isNodeDeleted(const F2fsNode& node) {
    // Check if node is marked as deleted
    return (node.flag & 0x1) != 0;
}