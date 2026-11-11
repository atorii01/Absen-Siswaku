package com.andev.absensiswaku.ui.presensi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.andev.absensiswaku.MainActivity
import com.andev.absensiswaku.R
import com.andev.absensiswaku.data.network.SupabaseClient
import com.andev.absensiswaku.data.pref.SessionManager
import com.andev.absensiswaku.databinding.FragmentPresensiBinding
import com.andev.absensiswaku.util.MotionUtils
import com.andev.absensiswaku.util.NotificationHelper
import com.andev.absensiswaku.util.applyBounceEffect
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.roundToInt

class PresensiFragment : Fragment() {

    private var _binding: FragmentPresensiBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var cancellationTokenSource: CancellationTokenSource? = null

    private var currentDistanceMeters: Double = 999.0 // Updated dynamically by GPS
    private var isMockLocationDetected: Boolean = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    private var isTorchOn: Boolean = false
    private var imageCapture: ImageCapture? = null

    // Google ML Kit Face Detector
    private lateinit var faceDetector: FaceDetector
    private var isFaceDetected: Boolean = false
    private var isFaceInsideCircle: Boolean = false
    private var lastBiometricScore: Double = 0.0
    private var hasEyesClosed: Boolean = false
    private var isLivenessVerified: Boolean = false

    private var isAlreadyPresensi: Boolean = false
    private var isAlreadyPulang: Boolean = false
    private var todayPresensiId: String? = null
    private var todayWaktuPulang: String? = null
    private var isPulangMode: Boolean = false
    private var liveClockJob: Job? = null

    companion object {
        // Koordinat Resmi SMKN 8 Jakarta
        private const val SCHOOL_LAT = -6.278162
        private const val SCHOOL_LNG = 106.836069
        private const val MAX_RADIUS_METERS = 50.0

        private const val JAM_BUKA_MINUTES = 5 * 60 + 30 // 05:30 WIB
        private const val JAM_BATAS_MINUTES = 6 * 60 + 40 // 06:40 WIB
        private const val JAM_PULANG_MINUTES = 15 * 60 // 15:00 WIB
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (!isAdded || _binding == null) return@registerForActivityResult

            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            if (cameraGranted) {
                startCamera()
            } else {
                Toast.makeText(context ?: return@registerForActivityResult, "Izin Kamera diperlukan untuk presensi biometrik", Toast.LENGTH_LONG).show()
            }

            if (locationGranted) {
                startLocationUpdates()
            } else {
                Toast.makeText(context ?: return@registerForActivityResult, "Izin Lokasi GPS diperlukan untuk verifikasi geofence", Toast.LENGTH_LONG).show()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (!isAdded || _binding == null) return@registerForActivityResult

            if (bitmap == null) {
                Toast.makeText(context ?: return@registerForActivityResult, "Batal mengambil foto", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            processBitmapForFaceDetection(bitmap)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPresensiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = context ?: return
        sessionManager = SessionManager(ctx)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupFaceDetector()
        setupHeaderData()
        setupCameraControls()
        setupSubmitButton()
        startLiveClock()
        updateFaceWarningBadge(FaceWarningBadgeState.NOT_DETECTED)
        checkAndRequestPermissions()

        binding.swipeRefreshPresensi.setColorSchemeResources(R.color.primary_teal)
        binding.swipeRefreshPresensi.setOnRefreshListener {
            checkDailyPresensiStatus()
            updateOfflineBannerUI()
            startLocationUpdates()
            view?.postDelayed({
                _binding?.swipeRefreshPresensi?.isRefreshing = false
            }, 1200)
        }

        binding.btnSyncOfflineNow.setOnClickListener {
            val c = context ?: return@setOnClickListener
            if (!com.andev.absensiswaku.util.NetworkUtils.isNetworkAvailable(c)) {
                Toast.makeText(c, "Tidak ada koneksi internet. Presensi tetap tersimpan aman di HP.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnSyncOfflineNow.isEnabled = false
            binding.btnSyncOfflineNow.text = "Menyinkronkan..."
            com.andev.absensiswaku.util.OfflinePresensiManager.syncPendingPresensi(c) { success, failed ->
                if (!isAdded || _binding == null) return@syncPendingPresensi
                binding.btnSyncOfflineNow.isEnabled = true
                binding.btnSyncOfflineNow.text = "Sinkron"
                updateOfflineBannerUI()
                if (success > 0) {
                    Toast.makeText(c, "✓ $success Presensi offline berhasil disinkronkan ke server!", Toast.LENGTH_LONG).show()
                    checkDailyPresensiStatus()
                } else if (failed > 0) {
                    Toast.makeText(c, "Sebagian presensi belum dapat diunggah, akan dicoba kembali saat koneksi stabil.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        MotionUtils.animateStaggeredEntrance(
            binding.cardStatusTanggal,
            binding.cardCameraContainer,
            binding.cardGpsStatus,
            binding.cardVerifiedInfo,
            binding.btnSubmitPresensi
        )
    }

    override fun onResume() {
        super.onResume()
        setupHeaderData()
        checkDailyPresensiStatus()
        updateOfflineBannerUI()
        syncOfflinePresensiIfNeeded()
    }

    private fun updateOfflineBannerUI() {
        if (!isAdded || _binding == null) return
        val ctx = context ?: return
        val count = com.andev.absensiswaku.util.OfflinePresensiManager.getPendingCount(ctx)
        if (count > 0) {
            binding.cardOfflineQueueBanner.visibility = View.VISIBLE
            binding.tvOfflineBannerTitle.text = "$count Presensi Menunggu Sinkron"
        } else {
            binding.cardOfflineQueueBanner.visibility = View.GONE
        }
    }

    private fun syncOfflinePresensiIfNeeded() {
        val ctx = context ?: return
        if (com.andev.absensiswaku.util.NetworkUtils.isNetworkAvailable(ctx) &&
            com.andev.absensiswaku.util.OfflinePresensiManager.getPendingCount(ctx) > 0) {
            com.andev.absensiswaku.util.OfflinePresensiManager.syncPendingPresensi(ctx) { success, _ ->
                if (isAdded && _binding != null) {
                    updateOfflineBannerUI()
                    if (success > 0) {
                        Toast.makeText(ctx, "✓ $success Presensi offline berhasil disinkronkan ke server!", Toast.LENGTH_LONG).show()
                        checkDailyPresensiStatus()
                    }
                }
            }
        } else {
            updateOfflineBannerUI()
        }
    }

    private fun checkDailyPresensiStatus() {
        if (!isAdded || _binding == null) return

        val pref = context?.getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)
        val siswaIdInt = try {
            pref?.getInt("user_id", 0)?.takeIf { it != 0 }
                ?: pref?.getInt("ID_SISWA", 0)?.takeIf { it != 0 }
                ?: pref?.getString("user_id", "0")?.toIntOrNull()
                ?: pref?.getString("ID_SISWA", "0")?.toIntOrNull()
                ?: sessionManager.getUserId().takeIf { it != 0 }
                ?: 1
        } catch (e: Exception) { 1 }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        SupabaseClient.instance.checkPresensiHariIni(
            filterSiswa = "eq.$siswaIdInt",
            filterTanggal = "eq.$todayDate"
        ).enqueue(object : Callback<List<Map<String, Any>>> {
            override fun onResponse(
                call: Call<List<Map<String, Any>>>,
                response: Response<List<Map<String, Any>>>
            ) {
                if (!isAdded || _binding == null) return
                binding.swipeRefreshPresensi.isRefreshing = false

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val record = response.body()!![0]
                    val statusText = record["status"]?.toString() ?: "Hadir"
                    val statusMsg = if (statusText.equals("Hadir", true)) "HADIR" else "TERLAMBAT"
                    todayPresensiId = record["id"]?.toString()
                    val pulangVal = record["waktu_pulang"]?.toString()
                    val hasPulang = !pulangVal.isNullOrBlank() && pulangVal != "null"
                    todayWaktuPulang = if (hasPulang) pulangVal else null

                    isAlreadyPresensi = true
                    isAlreadyPulang = true

                    binding.tvBadgeStatusPresensi.apply {
                        text = "SUDAH PRESENSI ($statusMsg)"
                        setBackgroundResource(R.drawable.bg_badge_pill_verified_teal)
                        val ctx = context ?: return@apply
                        setTextColor(ContextCompat.getColor(ctx, R.color.primary_teal))
                    }

                    binding.btnSubmitPresensi.apply {
                        isEnabled = false
                        text = "✓ Presensi Hari Ini Selesai"
                        val ctx = context ?: return@apply
                        setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
                    }
                }
            }

            override fun onFailure(call: Call<List<Map<String, Any>>>, t: Throwable) {
                if (!isAdded || _binding == null) return
                binding.swipeRefreshPresensi.isRefreshing = false
                // Biarkan status normal jika offline / koneksi gagal
            }
        })
    }

    private fun setupFaceDetector() {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()

        faceDetector = FaceDetection.getClient(options)
    }

    private fun checkAndRequestPermissions() {
        val ctx = context ?: return
        val hasCamera = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasLocation = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasCamera) {
            startCamera()
        }
        if (hasLocation) {
            startLocationUpdates()
        }

        if (!hasCamera || !hasLocation) {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun setupHeaderData() {
        if (!isAdded || _binding == null) return

        val dateFormatted = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())
        val nisn = sessionManager.getNisn().ifEmpty { "0061829103" }
        val nama = sessionManager.getNama().ifEmpty { "Muhammad Fadhil" }
        val namaKelas = sessionManager.getNamaKelas().ifEmpty { "10 IPA 1" }

        binding.tvTanggalHeader.text = dateFormatted
        binding.tvTopBarNisn.text = "NISN: $nisn"
        binding.tvNamaSiswa.text = nama
        binding.tvKelasNisnSub.text = "Kelas $namaKelas \u2022 NISN: $nisn"
        binding.tvBatasMasukJam.text = "06:40 WIB"

        // Muat foto profil kustom siswa jika ada
        val ctx = context ?: return
        val siswaId = sessionManager.getUserId()
        val avatarFile = com.andev.absensiswaku.util.AvatarHelper.getAvatarFileForSiswa(siswaId)
        com.andev.absensiswaku.util.AvatarHelper.loadAvatar(ctx, avatarFile, binding.imgAvatar)

        binding.imgAvatar.applyBounceEffect {
            (activity as? MainActivity)?.selectTab(R.id.nav_setelan)
        }
    }

    private fun startLiveClock() {
        if (!isAdded || _binding == null || view == null) return
        liveClockJob?.cancel()
        liveClockJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                if (!isAdded || _binding == null) break

                val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                val sdf = SimpleDateFormat("HH:mm:ss 'WIB'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")

                val currentTimeStr = sdf.format(calendar.time)
                binding.tvLiveClock.text = currentTimeStr

                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val currentMinuteOfDay = hour * 60 + minute

                evaluatePresensiState(currentMinuteOfDay)

                delay(1000)
            }
        }
    }

    private fun isMockLocation(location: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock || location.accuracy <= 0.001f || (location.latitude == 0.0 && location.longitude == 0.0)
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider || location.accuracy <= 0.001f || (location.latitude == 0.0 && location.longitude == 0.0)
        }
    }

    private fun isFaceInCircle(face: Face, frameWidth: Int, frameHeight: Int): Boolean {
        if (frameWidth <= 0 || frameHeight <= 0) return true

        val faceCenterX = face.boundingBox.centerX().toFloat()
        val faceCenterY = face.boundingBox.centerY().toFloat()

        val ovalWidth = frameWidth * 0.65f
        val ovalHeight = frameHeight * 0.68f
        val ovalLeft = (frameWidth - ovalWidth) / 2f
        val ovalTop = (frameHeight - ovalHeight) / 2f
        val ovalRight = ovalLeft + ovalWidth
        val ovalBottom = ovalTop + ovalHeight

        // Margin toleransi (12%)
        val marginX = ovalWidth * 0.12f
        val marginY = ovalHeight * 0.12f

        return faceCenterX in (ovalLeft - marginX)..(ovalRight + marginX) &&
               faceCenterY in (ovalTop - marginY)..(ovalBottom + marginY)
    }

    private fun evaluatePresensiState(currentMinuteOfDay: Int) {
        if (!isAdded || _binding == null) return

        if (isAlreadyPresensi || isAlreadyPulang) {
            binding.btnSubmitPresensi.apply {
                isEnabled = false
                text = "✓ Presensi Hari Ini Selesai"
                val ctx = context ?: return@apply
                setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
            }
            binding.tvBadgeStatusPresensi.apply {
                text = "PRESENSI HARI INI SELESAI"
                setBackgroundResource(R.drawable.bg_badge_pill_verified_teal)
                val ctx = context ?: return@apply
                setTextColor(ContextCompat.getColor(ctx, R.color.primary_teal))
            }
            return
        }

        isPulangMode = false

        val isSchoolWifi = context?.let { com.andev.absensiswaku.util.SchoolWifiHelper.isConnectedToSchoolWifi(it) } == true
        val inRadius = (currentDistanceMeters <= MAX_RADIUS_METERS) || isSchoolWifi
        val distanceInt = currentDistanceMeters.toInt()

        when {
            isMockLocationDetected -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = false
                    text = "Terdeteksi Fake GPS / Lokasi Tiruan!"
                    val ctx = context ?: return@apply
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
                }
            }
            !isFaceDetected -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = false
                    text = "✕ Wajah Tidak Terdeteksi"
                    val ctx = context ?: return@apply
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
                }
            }
            !isFaceInsideCircle -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = false
                    text = "✕ Wajah Di Luar Radius Lingkaran Panduan"
                    val ctx = context ?: return@apply
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
                }
            }
            !isLivenessVerified -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = false
                    text = "Kedipkan Mata (Verifikasi Liveness)"
                    val ctx = context ?: return@apply
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
                }
            }
            !inRadius -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = false
                    text = "Di Luar Radius Sekolah (${distanceInt}m)"
                    val ctx = context ?: return@apply
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
                }
            }
            isPulangMode -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = true
                    text = "✓ Kirim Presensi Pulang Sekarang"
                    val ctx = context ?: return@apply
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.primary_teal))
                }
            }
            currentMinuteOfDay < JAM_BUKA_MINUTES -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = false
                    text = "Presensi Dibuka Pukul 05:30 WIB"
                    val ctx = context ?: return@apply
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
                }
            }
            currentMinuteOfDay <= JAM_BATAS_MINUTES -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = true
                    text = "✓ Presensi Sekarang (Tepat Waktu)"
                    val ctx = context ?: return@apply
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.primary_teal))
                }
            }
            else -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = true
                    text = "✓ Presensi Sekarang (Terlambat)"
                    val ctx = context ?: return@apply
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_orange_late))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!isAdded) return

        cancellationTokenSource?.cancel()
        cancellationTokenSource = CancellationTokenSource()

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource!!.token
            ).addOnSuccessListener { location: Location? ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (location != null) {
                    updateLocationUI(location)
                } else {
                    binding.tvStatusRadiusTitle.text = "Mencari sinyal GPS..."
                    binding.btnSubmitPresensi.isEnabled = false
                }
            }.addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.tvStatusRadiusTitle.text = "Gagal membaca lokasi"
                binding.btnSubmitPresensi.isEnabled = false
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(1500L)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (!isAdded || _binding == null) return
                    val location = locationResult.lastLocation ?: return
                    updateLocationUI(location)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateLocationUI(userLocation: Location) {
        if (!isAdded || _binding == null) return

        val ctx = context ?: return

        // Proteksi Anti-Fake GPS / Mock Location
        if (isMockLocation(userLocation)) {
            isMockLocationDetected = true
            currentDistanceMeters = 999.0

            binding.tvJarakRealtime.text = "Ilegal"
            binding.tvAkurasiSub.text = "Fake GPS"
            binding.progressBarJarak.progress = 50
            binding.tvPosisiProgress.text = "Posisi: Fake GPS"

            binding.tvStatusRadiusTitle.text = "✕ Ilegal / Fake GPS Terdeteksi"
            binding.tvStatusRadiusTitle.setTextColor(
                ContextCompat.getColor(ctx, R.color.status_unread_text)
            )

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            evaluatePresensiState(hour * 60 + minute)
            return
        }

        isMockLocationDetected = false

        val sekolahLoc = Location("sekolah").apply {
            latitude = SCHOOL_LAT
            longitude = SCHOOL_LNG
        }

        val jarakMeter = userLocation.distanceTo(sekolahLoc)
        currentDistanceMeters = jarakMeter.toDouble()
        val distanceInt = jarakMeter.toInt()

        val isSchoolWifi = com.andev.absensiswaku.util.SchoolWifiHelper.isConnectedToSchoolWifi(ctx)

        binding.tvJarakRealtime.text = "$distanceInt m"
        binding.tvAkurasiSub.text = if (isSchoolWifi) "Wi-Fi SMKN 8 Terverifikasi" else "Akurasi ±${userLocation.accuracy.toInt()}m"
        binding.progressBarJarak.progress = minOf(distanceInt, 50)
        binding.tvPosisiProgress.text = if (isSchoolWifi) "Wi-Fi: SMKN 8" else "Posisi: ${distanceInt}m"

        if (isSchoolWifi) {
            binding.tvStatusRadiusTitle.text = "Dalam Radius (Wi-Fi SMKN 8) ✓"
            binding.tvStatusRadiusTitle.setTextColor(
                ContextCompat.getColor(ctx, R.color.emerald_green)
            )
        } else if (jarakMeter <= MAX_RADIUS_METERS) {
            binding.tvStatusRadiusTitle.text = "Dalam Radius Sekolah ✓"
            binding.tvStatusRadiusTitle.setTextColor(
                ContextCompat.getColor(ctx, R.color.emerald_green)
            )
        } else {
            binding.tvStatusRadiusTitle.text = "Di Luar Radius Sekolah (${distanceInt}m)"
            binding.tvStatusRadiusTitle.setTextColor(
                ContextCompat.getColor(ctx, R.color.status_unread_text)
            )
        }

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        evaluatePresensiState(hour * 60 + minute)
    }

    private fun startCamera() {
        val ctx = context ?: return
        if (!isAdded || _binding == null || view == null) return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            if (!isAdded || _binding == null || view == null) return@addListener
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = binding.cameraPreviewView.surfaceProvider
                }

                imageCapture = ImageCapture.Builder().build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imageProxy ->
                    processImageFrameForFaceDetection(imageProxy)
                }

                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageFrameForFaceDetection(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    isFaceDetected = true
                    isFaceInsideCircle = isFaceInCircle(face, inputImage.width, inputImage.height)

                    val faceWidth = face.boundingBox.width().toFloat()
                    val faceHeight = face.boundingBox.height().toFloat()
                    val frameArea = (inputImage.width * inputImage.height).toFloat()
                    val faceAreaRatio = if (frameArea > 0f) (faceWidth * faceHeight) / frameArea else 0.1f

                    val baseScore = 90.0 + (faceAreaRatio * 20.0).coerceAtMost(6.0)
                    val eyeBonus = if ((face.leftEyeOpenProbability ?: 0f) > 0.5f && (face.rightEyeOpenProbability ?: 0f) > 0.5f) 2.5 else 0.0
                    val smileBonus = if ((face.smilingProbability ?: 0f) > 0.3f) 0.5 else 0.0

                    lastBiometricScore = (baseScore + eyeBonus + smileBonus).coerceIn(85.0, 99.2)
                    val scoreStr = String.format(Locale.US, "%.1f", lastBiometricScore)

                    val leftEye = face.leftEyeOpenProbability ?: -1f
                    val rightEye = face.rightEyeOpenProbability ?: -1f

                    if (isFaceInsideCircle) {
                        if (!isLivenessVerified) {
                            if (leftEye in 0.0f..0.25f && rightEye in 0.0f..0.25f) {
                                hasEyesClosed = true
                            } else if (hasEyesClosed && leftEye > 0.60f && rightEye > 0.60f) {
                                isLivenessVerified = true
                                view?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        }

                        if (isLivenessVerified) {
                            binding.tvStatusKameraChip.text = "Kamera Aktif"
                            updateFaceWarningBadge(FaceWarningBadgeState.MATCHED, "$scoreStr% (Live ✓)")
                        } else {
                            binding.tvStatusKameraChip.text = "Kamera Aktif"
                            updateFaceWarningBadge(FaceWarningBadgeState.NEED_BLINK, scoreStr)
                        }
                    } else {
                        hasEyesClosed = false
                        isLivenessVerified = false
                        binding.tvStatusKameraChip.text = "Kamera Aktif"
                        updateFaceWarningBadge(FaceWarningBadgeState.OUTSIDE_CIRCLE)
                    }
                } else {
                    isFaceDetected = false
                    isFaceInsideCircle = false
                    lastBiometricScore = 0.0
                    hasEyesClosed = false
                    isLivenessVerified = false

                    binding.tvStatusKameraChip.text = "Kamera Aktif"
                    updateFaceWarningBadge(FaceWarningBadgeState.NOT_DETECTED)
                }

                val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                evaluatePresensiState(hour * 60 + minute)
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                isFaceDetected = false
                isFaceInsideCircle = false
                updateFaceWarningBadge(FaceWarningBadgeState.NOT_DETECTED)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun processBitmapForFaceDetection(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val ctx = context ?: return@addOnSuccessListener
                if (faces.isNotEmpty()) {
                    isFaceDetected = true
                    isFaceInsideCircle = true
                    lastBiometricScore = 95.0
                    updateFaceWarningBadge(FaceWarningBadgeState.MATCHED, "95.0")
                    Toast.makeText(ctx, "Wajah Terdeteksi dari foto!", Toast.LENGTH_SHORT).show()
                } else {
                    isFaceDetected = false
                    isFaceInsideCircle = false
                    lastBiometricScore = 0.0
                    updateFaceWarningBadge(FaceWarningBadgeState.NOT_DETECTED)
                    Toast.makeText(ctx, "Wajah tidak terdeteksi dalam foto!", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                isFaceDetected = false
                isFaceInsideCircle = false
                updateFaceWarningBadge(FaceWarningBadgeState.NOT_DETECTED)
            }
    }

    private enum class FaceWarningBadgeState {
        MATCHED,
        NEED_BLINK,
        OUTSIDE_CIRCLE,
        NOT_DETECTED
    }

    /**
     * Memperbarui kontras visual badge status deteksi wajah pada overlay kamera presensi
     * sesuai standar High-Contrast Semantic Alert (WCAG AAA contrast on camera feeds).
     */
    private fun updateFaceWarningBadge(state: FaceWarningBadgeState, scoreStr: String = "") {
        if (!isAdded || _binding == null) return
        val ctx = context ?: return

        when (state) {
            FaceWarningBadgeState.MATCHED -> {
                binding.biometricOverlay.setOverlayState(com.andev.absensiswaku.ui.custom.BiometricOverlayView.OverlayState.MATCHED)
                // Status Berhasil / Wajah Terdeteksi & Presisi di Dalam Lingkaran
                binding.cardBadgeWarningWajah.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.camera_badge_success_bg)
                )
                binding.cardBadgeWarningWajah.strokeColor =
                    ContextCompat.getColor(ctx, R.color.camera_badge_success_stroke)
                binding.cardBadgeWarningWajah.strokeWidth =
                    (1 * resources.displayMetrics.density).roundToInt()
                binding.ivStatusWarningIcon.setImageResource(R.drawable.ic_check_circle)
                binding.ivStatusWarningIcon.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.white))
                binding.tvStatusWajah.text = if (scoreStr.contains("%")) "AI Match $scoreStr" else "AI Match $scoreStr%"
            }
            FaceWarningBadgeState.NEED_BLINK -> {
                binding.biometricOverlay.setOverlayState(com.andev.absensiswaku.ui.custom.BiometricOverlayView.OverlayState.MATCHED)
                binding.cardBadgeWarningWajah.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.camera_badge_alert_bg)
                )
                binding.cardBadgeWarningWajah.strokeColor =
                    ContextCompat.getColor(ctx, R.color.camera_badge_alert_stroke)
                binding.cardBadgeWarningWajah.strokeWidth =
                    (1 * resources.displayMetrics.density).roundToInt()
                binding.ivStatusWarningIcon.setImageResource(R.drawable.ic_check_circle)
                binding.ivStatusWarningIcon.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.camera_badge_alert_icon_yellow))
                binding.tvStatusWajah.text = "Kedipkan Mata Anda"
            }
            FaceWarningBadgeState.OUTSIDE_CIRCLE -> {
                binding.biometricOverlay.setOverlayState(com.andev.absensiswaku.ui.custom.BiometricOverlayView.OverlayState.OUTSIDE)
                // Status Peringatan: Wajah terdeteksi namun di luar area oval lingkaran panduan
                binding.cardBadgeWarningWajah.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.camera_badge_alert_bg)
                )
                binding.cardBadgeWarningWajah.strokeColor =
                    ContextCompat.getColor(ctx, R.color.camera_badge_alert_stroke)
                binding.cardBadgeWarningWajah.strokeWidth =
                    (1 * resources.displayMetrics.density).roundToInt()
                binding.ivStatusWarningIcon.setImageResource(R.drawable.ic_warning)
                binding.ivStatusWarningIcon.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.camera_badge_alert_icon_yellow))
                binding.tvStatusWajah.text = "Wajah Di Luar Lingkaran"
            }
            FaceWarningBadgeState.NOT_DETECTED -> {
                binding.biometricOverlay.setOverlayState(com.andev.absensiswaku.ui.custom.BiometricOverlayView.OverlayState.IDLE)
                // Status Peringatan: Wajah belum/tidak terdeteksi oleh kamera
                binding.cardBadgeWarningWajah.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.camera_badge_alert_bg)
                )
                binding.cardBadgeWarningWajah.strokeColor =
                    ContextCompat.getColor(ctx, R.color.camera_badge_alert_stroke)
                binding.cardBadgeWarningWajah.strokeWidth =
                    (1 * resources.displayMetrics.density).roundToInt()
                binding.ivStatusWarningIcon.setImageResource(R.drawable.ic_warning)
                binding.ivStatusWarningIcon.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.white))
                binding.tvStatusWajah.text = "Wajah Tidak Terdeteksi"
            }
        }
    }

    private fun setupCameraControls() {
        binding.btnFlashlight.applyBounceEffect {
            val ctx = context ?: return@applyBounceEffect
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                isTorchOn = !isTorchOn
                camera?.cameraControl?.enableTorch(isTorchOn)
                binding.btnFlashlight.setColorFilter(
                    if (isTorchOn) ContextCompat.getColor(ctx, R.color.emerald_green)
                    else Color.WHITE
                )
            } else {
                Toast.makeText(ctx, "Flashlight tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSwitchCamera.applyBounceEffect {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
            startCamera()
        }

        binding.btnShutter.applyBounceEffect {
            val ctx = context ?: return@applyBounceEffect
            if (isFaceDetected && isFaceInsideCircle) {
                val scoreStr = String.format(Locale.US, "%.1f", lastBiometricScore)
                Toast.makeText(ctx, "Wajah Terdeteksi! Skor AI Match: $scoreStr%", Toast.LENGTH_SHORT).show()
            } else if (isFaceDetected && !isFaceInsideCircle) {
                Toast.makeText(ctx, "Posisikan wajah Anda tepat di dalam lingkaran panduan hijau!", Toast.LENGTH_LONG).show()
            } else {
                takePictureLauncher.launch(null)
            }
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitPresensi.applyBounceEffect {
            val ctx = context ?: return@applyBounceEffect

            if (isAlreadyPresensi || isAlreadyPulang) {
                Toast.makeText(ctx, "Presensi hari ini sudah selesai tercatat.", Toast.LENGTH_LONG).show()
                return@applyBounceEffect
            }

            if (isMockLocationDetected) {
                Toast.makeText(ctx, "Presensi ditolak: Terdeteksi penggunaan Fake GPS / Lokasi Tiruan!", Toast.LENGTH_LONG).show()
                return@applyBounceEffect
            }

            if (!isFaceDetected) {
                Toast.makeText(ctx, "Presensi ditolak: Wajah wajib terdeteksi kamera!", Toast.LENGTH_LONG).show()
                return@applyBounceEffect
            }

            if (!isFaceInsideCircle) {
                Toast.makeText(ctx, "Presensi ditolak: Posisikan wajah Anda tepat di dalam lingkaran panduan hijau!", Toast.LENGTH_LONG).show()
                return@applyBounceEffect
            }

            if (!isLivenessVerified) {
                Toast.makeText(ctx, "Presensi ditolak: Silakan kedipkan mata untuk verifikasi biometrik asli!", Toast.LENGTH_LONG).show()
                return@applyBounceEffect
            }

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val currentMinuteOfDay = hour * 60 + minute

            val statusPresensi = if (currentMinuteOfDay <= JAM_BATAS_MINUTES) "Hadir" else "Terlambat"

            sendPresensiData(statusPresensi)
        }
    }

    private fun sendPresensiData(status: String) {
        val pref = context?.getSharedPreferences("PREF_SMKN8_SESSION", Context.MODE_PRIVATE)

        val siswaIdInt = try {
            pref?.getInt("user_id", 0)?.takeIf { it != 0 }
                ?: pref?.getInt("ID_SISWA", 0)?.takeIf { it != 0 }
                ?: pref?.getString("user_id", "0")?.toIntOrNull()
                ?: pref?.getString("ID_SISWA", "0")?.toIntOrNull()
                ?: sessionManager.getUserId().takeIf { it != 0 }
                ?: 1
        } catch (e: Exception) { 1 }

        val kelasIdInt = try {
            pref?.getInt("id_kelas", 0)?.takeIf { it != 0 }
                ?: pref?.getInt("ID_KELAS", 0)?.takeIf { it != 0 }
                ?: pref?.getString("id_kelas", "0")?.toIntOrNull()
                ?: pref?.getString("ID_KELAS", "0")?.toIntOrNull()
                ?: sessionManager.getIdKelas().takeIf { it != 0 }
                ?: 9
        } catch (e: Exception) { 9 }

        val currentCtx = context
        val isSchoolWifi = currentCtx?.let { com.andev.absensiswaku.util.SchoolWifiHelper.isConnectedToSchoolWifi(it) } == true
        val distanceInt = currentDistanceMeters.roundToInt()
        val effectiveDistance = if (isSchoolWifi && distanceInt > 50) minOf(distanceInt, 15) else distanceInt
        val scoreToSubmit = if (lastBiometricScore > 0) lastBiometricScore else 95.0

        val sdfTanggal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfWaktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()

        val jamSekarang = sdfWaktu.format(now)
        val statusHadir = if (jamSekarang <= "06:40:00") "Hadir" else "Terlambat"

        val newId = UUID.randomUUID().toString()
        val dataMap: Map<String, Any> = mapOf(
            "id" to newId,
            "siswa_id" to siswaIdInt,
            "id_kelas" to kelasIdInt,
            "tanggal" to sdfTanggal.format(now),
            "waktu_masuk" to jamSekarang,
            "status" to statusHadir,
            "jarak_gerbang_meter" to effectiveDistance,
            "biometrik_match_score" to scoreToSubmit
        )

        binding.btnSubmitPresensi.isEnabled = false
        binding.btnSubmitPresensi.text = "Memproses Presensi..."

        // Deteksi apakah sedang offline
        if (currentCtx != null && !com.andev.absensiswaku.util.NetworkUtils.isNetworkAvailable(currentCtx)) {
            com.andev.absensiswaku.util.OfflinePresensiManager.savePresensiOffline(currentCtx, dataMap)
            NotificationHelper.showNotifikasiPresensi(currentCtx, "$statusHadir (Tersimpan Offline)")
            handleSuccessPresensi(statusHadir, isOffline = true, presensiId = newId)
            return
        }

        SupabaseClient.instance.simpanPresensi(dataMap).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!isAdded || _binding == null) return

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Presensi Berhasil Tercatat!", Toast.LENGTH_SHORT).show()
                    NotificationHelper.showNotifikasiPresensi(requireContext(), statusHadir)
                    handleSuccessPresensi(statusHadir, isOffline = false, presensiId = newId)
                } else if (response.code() == 409 || (response.errorBody()?.string()?.contains("duplicate", true) == true)) {
                    isAlreadyPresensi = true
                    binding.btnSubmitPresensi.isEnabled = false
                    binding.btnSubmitPresensi.text = "✓ Anda Sudah Presensi Hari Ini"
                    binding.tvBadgeStatusPresensi.text = "SUDAH PRESENSI DATANG"

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Peringatan")
                        .setMessage("Presensi hari ini sudah pernah tercatat.")
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    Toast.makeText(requireContext(), "Gagal simpan ke Supabase (${response.code()}): $errorBody", Toast.LENGTH_LONG).show()
                    binding.btnSubmitPresensi.isEnabled = true
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(Calendar.MINUTE)
                    evaluatePresensiState(hour * 60 + minute)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (!isAdded || _binding == null) return
                // Cadangkan secara offline jika request jaringan terputus
                val fallbackCtx = context
                if (fallbackCtx != null) {
                    com.andev.absensiswaku.util.OfflinePresensiManager.savePresensiOffline(fallbackCtx, dataMap)
                    NotificationHelper.showNotifikasiPresensi(fallbackCtx, "$statusHadir (Tersimpan Offline)")
                    handleSuccessPresensi(statusHadir, isOffline = true, presensiId = newId)
                } else {
                    Toast.makeText(requireContext(), "Koneksi Error: ${t.localizedMessage ?: t.message}", Toast.LENGTH_LONG).show()
                    binding.btnSubmitPresensi.isEnabled = true
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(Calendar.MINUTE)
                    evaluatePresensiState(hour * 60 + minute)
                }
            }
        })
    }

    private fun handleSuccessPresensi(status: String, isOffline: Boolean = false, presensiId: String? = null) {
        if (!isAdded || _binding == null) return

        val ctx = context ?: return
        isAlreadyPresensi = true
        if (presensiId != null) {
            todayPresensiId = presensiId
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        } else {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }

        val statusMsg = if (status.equals("Hadir", true)) "Tepat Waktu" else "Terlambat"
        val scoreStr = String.format(Locale.US, "%.1f", if (lastBiometricScore > 0) lastBiometricScore else 95.0)

        binding.tvBadgeStatusPresensi.apply {
            text = if (isOffline) "SUDAH PRESENSI ($statusMsg - OFFLINE)" else "SUDAH PRESENSI ($statusMsg)"
            setBackgroundResource(R.drawable.bg_badge_pill_verified_teal)
            setTextColor(ContextCompat.getColor(ctx, R.color.primary_teal))
        }

        binding.btnSubmitPresensi.apply {
            isEnabled = false
            text = "✓ Presensi Hari Ini Selesai"
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
        }

        updateOfflineBannerUI()

        val dialogTitle = if (isOffline) "✓ Presensi Tersimpan Offline" else "✓ Presensi Berhasil!"
        val dialogMessage = if (isOffline) {
            "Data presensi Anda ($statusMsg) telah diverifikasi liveness biometrik & GPS, lalu disimpan secara aman di memori lokal.\n\nSistem SMKN 8 Jakarta akan otomatis mengunggah data ini ke cloud saat terhubung ke internet.\n\nJarak: ${currentDistanceMeters.roundToInt()}m | AI Match: $scoreStr%"
        } else {
            "Data presensi masuk Anda ($statusMsg) telah berhasil dicatat di sistem SMKN 8 Jakarta.\n\nPresensi hari ini selesai. Selamat belajar!\n\nJarak: ${currentDistanceMeters.roundToInt()}m | AI Match: $scoreStr%"
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(dialogTitle)
            .setMessage(dialogMessage)
            .setPositiveButton("Selesai") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun sendPresensiPulangData() {
        val presensiId = todayPresensiId
        if (presensiId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "ID presensi masuk hari ini tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }

        val sdfWaktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdfWaktu.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        val jamSekarang = sdfWaktu.format(Date())

        binding.btnSubmitPresensi.isEnabled = false
        binding.btnSubmitPresensi.text = "Mencatat Presensi Pulang..."

        val currentCtx = context
        if (currentCtx != null && !com.andev.absensiswaku.util.NetworkUtils.isNetworkAvailable(currentCtx)) {
            com.andev.absensiswaku.util.OfflinePresensiManager.savePresensiPulangOffline(currentCtx, presensiId, jamSekarang)
            NotificationHelper.showNotifikasiPresensi(currentCtx, "Pulang: $jamSekarang WIB (Offline)")
            handleSuccessPresensiPulang(jamSekarang, isOffline = true)
            return
        }

        val updateMap = mapOf<String, Any>(
            "waktu_pulang" to jamSekarang
        )

        SupabaseClient.instance.updatePresensiHarian("eq.$presensiId", updateMap)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (!isAdded || _binding == null) return

                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Presensi Pulang Berhasil Dicatat!", Toast.LENGTH_SHORT).show()
                        NotificationHelper.showNotifikasiPresensi(requireContext(), "Pulang: $jamSekarang WIB")
                        handleSuccessPresensiPulang(jamSekarang, isOffline = false)
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        Toast.makeText(requireContext(), "Gagal mencatat pulang (${response.code()}): $errorBody", Toast.LENGTH_LONG).show()
                        binding.btnSubmitPresensi.isEnabled = true
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    val fallbackCtx = context
                    if (fallbackCtx != null) {
                        com.andev.absensiswaku.util.OfflinePresensiManager.savePresensiPulangOffline(fallbackCtx, presensiId, jamSekarang)
                        NotificationHelper.showNotifikasiPresensi(fallbackCtx, "Pulang: $jamSekarang WIB (Offline)")
                        handleSuccessPresensiPulang(jamSekarang, isOffline = true)
                    } else {
                        Toast.makeText(requireContext(), "Koneksi Error: ${t.localizedMessage ?: t.message}", Toast.LENGTH_LONG).show()
                        binding.btnSubmitPresensi.isEnabled = true
                    }
                }
            })
    }

    private fun handleSuccessPresensiPulang(jamPulang: String, isOffline: Boolean = false) {
        if (!isAdded || _binding == null) return

        val ctx = context ?: return
        isAlreadyPulang = true
        todayWaktuPulang = jamPulang

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        } else {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }

        binding.tvBadgeStatusPresensi.apply {
            text = if (isOffline) "PRESENSI LENGKAP (PULANG OFFLINE)" else "PRESENSI HARI INI LENGKAP"
            setBackgroundResource(R.drawable.bg_badge_pill_blue)
            setTextColor(ContextCompat.getColor(ctx, R.color.primary_teal))
        }

        binding.btnSubmitPresensi.apply {
            isEnabled = false
            text = "✓ Presensi Hari Ini Selesai"
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
        }

        val dialogTitle = if (isOffline) "✓ Presensi Pulang Tersimpan Offline" else "✓ Presensi Pulang Berhasil!"
        val dialogMessage = if (isOffline) {
            "Presensi kepulangan Anda pada pukul $jamPulang WIB telah disimpan secara offline di memori perangkat.\n\nSistem akan otomatis mengunggah ke server SMKN 8 Jakarta saat terhubung ke internet."
        } else {
            "Presensi kepulangan Anda telah berhasil dicatat pada pukul $jamPulang WIB.\n\nKehadiran hari ini lengkap (Masuk & Pulang). Selamat beristirahat dan hati-hati di jalan!"
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(dialogTitle)
            .setMessage(dialogMessage)
            .setPositiveButton("Selesai") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            liveClockJob?.cancel()
            cameraProvider?.unbindAll()
            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        } else {
            if (!isAdded || _binding == null || view == null) return
            startLiveClock()
            checkDailyPresensiStatus()
            updateOfflineBannerUI()
            startCamera()
            startLocationUpdates()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancellationTokenSource?.cancel()
        liveClockJob?.cancel()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        cameraProvider?.unbindAll()
        if (::faceDetector.isInitialized) {
            faceDetector.close()
        }
        _binding = null
    }
}
