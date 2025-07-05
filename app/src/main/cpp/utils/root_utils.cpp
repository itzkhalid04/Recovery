#include "root_utils.h"
#include <android/log.h>
#include <unistd.h>
#include <sys/stat.h>
#include <cstdlib>
#include <fstream>
#include <vector>

#define LOG_TAG "RootUtils"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

bool RootUtils::checkRootAccess() {
    LOGI("Checking root access");
    
    // Check if we can get root privileges
    if (geteuid() == 0) {
        LOGI("Already running as root");
        return true;
    }
    
    // Check for su binaries and test access
    return isDeviceRooted() && testSuAccess();
}

bool RootUtils::executeRootCommand(const std::string& command, std::string& output) {
    if (!checkRootAccess()) {
        LOGE("No root access available");
        return false;
    }
    
    std::string suBinary = getSuBinary();
    if (suBinary.empty()) {
        LOGE("No su binary found");
        return false;
    }
    
    std::string fullCommand = suBinary + " -c \"" + command + "\"";
    
    FILE* pipe = popen(fullCommand.c_str(), "r");
    if (!pipe) {
        LOGE("Failed to execute root command: %s", command.c_str());
        return false;
    }
    
    char buffer[256];
    output.clear();
    
    while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
        output += buffer;
    }
    
    int result = pclose(pipe);
    
    if (result == 0) {
        LOGI("Root command executed successfully: %s", command.c_str());
        return true;
    } else {
        LOGE("Root command failed with code %d: %s", result, command.c_str());
        return false;
    }
}

bool RootUtils::isDeviceRooted() {
    return checkSuBinaries() || checkRootFiles();
}

std::string RootUtils::getSuBinary() {
    std::vector<std::string> suPaths = {
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su"
    };
    
    for (const auto& path : suPaths) {
        struct stat st;
        if (stat(path.c_str(), &st) == 0) {
            LOGI("Found su binary at: %s", path.c_str());
            return path;
        }
    }
    
    LOGI("No su binary found");
    return "";
}

bool RootUtils::testSuAccess() {
    std::string output;
    return executeRootCommand("id", output) && output.find("uid=0") != std::string::npos;
}

bool RootUtils::checkSuBinaries() {
    return !getSuBinary().empty();
}

bool RootUtils::checkRootFiles() {
    std::vector<std::string> rootFiles = {
        "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk",
        "/system/app/Kinguser.apk",
        "/data/data/eu.chainfire.supersu",
        "/data/data/com.noshufou.android.su",
        "/data/data/com.kingroot.kinguser"
    };
    
    for (const auto& file : rootFiles) {
        struct stat st;
        if (stat(file.c_str(), &st) == 0) {
            LOGI("Found root indicator file: %s", file.c_str());
            return true;
        }
    }
    
    return false;
}