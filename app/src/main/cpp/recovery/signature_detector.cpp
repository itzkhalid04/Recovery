#include "signature_detector.h"
#include <android/log.h>
#include <fstream>
#include <algorithm>

#define LOG_TAG "SignatureDetector"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

SignatureDetector::SignatureDetector() {
    initializeSignatures();
}

SignatureDetector::~SignatureDetector() = default;

void SignatureDetector::initializeSignatures() {
    // JPEG signatures
    m_signatures.push_back({
        {0xFF, 0xD8, 0xFF}, // JPEG header
        1, // PHOTO
        "jpg"
    });
    
    // PNG signatures
    m_signatures.push_back({
        {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, // PNG header
        1, // PHOTO
        "png"
    });
    
    // GIF signatures
    m_signatures.push_back({
        {0x47, 0x49, 0x46, 0x38}, // GIF87a or GIF89a
        1, // PHOTO
        "gif"
    });
    
    // MP4 signatures
    m_signatures.push_back({
        {0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70}, // MP4 ftyp box
        2, // VIDEO
        "mp4"
    });
    
    // AVI signatures
    m_signatures.push_back({
        {0x52, 0x49, 0x46, 0x46}, // RIFF header (AVI)
        2, // VIDEO
        "avi"
    });
    
    // MP3 signatures
    m_signatures.push_back({
        {0xFF, 0xFB}, // MP3 frame header
        4, // AUDIO
        "mp3"
    });
    
    // WAV signatures
    m_signatures.push_back({
        {0x52, 0x49, 0x46, 0x46}, // RIFF header (WAV)
        4, // AUDIO
        "wav"
    });
    
    // PDF signatures
    m_signatures.push_back({
        {0x25, 0x50, 0x44, 0x46}, // %PDF
        3, // DOCUMENT
        "pdf"
    });
    
    // ZIP signatures
    m_signatures.push_back({
        {0x50, 0x4B, 0x03, 0x04}, // ZIP local file header
        5, // ARCHIVE
        "zip"
    });
    
    // APK signatures (same as ZIP)
    m_signatures.push_back({
        {0x50, 0x4B, 0x03, 0x04}, // APK is ZIP format
        6, // APK
        "apk"
    });
    
    LOGI("Initialized %zu file signatures", m_signatures.size());
}

int SignatureDetector::detectFileType(const std::string& filePath) {
    std::ifstream file(filePath, std::ios::binary);
    if (!file.is_open()) {
        return 0; // OTHER
    }
    
    // Read first 16 bytes for signature detection
    uint8_t buffer[16];
    file.read(reinterpret_cast<char*>(buffer), sizeof(buffer));
    size_t bytesRead = file.gcount();
    file.close();
    
    if (bytesRead == 0) {
        return 0; // OTHER
    }
    
    // Check against known signatures
    for (const auto& signature : m_signatures) {
        if (bytesRead >= signature.pattern.size()) {
            if (matchesPattern(buffer, signature.pattern)) {
                // Additional validation for some formats
                if (signature.extension == "wav" || signature.extension == "avi") {
                    // Check for specific format indicators
                    if (bytesRead >= 12) {
                        if (signature.extension == "wav" && 
                            buffer[8] == 'W' && buffer[9] == 'A' && buffer[10] == 'V' && buffer[11] == 'E') {
                            return signature.fileType;
                        } else if (signature.extension == "avi" && 
                                   buffer[8] == 'A' && buffer[9] == 'V' && buffer[10] == 'I' && buffer[11] == ' ') {
                            return signature.fileType;
                        }
                    }
                } else {
                    return signature.fileType;
                }
            }
        }
    }
    
    // Fallback to extension-based detection
    return detectByExtension(filePath);
}

int SignatureDetector::detectFileType(const uint8_t* data, size_t size) {
    if (!data || size == 0) {
        return 0; // OTHER
    }
    
    // Check against known signatures
    for (const auto& signature : m_signatures) {
        if (size >= signature.pattern.size()) {
            if (matchesPattern(data, signature.pattern)) {
                // Additional validation for some formats
                if (signature.extension == "wav" || signature.extension == "avi") {
                    if (size >= 12) {
                        if (signature.extension == "wav" && 
                            data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E') {
                            return signature.fileType;
                        } else if (signature.extension == "avi" && 
                                   data[8] == 'A' && data[9] == 'V' && data[10] == 'I' && data[11] == ' ') {
                            return signature.fileType;
                        }
                    }
                } else {
                    return signature.fileType;
                }
            }
        }
    }
    
    return 0; // OTHER
}

bool SignatureDetector::matchesPattern(const uint8_t* data, const std::vector<uint8_t>& pattern) {
    for (size_t i = 0; i < pattern.size(); ++i) {
        if (data[i] != pattern[i]) {
            return false;
        }
    }
    return true;
}

int SignatureDetector::detectByExtension(const std::string& filePath) {
    size_t dotPos = filePath.find_last_of('.');
    if (dotPos == std::string::npos) {
        return 0; // OTHER
    }
    
    std::string extension = filePath.substr(dotPos + 1);
    std::transform(extension.begin(), extension.end(), extension.begin(), ::tolower);
    
    // Photo extensions
    if (extension == "jpg" || extension == "jpeg" || extension == "png" || 
        extension == "gif" || extension == "bmp" || extension == "webp" || 
        extension == "heic" || extension == "tiff") {
        return 1; // PHOTO
    }
    
    // Video extensions
    if (extension == "mp4" || extension == "avi" || extension == "mov" || 
        extension == "mkv" || extension == "3gp" || extension == "flv" || 
        extension == "wmv" || extension == "webm") {
        return 2; // VIDEO
    }
    
    // Document extensions
    if (extension == "pdf" || extension == "doc" || extension == "docx" || 
        extension == "xls" || extension == "xlsx" || extension == "ppt" || 
        extension == "pptx" || extension == "txt" || extension == "rtf") {
        return 3; // DOCUMENT
    }
    
    // Audio extensions
    if (extension == "mp3" || extension == "wav" || extension == "aac" || 
        extension == "flac" || extension == "ogg" || extension == "m4a" || 
        extension == "wma") {
        return 4; // AUDIO
    }
    
    // Archive extensions
    if (extension == "zip" || extension == "rar" || extension == "7z" || 
        extension == "tar" || extension == "gz" || extension == "bz2") {
        return 5; // ARCHIVE
    }
    
    // APK extension
    if (extension == "apk") {
        return 6; // APK
    }
    
    return 0; // OTHER
}

std::string SignatureDetector::getFileExtension(int fileType) {
    switch (fileType) {
        case 1: return "jpg";    // PHOTO
        case 2: return "mp4";    // VIDEO
        case 3: return "pdf";    // DOCUMENT
        case 4: return "mp3";    // AUDIO
        case 5: return "zip";    // ARCHIVE
        case 6: return "apk";    // APK
        default: return "bin";   // OTHER
    }
}

bool SignatureDetector::isValidFileSignature(const uint8_t* data, size_t size, int expectedType) {
    int detectedType = detectFileType(data, size);
    return detectedType == expectedType;
}