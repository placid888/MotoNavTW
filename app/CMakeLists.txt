cmake_minimum_required(VERSION 3.22.1)
project("motonavtw")

# 將所有 C++ 原始碼加入專案
add_library(
        motonavtw
        SHARED
        src/main/cpp/native-lib.cpp
        # 等我們正式把 Glimpse 的 shared 資料夾搬過來後，再取消下面兩行的註解
        # src/main/cpp/moto.ble_protocol/ble_protocol.cpp
        # src/main/cpp/nav_app/nav_app.cpp
)

# 設定標頭檔搜尋路徑 (Include directories)
target_include_directories(
        motonavtw
        PRIVATE
        src/main/cpp/moto.ble_protocol
        src/main/cpp/nav_app
)

# 連結 Android 系統日誌庫
find_library(log-lib log)

target_link_libraries(
        motonavtw
        ${log-lib}
)
# 強制 16 KB 記憶體分頁對齊以相容 Android 15+
target_link_options(
        motonavtw
        PRIVATE
        "-Wl,-z,max-page-size=16384"
)