/**
 * Native Security Library for SecureKit Integrity Module.
 * 
 * Low-level security integrity checks using C++ Linux system calls.
 */

#include <jni.h>
#include <string>
#include <unistd.h>
#include <sys/stat.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <fcntl.h>
#include <fstream>
#include <cstring>
#include <vector>
#include <algorithm>

/**
 * Multi-byte compile-time XOR key to prevent single-byte automated reverse engineering.
 */
static const uint8_t MULTI_XOR_KEY[] = { 0xA5, 0x5F, 0x3C, 0x89, 0xD2, 0x71, 0xE4, 0x1B };
static const size_t MULTI_XOR_KEY_LEN = sizeof(MULTI_XOR_KEY);

/**
 * Securely decrypts obfuscated strings and zeroes out temporary memory.
 */
[[maybe_unused]] std::string decryptStringSecurely(const unsigned char* encryptedStr, size_t length) {
    std::vector<char> buffer(length);
    for (size_t i = 0; i < length; i++) {
        buffer[i] = static_cast<char>(encryptedStr[i] ^ MULTI_XOR_KEY[i % MULTI_XOR_KEY_LEN]);
    }
    std::string result(buffer.begin(), buffer.end());
    // Zero out memory buffer immediately
    std::fill(buffer.begin(), buffer.end(), 0);
    return result;
}

/**
 * Checks file existence via stat syscall.
 */
bool isFileExists(const char* path) {
    struct stat buffer;
    return (stat(path, &buffer) == 0);
}

/**
 * Native Root Detection via direct syscall checks on known binary paths.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_byan_securekit_integrity_NativeSecurityBridge_isDeviceRooted(JNIEnv* env, jobject /* this */) {
    const char* rootPaths[] = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/sbin/magisk",
            "/data/adb/magisk"
    };

    int numPaths = sizeof(rootPaths) / sizeof(rootPaths[0]);
    for (int i = 0; i < numPaths; ++i) {
        if (isFileExists(rootPaths[i])) {
            return JNI_TRUE;
        }
    }

    return JNI_FALSE;
}

/**
 * Anti-Hooking Frida Socket Probe using Non-blocking socket with 500ms timeout
 * to prevent ANR / blocking application execution.
 */
bool check_frida_sockets() {
    int ports[] = { 27042, 27043 };
    for (int i = 0; i < 2; i++) {
        int sock = socket(AF_INET, SOCK_STREAM, 0);
        if (sock < 0) {
            continue;
        }

        // Set socket to non-blocking mode
        int flags = fcntl(sock, F_GETFL, 0);
        fcntl(sock, F_SETFL, flags | O_NONBLOCK);

        struct sockaddr_in serv_addr;
        memset(&serv_addr, 0, sizeof(serv_addr));
        serv_addr.sin_family = AF_INET;
        serv_addr.sin_port = htons(ports[i]);
        inet_pton(AF_INET, "127.0.0.1", &serv_addr.sin_addr);

        int res = connect(sock, (struct sockaddr *)&serv_addr, sizeof(serv_addr));
        if (res < 0) {
            if (errno == EINPROGRESS) {
                fd_set wait_set;
                FD_ZERO(&wait_set);
                FD_SET(sock, &wait_set);

                // Timeout 500ms
                struct timeval tv;
                tv.tv_sec = 0;
                tv.tv_usec = 500000;

                res = select(sock + 1, NULL, &wait_set, NULL, &tv);
                if (res > 0) {
                    int so_error = 0;
                    socklen_t len = sizeof(so_error);
                    getsockopt(sock, SOL_SOCKET, SO_ERROR, &so_error, &len);
                    if (so_error == 0) {
                        close(sock);
                        return true; // Frida socket connected
                    }
                }
            }
        } else {
            close(sock);
            return true;
        }
        close(sock);
    }
    return false;
}

/**
 * Scans /proc/self/maps for injected hooks (Frida, Xposed, Substrate, LSPosed, EdXposed).
 */
bool check_memory_maps() {
    std::ifstream file("/proc/self/maps");
    if (!file.is_open()) {
        return false;
    }

    std::string line;
    while (std::getline(file, line)) {
        if (line.find("frida") != std::string::npos ||
            line.find("xposed") != std::string::npos ||
            line.find("substrate") != std::string::npos ||
            line.find("lsposed") != std::string::npos ||
            line.find("edxposed") != std::string::npos) {
            return true;
        }
    }
    return false;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_byan_securekit_integrity_NativeSecurityBridge_isHookingDetected(JNIEnv* env, jobject /* this */) {
    if (check_frida_sockets() || check_memory_maps()) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

/**
 * Scans mount options in /proc/self/mounts for RW remounts on critical partitions.
 */
bool check_mount_status() {
    std::ifstream mounts("/proc/self/mounts");
    if (!mounts.is_open()) return false;

    std::string line;
    const char* criticalPaths[] = { "/system", "/vendor", "/system_ext", "/product" };
    int numCritical = sizeof(criticalPaths) / sizeof(criticalPaths[0]);

    while (std::getline(mounts, line)) {
        for (int i = 0; i < numCritical; i++) {
            if (line.find(criticalPaths[i]) != std::string::npos) {
                if (line.find(" rw,") != std::string::npos || 
                    line.find(",rw,") != std::string::npos || 
                    line.find(",rw ") != std::string::npos ||
                    line.find(" rw ") != std::string::npos) {
                    return true;
                }
            }
        }
    }
    return false;
}

/**
 * Probes for Magisk artifacts using access() syscall.
 */
bool check_magisk_artifacts() {
    const char* magiskPaths[] = {
        "/sbin/.magisk",
        "/data/adb/magisk.db",
        "/data/adb/magisk",
        "/data/adb/modules",
        "/cache/magisk.log"
    };

    for (const char* path : magiskPaths) {
        if (access(path, F_OK) == 0) {
            return true;
        }
    }
    return false;
}

/**
 * Native Emulator Device Node probing (/dev/qemu_pipe, /dev/goldfish_pipe, etc.)
 */
bool check_emulator_device_nodes() {
    const char* emuNodes[] = {
        "/dev/qemu_pipe",
        "/dev/goldfish_pipe",
        "/dev/socket/qemud",
        "/dev/qemu_trace"
    };

    for (const char* node : emuNodes) {
        if (access(node, F_OK) == 0) {
            return true;
        }
    }
    return false;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_byan_securekit_integrity_NativeSecurityBridge_isAdvancedRootDetected(JNIEnv* env, jobject /* this */) {
    if (check_mount_status() || check_magisk_artifacts()) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_byan_securekit_integrity_NativeSecurityBridge_isNativeEmulatorDetected(JNIEnv* env, jobject /* this */) {
    if (check_emulator_device_nodes()) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

