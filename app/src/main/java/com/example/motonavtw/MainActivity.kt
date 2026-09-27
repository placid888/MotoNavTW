package com.example.motonavtw

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
                Log.d("MotoNavTW", "權限取得成功！")
                initializeServices()
            } else {
                Toast.makeText(this, "需要藍牙與定位權限才能完整體驗機車儀表板！", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bleManager = BleManager(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 初始化介面狀態
        binding.txtSpeed.text = "0"
        binding.txtRoadName.text = "等待定位..."
        binding.txtBleStatus.text = "🔵 藍牙：準備中"
        binding.txtGpsStatus.text = "🛰️ GPS：準備中"
        binding.txtHexDebug.text = "BLE Hex: 尚無數據"
        binding.txtTurnDistance.text = "前方 0 公尺"
        binding.txtTurnInstruction.text = "等待導航中"
        binding.txtTurnIcon.text = "⬆️"
        binding.txtMediaTitle.text = "🎵 來源：未選擇 (播放中)"

        setupSwitches()
        setupMediaButtons()
        checkAndRequestPermissions()
        checkNotificationPermission()

        // 監聽背景多媒體切換 (來源, 標題, 作者)
        MediaNotificationListener.onSongChangedListener = { source, title, artist ->
            runOnUiThread {
                val displayText = if (artist.isNotEmpty()) "[$source] $artist - $title" else "[$source] $title"
                binding.txtMediaTitle.text = "🎵 $displayText"
            }
        }
    }

    private fun setupSwitches() {
        binding.switchGps.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startGpsTracking()
                Toast.makeText(this, "已開啟 GPS 追蹤", Toast.LENGTH_SHORT).show()
            } else {
                stopGpsTracking()
                binding.txtGpsStatus.text = "🛰️ GPS：已關閉"
                Toast.makeText(this, "已關閉 GPS 追蹤", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchBle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bleManager.startScan()
                binding.txtBleStatus.text = "🔵 藍牙：掃描中..."
                Toast.makeText(this, "已開啟 BLE 發射", Toast.LENGTH_SHORT).show()
            } else {
                bleManager.stopScan()
                binding.txtBleStatus.text = "🔵 藍牙：已關閉"
                Toast.makeText(this, "已暫停 BLE 發射", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMediaButtons() {
        binding.btnPrev.setOnClickListener {
            MediaNotificationListener.skipToPrevious()
            Toast.makeText(this, "切換至上一首/上一頁", Toast.LENGTH_SHORT).show()
        }

        binding.btnPlayPause.setOnClickListener {
            MediaNotificationListener.togglePlayPause()
            Toast.makeText(this, "切換 播放/暫停", Toast.LENGTH_SHORT).show()
        }

        binding.btnNext.setOnClickListener {
            MediaNotificationListener.skipToNext()
            Toast.makeText(this, "切換至下一首/下一頁", Toast.LENGTH_SHORT).show()
        }
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
            initializeServices()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun checkNotificationPermission() {
        val cn = android.content.ComponentName(this, MediaNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val enabled = flat != null && flat.contains(cn.flattenToString())

        if (!enabled) {
            Toast.makeText(this, "請授權 MotoNavTW 讀取通知以同步播放源！", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
    }

    private fun initializeServices() {
        if (binding.switchBle.isChecked) {
            bleManager.startScan()
            binding.txtBleStatus.text = "🔵 藍牙：掃描中..."
        }
        if (binding.switchGps.isChecked) {
            startGpsTracking()
            binding.txtGpsStatus.text = "🛰️ GPS：監聽中"
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGpsTracking() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this)
            binding.txtGpsStatus.text = "🛰️ GPS：運行中"
        } else {
            Toast.makeText(this, "請先開啟手機的 GPS 定位服務！", Toast.LENGTH_LONG).show()
            binding.txtGpsStatus.text = "🛰️ GPS：未開啟定位"
        }
    }

    private fun stopGpsTracking() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        if (!binding.switchGps.isChecked) return

        val speedKph = (location.speed * 3.6).toInt()
        val roadName = "忠孝東路四段"
        val turnType = 2
        val distanceMeters = 150

        updateTurnUi(turnType, distanceMeters)

        val blePacket = encodeNavigationSnapshot(speedKph, roadName, turnType, distanceMeters)
        val hexString = blePacket.joinToString("") { "%02x".format(it) }

        binding.txtSpeed.text = "$speedKph"
        binding.txtRoadName.text = roadName
        binding.txtHexDebug.text = "BLE Hex: $hexString"

        if (binding.switchBle.isChecked) {
            bleManager.sendNavigationData(blePacket)
        }
    }

    private fun updateTurnUi(turnType: Int, distance: Int) {
        binding.txtTurnDistance.text = "前方 $distance 公尺"
        when (turnType) {
            1 -> {
                binding.txtTurnIcon.text = "⬅️"
                binding.txtTurnInstruction.text = "準備向左轉"
            }
            2 -> {
                binding.txtTurnIcon.text = "➡️"
                binding.txtTurnInstruction.text = "準備向右轉"
            }
            else -> {
                binding.txtTurnIcon.text = "⬆️"
                binding.txtTurnInstruction.text = "請繼續直行"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGpsTracking()
        bleManager.stopScan()
    }

    external fun encodeNavigationSnapshot(speedKph: Int, roadName: String, turnType: Int, distanceMeters: Int): ByteArray

    companion object {
        init {
            System.loadLibrary("motonavtw")
        }
    }
}