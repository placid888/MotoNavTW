# 設定 CMake 最低版本要求
cmake_minimum_required(VERSION 3.22.1)

# 定義專案名稱
project("motonavtw")

# 設定 C++ 標準為 C++17 (非常重要，因為原始碼使用了 std::variant 等 C++17 特性)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)

# 設定要編譯的 shared 目錄下的原始碼
# (初期我們先編譯藍牙協定相關的核心檔案)
set(SHARED_SRC
        shared/ble_protocol/src/ble_protocol.cpp
)

# 建立我們要編譯的共享程式庫 (Library)
add_library(
        # 產出的 SO 檔案名稱 (在 Kotlin 裡會用 System.loadLibrary("motonavtw") 呼叫)
        motonavtw

        # 設定為動態連結庫
        SHARED

        # 這裡加入預設的 native-lib.cpp 以及我們上面定義的 SHARED_SRC
        native-lib.cpp
        ${SHARED_SRC}
)

# 告訴編譯器去哪裡找 C++ 的 Header 檔 (.hpp)
# 加入了 shared/ble_protocol/include，讓編譯器能精準對應到 #include "moto/ble_protocol/..."
target_include_directories(motonavtw PRIVATE
        shared
        shared/ble_protocol/include
)

# 連結 Android 內建的 Log 程式庫 (為了我們後續可以用 __android_log_print 輸出日誌)
find_library(
        log-lib
        log)

# 將我們的程式庫與 Android 系統程式庫連結
target_link_libraries(
        motonavtw
        ${log-lib})