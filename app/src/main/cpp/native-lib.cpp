#include <jni.h>
#include <string>
#include <unistd.h>
#include <sys/stat.h>
#include <cstdio>
#include <fcntl.h>
#include <cstring>

using u8 = unsigned char;
using s8 = char;

namespace RootUtil {

    bool fileExists(const char * const path) {
        struct stat buffer;
        return (stat(path, &buffer) == 0);
    }

#ifdef __ANDROID__
#include <sys/system_properties.h>
#endif

    bool checkRootMethod1() {
#ifdef __ANDROID__
        char value[PROP_VALUE_MAX] = {0};
        if (__system_property_get("ro.build.tags", value) > 0) {
            if (strstr(value, "test-keys") != nullptr) {
                return true;
            }
        }
#endif
        return false;
    }

    bool checkRootMethod2() {
        const char * const paths[10] {
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
        };

        for (size_t index{0}; index < \
                sizeof(paths) / sizeof(size_t); index++) {
            if (fileExists(paths[index])) return true;
        }
        return false;
    }

    bool checkRootMethod3() {
        FILE* pipe = popen("/system/xbin/which su", "r");
        if (!pipe) return false;

        s8 buffer[128];
        bool found = (fgets(buffer, sizeof(buffer), pipe) != nullptr);

        pclose(pipe);
        return found;
    }

    bool isDeviceRooted() {
        return checkRootMethod1() || \
            checkRootMethod2() || checkRootMethod3();
    }

}


extern "C" JNIEXPORT jstring JNICALL
Java_com_example_myapplication_MainActivity_cheakRoot(
        JNIEnv* env, jobject) {
    const char * const messageIsRoot{RootUtil::isDeviceRooted() ? \
        "You have ROOT!" : "You haven't root! :("};
    jstring result {nullptr};
    do {
        result = env->NewStringUTF(messageIsRoot);
    } while (result == nullptr);
    return result;
}

#include <sys/syscall.h>
#include <linux/random.h>

static ssize_t getrandom(void *buf, size_t buflen, unsigned int flags) {
    return syscall(SYS_getrandom, buf, buflen, flags);
}

template<typename Tp>
inline Tp getRandomBit() {
    Tp random;
    ssize_t result {getrandom(&random, sizeof(random), 0)};
    if (result == sizeof(random))
        return random & 1;

    static Tp _Randseed {static_cast<Tp>(time(nullptr) ^ getpid())};

    _Randseed = _Randseed * time(nullptr) + getpid();
    return _Randseed & 1;
}


extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_myapplication_MainActivity_playHeadsOrTails(
        JNIEnv* env, jobject) {
    return getRandomBit<jboolean>();
}

bool hasHardwareRNG() {
    struct stat buffer{};
    if (stat("/dev/hw_random", &buffer) == 0) {
        int fd = open("/dev/hw_random", O_RDONLY);
        if (fd < 0) return false;

        unsigned char a[4], b[4];
        if (read(fd, a, 4) != 4 ||
            read(fd, b, 4) != 4) {
            close(fd);
            return false;
        }
        close(fd);
        return memcmp(a, b, sizeof(a)) != 0;
    }
    return false;
}

bool canGetRandom() {
    u8 buf[4];
    ssize_t ret {getrandom(buf, sizeof(buf), 0)};
    if (ret == -1 && errno == ENOSYS)
        return false;
    return ret == sizeof(buf);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_myapplication_MainActivity_cheakRNG(
        JNIEnv* env, jobject) {
    bool isRNG{false};
    if (hasHardwareRNG()) isRNG = true;
    else isRNG = canGetRandom();
    return env->NewStringUTF(isRNG ? "You have RNG!" : "You haven't RNG! :(");
}