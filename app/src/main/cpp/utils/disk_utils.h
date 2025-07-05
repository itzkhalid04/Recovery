#ifndef DISK_UTILS_H
#define DISK_UTILS_H

#include <string>
#include <vector>

class DiskUtils {
public:
    static std::string getFileSystemType(const std::string& path);
    static std::vector<std::string> getAvailablePartitions();
    static std::vector<std::string> getMountPoints();
    static bool isPartitionMounted(const std::string& partition);
    static long long getPartitionSize(const std::string& partition);
    static long long getFreeSpace(const std::string& path);

private:
    static std::string readMountInfo();
    static std::string readPartitionInfo();
};

#endif // DISK_UTILS_H