package com.motonavtw.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

@SuppressLint("MissingPermission") // 權限將在 UI 層透過 Accompanist 請求
class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    // Glimpse 專案預設的 Service 與 TX Characteristic UUID (請核對 shared/ble-navigation-v1.md)
    private val GLIMPSE_SERVICE_UUID = UUID.fromString("0000B700-0000-1000-8000-00805F9B34FB")
    private val GLIMPSE_TX_UUID = UUID.fromString("0000B701-0000-1000-8000-00805F9B34FB")

    // 連線狀態機
    private val _connectionState = MutableStateFlow("未連線")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    // GATT 操作排隊機制 (避免發送背壓與交織)
    private val writeQueue: Queue<ByteArray> = LinkedList()
    private var isWriting = false
    private var negotiatedMtu = 20 // 預設 MTU

    // 掃描回呼
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: "Unknown Device"

            // 找到微雪 ESP32 圓屏後自動停止掃描並連線
            if (deviceName.contains("MOTO") || deviceName.contains("GLIMPSE")) {
                Log.d("BleManager", "找到目標設備: $deviceName [${device.address}]")
                stopScan()
                connectToDevice(device)
            }
        }
    }

    // GATT 連線回呼
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectionState.value = "已連線，正在發現服務..."
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionState.value = "已斷線"
                    closeGatt()
                }
            } else {
                _connectionState.value = "連線錯誤 (Status: $status)"
                closeGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = "服務已發現，請求 MTU..."
                // 遵守指南：必須發起 MTU 協商，不能假設永遠是 512
                gatt.requestMtu(512)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                negotiatedMtu = mtu
                _connectionState.value = "連線就緒 (MTU: $mtu)"
                Log.d("BleManager", "MTU 協商成功: $mtu bytes")
                // TODO: 訂閱 RX Notify 並發送版本握手訊號
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            isWriting = false
            if (status == BluetoothGatt.GATT_SUCCESS) {
                processNextWrite() // 成功後處理佇列中的下一個封包
            } else {
                Log.e("BleManager", "寫入失敗，Status: $status")
                // TODO: 實作重試上限與最新狀態補發邏輯
            }
        }
    }

    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = "藍牙未開啟"
            return
        }
        _connectionState.value = "掃描中..."
        bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        _connectionState.value = "連線中..."
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    // 將來自 C++ 核心序列化後的位元組陣列加入佇列
    fun queueWrite(data: ByteArray) {
        writeQueue.offer(data)
        if (!isWriting) {
            processNextWrite()
        }
    }

    private fun processNextWrite() {
        if (writeQueue.isEmpty() || bluetoothGatt == null) return

        val service = bluetoothGatt!!.getService(GLIMPSE_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(GLIMPSE_TX_UUID)

        if (characteristic != null) {
            val data = writeQueue.poll()
            characteristic.value = data
            // 遵守指南：需支援 Write Without Response，提升導航畫面幀率
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            isWriting = true
            bluetoothGatt!!.writeCharacteristic(characteristic)
        }
    }

    fun closeGatt() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeQueue.clear()
        isWriting = false
    }
}