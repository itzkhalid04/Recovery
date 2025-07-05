#ifndef SIGNATURE_DETECTOR_H
#define SIGNATURE_DETECTOR_H

#include <string>
#include <vector>
#include <cstdint>

class SignatureDetector {
public:
    SignatureDetector();
    ~SignatureDetector();

    int detectFileType(const std::string& filePath);
    int detectFileType(const uint8_t* data, size_t size);
    std::string getFileExtension(int fileType);
    bool isValidFileSignature(const uint8_t* data, size_t size, int expectedType);

private:
    struct FileSignature {
        std::vector<uint8_t> pattern;
        int fileType;
        std::string extension;
    };
    
    std::vector<FileSignature> m_signatures;
    
    void initializeSignatures();
    bool matchesPattern(const uint8_t* data, const std::vector<uint8_t>& pattern);
    int detectByExtension(const std::string& filePath);
};

#endif // SIGNATURE_DETECTOR_H