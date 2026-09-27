#include <jni.h>
#include <string>
#include <vector>

extern "C" {

JNIEXPORT jbyteArray JNICALL
Java_com_example_motonavtw_MainActivity_encodeNavigationSnapshot(
        JNIEnv *env,
        jobject /* thiz */,
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

    // 建立藍牙封包二進位資料
    std::vector<uint8_t> packet;
    packet.push_back(0xB7); // 協議 Magic Header
    packet.push_back(0x01); // 協議版本 1
    packet.push_back(0x10); // 導航快照訊息類型
    packet.push_back(0x00); // 旗標

    // 填入導航數據
    packet.push_back(static_cast<uint8_t>(speed_kph & 0xFF));
    packet.push_back(static_cast<uint8_t>(turn_type & 0xFF));
    packet.push_back(static_cast<uint8_t>((distance_meters >> 8) & 0xFF));
    packet.push_back(static_cast<uint8_t>(distance_meters & 0xFF));

    // 填入路名字節
    for (char c : roadStr) {
        packet.push_back(static_cast<uint8_t>(c));
    }

    // 轉換並回傳給 Kotlin ByteArray
    jsize len = static_cast<jsize>(packet.size());
    jbyteArray resultArray = env->NewByteArray(len);
    if (resultArray != nullptr && len > 0) {
        env->SetByteArrayRegion(resultArray, 0, len, reinterpret_cast<const jbyte*>(packet.data()));
    }

    return resultArray;
}

}