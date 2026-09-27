#include <jni.h>
#include <string>
#include <vector>
// 引入你的藍牙通訊協定標頭檔
#include "moto/ble_protocol/ble_protocol.hpp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_motonavtw_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "機車導航核心啟動！來自 C++ 引擎的問候！";
    return env->NewStringUTF(hello.c_str());
}

// 這是我們新加的魔法橋樑：把導航數據轉成 BLE Byte Array
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_motonavtw_MainActivity_encodeNavigationSnapshot(
        JNIEnv* env,
        jobject /* this */,
        jint speedKph,
        jstring roadName) {

    // 1. 將 Java/Kotlin 的字串轉換成 C++ 的字串
    const char *roadNameStr = env->GetStringUTFChars(roadName, nullptr);

    // 2. 建立一個導航快照物件 (NavigationSnapshot)
    moto::ble::NavigationSnapshot snapshot;

    // 將 Kotlin 傳來的速度 (km/h) 乘以 10 轉換成協定要求的 deci_kph (0.1 km/h)
    snapshot.speed_deci_kph = speedKph * 10;

    // 填入路名
    if (roadNameStr != nullptr) {
        snapshot.road_name = std::string(roadNameStr);
    }

    // 設定這份資料有「導航進行中」的標籤
    snapshot.flags = moto::ble::NavigationHasFix | moto::ble::NavigationHasDestination;

    // 釋放剛才取得的字串記憶體
    env->ReleaseStringUTFChars(roadName, roadNameStr);

    // 3. 呼叫你的核心函數，把 snapshot 轉換成藍牙用的 Bytes
    moto::ble::BytesResult result = moto::ble::encode_message(snapshot);

    // 4. 如果編碼成功，就把 C++ 的 Bytes 轉成 Kotlin 可以用的 ByteArray 傳回去
    if (result.ok()) {
        jbyteArray ret = env->NewByteArray(result.value.size());
        env->SetByteArrayRegion(ret, 0, result.value.size(), (const jbyte*)result.value.data());
        return ret;
    } else {
        // 如果編碼失敗（雖然機率很低），回傳一個空的陣列
        return env->NewByteArray(0);
    }
}