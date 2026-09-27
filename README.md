# 🏍️ MotoNavTW：台灣專屬重機/自行車智能導航儀

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![C++](https://img.shields.io/badge/C++-00599C?style=for-the-badge&logo=c%2B%2B&logoColor=white)
![Bluetooth](https://img.shields.io/badge/BLE-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white)

**MotoNavTW** 是一款專為台灣騎士（重機與自行車）量身打造的 Android 導航伴手應用程式 (Companion App)。
本專案與 [微雪 (Waveshare) ESP32-S3 1.75 吋 AMOLED 圓形開發板](https://www.waveshare.net/) 硬體深度整合，將你的 Android 手機化身為強大的導航大腦，透過低功耗藍牙 (BLE) 將畫面與指令投射到車把上的微型儀表板。

---

## ✨ 核心特色與台灣在地化功能

*   🇹🇼 **專屬路權算路引擎**：深度整合 Google Routes API，精準避開禁行機車的國道與快速道路，為不同白/黃/紅牌級距機車規劃最佳路線。
*   📸 **測速照相語音預警**：內建台灣測速照相圖資。考量騎乘風切聲，**警示語音將透過手機藍牙直接傳送至騎士的安全帽耳機 (如 Cardo/Sena)**，確保提示清晰不漏接。
*   🗺️ **無網域離線圖資 (OSM)**：支援 OpenStreetMap 離線瓦片地圖快取，即使騎乘至訊號不佳的深山林道，依然能保持精準導航。
*   🔋 **極致省電與 AMOLED 絕佳對比**：手機端負責重度運算，ESP32-S3 僅作終端顯示。配合 1.75 吋 AMOLED 螢幕「純黑不發光」的特性，儀表板 UI 在烈日下依然清晰且極度省電。

---

## 🛠️ 系統架構與技術棧

本專案採用 **Kotlin (UI 與系統通訊) + Native C++ (核心演算法)** 的混合架構，確保跨平台協定的高效與一致性。

*   **前端介面 (Android)**：`Kotlin` + `Jetpack Compose` (建構現代化且流暢的手機端設定介面)。
*   **導航與通訊核心 (JNI)**：`C++17` + `CMake`。直接移植並封裝開源的導航通訊協定，處理 CRC 校驗、封包分片與座標轉換。
*   **硬體通訊**：Android Bluetooth Low Energy (BLE) API。

---

## 📱 硬體需求與建議

### 1. 儀表板硬體 (強烈建議規格)
為了與本專案的底層顯示協定與 UI 佈局完美相容，請務必選購以下硬體：
*   **型號**：ESP32-S3 1.75 寸 AMOLED 开发板 C型
*   **解析度**：466 × 466 pixels (純黑底色最佳化)
*   **備註**：*請勿購買 1.85 吋 LCD 版本，其底層顯示驅動器與本專案不相容，會導致畫面黑屏或破圖。*

### 2. 手機端需求
*   **系統版本**：Android 10.0 (API Level 29) 或以上版本。
*   **硬體功能**：需支援 Bluetooth 5.0 (BLE) 與 GPS 定位。

---

## 🚀 開發與建置指南

### 1. 複製專案
```bash
git clone https://github.com/你的帳號/MotoNavTW.git
```

### 2. Android Studio 環境準備
*   確保已安裝最新版的 **Android Studio**。
*   透過 SDK Manager 安裝 **NDK (Side by side)** 與 **CMake**，這是編譯 C++ 核心檔案所必須的工具。

### 3. 編譯與執行
1. 在 Android Studio 中開啟本專案。
2. 點擊 `Sync Project with Gradle Files` 確保 C++ `CMakeLists.txt` 成功連結。
3. 授予 App 必要的權限（精確定位、藍牙連線、背景執行）。
4. 點擊 `Run` 編譯並安裝至測試手機。

---

## 🙏 致謝 (Acknowledgments)

本專案的 C++ 核心通訊協定與 ESP32 硬體介面靈感，大量參考並受惠於 [mx3353672833-debug/moto-gps-waveshare](https://github.com/mx3353672833-debug/moto-gps-waveshare) 開源專案。特此感謝原作者對兩輪騎乘導航社群的貢獻！

---
*Ride Safe, Ride Smart! 🏍️💨*