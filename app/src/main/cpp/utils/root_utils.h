#ifndef ROOT_UTILS_H
#define ROOT_UTILS_H

#include <string>

class RootUtils {
public:
    static bool checkRootAccess();
    static bool executeRootCommand(const std::string& command, std::string& output);
    static bool isDeviceRooted();
    static std::string getSuBinary();

private:
    static bool testSuAccess();
    static bool checkSuBinaries();
    static bool checkRootFiles();
};

#endif // ROOT_UTILS_H