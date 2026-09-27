package com.example.motonavtw

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID

class BleManager(private val context: Context) {
    private val TAG = "MotoNavTW_BLE"

    // 依照你的 C++ 標頭檔設定 UUID
    private val SERVICE_UUID = UUID.fromString("7e57a000-b50c-4b6a-9c57-40a54e8e1000")
    private val WRITE_CHAR_UUID = UUID.fromString("7e57a001-b50c-4b6a-9c57-40a54e8e1000")

    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private var isScanning = false
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    // --- 2. 負責處理連線與資料交換的 GATT 回呼 ---
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "🎉 成功連上 ESP32！正在尋找專屬服務頻道...")
                // 連上後，必須去尋找對方提供了哪些服務
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "❌ 與 ESP32 斷開連線")
                bluetoothGatt?.close()
                bluetoothGatt = null
                writeCharacteristic = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 尋找我們專屬的 Service 和負責寫入的 Characteristic
                val service = gatt.getService(SERVICE_UUID)
                writeCharacteristic = service?.getCharacteristic(WRITE_CHAR_UUID)

                if (writeCharacteristic != null) {
                    Log.d(TAG, "✅ 發射管線建立完成！隨時可以發送導航資料。")
                } else {
                    Log.e(TAG, "⚠️ 找不到發射管線，請檢查 ESP32 端的 UUID 設定。")
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "🚀 封包發射成功！")
            }
        }
    }

    // --- 1. 掃描回呼：找到 ESP32 就立刻連線 ---
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            Log.d(TAG, "找到目標設備: ${device.name ?: "未知"}, 準備發起連線...")
            stopScan()

            // 發起連線，並把結果交給上面的 gattCallback 處理
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothLeScanner == null || isScanning) return

        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        isScanning = true
        Log.d(TAG, "開始雷達掃描...")
        bluetoothLeScanner.startScan(listOf(filter), settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
        Log.d(TAG, "停止雷達掃描")
    }

    // --- 3. 提供給 MainActivity 呼叫的發送函數 ---
    @SuppressLint("MissingPermission")
    fun sendNavigationData(data: ByteArray) {
        val char = writeCharacteristic
        val gatt = bluetoothGatt
        if (char != null && gatt != null) {
            char.value = data
            // 根據你的 C++ 標頭檔，這個通道支援 WriteWithoutResponse
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            gatt.writeCharacteristic(char)
            Log.d(TAG, "嘗試發送 ${data.size} bytes 的資料...")
        } else {
            Log.w(TAG, "尚未連線或管線未建立，無法發送資料！")
        }
    }
}