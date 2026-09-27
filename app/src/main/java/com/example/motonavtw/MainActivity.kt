package com.example.motonavtw

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.motonavtw.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bleManager: BleManager
    private lateinit var locationManager: LocationManager

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Log.d("MotoNavTW", "權限取得成功！啟動藍牙與 GPS...")
                bleManager.startScan()
                startGpsTracking()
            } else {
                Toast.makeText(this, "需要藍牙與定位權限才能執行機車導航！", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 初始化藍牙與定位管理員
        bleManager = BleManager(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        binding.sampleText.text = "MotoNavTW 啟動中...\n等待權限與 GPS 訊號..."

        // 2. 檢查並請求所有必要權限 (包含藍牙與 GPS)
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            Log.d("MotoNavTW", "所有權限已具備，啟動服務...")
            bleManager.startScan()
            startGpsTracking()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGpsTracking() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // 每隔 1 秒或移動 1 公尺更新一次 GPS 數據
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                1f,
                this
            )
            Log.d("MotoNavTW", "已開始監聽 GPS 衛星時速...")
        } else {
            Toast.makeText(this, "請先開啟手機的 GPS 定位服務！", Toast.LENGTH_LONG).show()
        }
    }

    // --- 當 GPS 衛星回傳最新位置與速度時觸發 ---
    override fun onLocationChanged(location: Location) {
        // location.speed 單位是「公尺/秒 (m/s)」，我們把它換算成「公里/小時 (km/h)」
        val speedKph = (location.speed * 3.6).toInt()
        val roadName = "即時導航路段" // 後續我們可以對接地圖 API 取得真實路名

        Log.d("MotoNavTW", "收到真實 GPS 時速: $speedKph km/h")

        // 1. 透過 C++ 引擎將時速與路名打包成 BLE 專屬二進位封包
        val blePacket = encodeNavigationSnapshot(speedKph, roadName)
        val hexString = blePacket.joinToString("") { "%02x".format(it) }

        // 2. 更新手機畫面顯示
        binding.sampleText.text = "GPS 即時同步中 🏍️\n當前時速: $speedKph km/h\n\nBLE 封包:\n$hexString"

        // 3. 透過藍牙發射器把封包射給 ESP32 儀表板
        bleManager.sendNavigationData(blePacket)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
        bleManager.stopScan()
    }

    // --- C++ JNI 外部函數宣告 ---
    external fun encodeNavigationSnapshot(speedKph: Int, roadName: String): ByteArray

    companion object {
        init {
            System.loadLibrary("motonavtw")
        }
    }
}