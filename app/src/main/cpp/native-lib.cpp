#include <jni.h>
#include <string>
#include <vector>

extern "C" {

JNIEXPORT jbyteArray JNICALL
Java_com_example_motonavtw_MainActivity_encodeNavigationSnapshot(
        JNIEnv *env,
        jobject thiz,
        jint speed_kph,
        jstring road_name,
        jint turn_type,
        jint distance_meters
) {
    // 取得路名字串
    const char *nativeRoadName = env->GetStringUTFChars(road_name, JNI_FALSE);
    std::string roadStr(nativeRoadName ? nativeRoadName : "");
    if (nativeRoadName) {
        env->ReleaseStringUTFChars(road_name, nativeRoadName);
    }

    // 建立封包資料
    std::vector<uint8_t> packet;
    packet.push_back(0x01);
    packet.push_back(static_cast<uint8_t>(speed_kph & 0xFF));
    packet.push_back(static_cast<uint8_t>(turn_type & 0xFF));
    packet.push_back(static_cast<uint8_t>((distance_meters >> 8) & 0xFF));
    packet.push_back(static_cast<uint8_t>(distance_meters & 0xFF));

    for (char c : roadStr) {
        packet.push_back(static_cast<uint8_t>(c));
    }

    // 轉換並回傳給 Kotlin
    jsize len = static_cast<jsize>(packet.size());
    jbyteArray resultArray = env->NewByteArray(len);
    if (resultArray != nullptr && len > 0) {
        env->SetByteArrayRegion(resultArray, 0, len, reinterpret_cast<const jbyte*>(packet.data()));
    }

    return resultArray;
}

}