#include "disk_utils.h"
#include <android/log.h>
#include <fstream>
#include <sstream>
#include <sys/statfs.h>
#include <sys/mount.h>

#define LOG_TAG "DiskUtils"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

std::string DiskUtils::getFileSystemType(const std::string& path) {
    std::ifstream mounts("/proc/mounts");
    std::string line;
    
    while (std::getline(mounts, line)) {
        std::istringstream iss(line);
        std::string device, mountPoint, fsType;
        
        if (iss >> device >> mountPoint >> fsType) {
            if (path.find(mountPoint) == 0) {
                LOGI("File system type for %s: %s", path.c_str(), fsType.c_str());
                return fsType;
            }
        }
    }
    
    LOGI("Unknown file system type for %s, defaulting to ext4", path.c_str());
    return "ext4";
}

std::vector<std::string> DiskUtils::getAvailablePartitions() {
    std::vector<std::string> partitions;
    
    std::ifstream partitions_file("/proc/partitions");
    std::string line;
    
    // Skip header
    std::getline(partitions_file, line);
    std::getline(partitions_file, line);
    
    while (std::getline(partitions_file, line)) {
        std::istringstream iss(line);
        std::string major, minor, blocks, name;
        
        if (iss >> major >> minor >> blocks >> name) {
            if (!name.empty() && name.find("loop") == std::string::npos) {
                std::string partition = "/dev/block/" + name;
                partitions.push_back(partition);
                LOGI("Found partition: %s", partition.c_str());
            }
        }
    }
    
    return partitions;
}

std::vector<std::string> DiskUtils::getMountPoints() {
    std::vector<std::string> mountPoints;
    
    std::ifstream mounts("/proc/mounts");
    std::string line;
    
    while (std::getline(mounts, line)) {
        std::istringstream iss(line);
        std::string device, mountPoint, fsType;
        
        if (iss >> device >> mountPoint >> fsType) {
            if (mountPoint != "/" && mountPoint.find("/proc") != 0 && 
                mountPoint.find("/sys") != 0 && mountPoint.find("/dev") != 0) {
                mountPoints.push_back(mountPoint);
                LOGI("Found mount point: %s (%s)", mountPoint.c_str(), fsType.c_str());
            }
        }
    }
    
    return mountPoints;
}

bool DiskUtils::isPartitionMounted(const std::string& partition) {
    std::ifstream mounts("/proc/mounts");
    std::string line;
    
    while (std::getline(mounts, line)) {
        if (line.find(partition) != std::string::npos) {
            return true;
        }
    }
    
    return false;
}

long long DiskUtils::getPartitionSize(const std::string& partition) {
    struct statfs stat;
    
    if (statfs(partition.c_str(), &stat) == 0) {
        return (long long)stat.f_blocks * stat.f_bsize;
    }
    
    return 0;
}

long long DiskUtils::getFreeSpace(const std::string& path) {
    struct statfs stat;
    
    if (statfs(path.c_str(), &stat) == 0) {
        return (long long)stat.f_bavail * stat.f_bsize;
    }
    
    return 0;
}

std::string DiskUtils::readMountInfo() {
    std::ifstream file("/proc/mounts");
    std::string content;
    std::string line;
    
    while (std::getline(file, line)) {
        content += line + "\n";
    }
    
    return content;
}

std::string DiskUtils::readPartitionInfo() {
    std::ifstream file("/proc/partitions");
    std::string content;
    std::string line;
    
    while (std::getline(file, line)) {
        content += line + "\n";
    }
    
    return content;
}