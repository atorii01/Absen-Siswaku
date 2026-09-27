# 🎓 Presensi Siswa & Guru SMKN 8 Jakarta

<p align="center">
  <img src="app/src/main/res/mipmap-hdpi/logo_smkn8_foreground.webp" width="128" height="128" alt="Logo SMKN 8 Jakarta" style="border-radius: 28px;" />
</p>

<p align="center">
  <b>Sistem Informasi Presensi Berbasis Geofencing, Biometrik Wajah, & Sinkronisasi Multi-Role Terpadu</b><br>
  <i>SMK Negeri 8 Jakarta • Portal Terpadu Absensi Mandiri & Manajemen Akademik</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Platform" />
  <img src="https://img.shields.io/badge/Min%20SDK-24%20(Nougat)-blue?style=for-the-badge" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target%20SDK-37%20(Android%2015+)-00685F?style=for-the-badge" alt="Target SDK" />
  <img src="https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 100%" />
  <img src="https://img.shields.io/badge/UI-Material%20Design%203-0B1C30?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material Design 3" />
  <img src="https://img.shields.io/badge/Backend-Supabase%20PostgreSQL-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white" alt="Supabase" />
</p>

<p align="center">
  <a href="https://drive.google.com/drive/folders/1LcCKqBAG9QNBFVJOs-0Wk0zwq5zIJxVD?usp=sharing" target="_blank">
    <img src="https://img.shields.io/badge/Unduh%20Langsung%20APK-Google%20Drive-4285F4?style=for-the-badge&logo=googledrive&logoColor=white" alt="Download APK Google Drive" />
  </a>
  <a href="https://drive.google.com/drive/folders/1LcCKqBAG9QNBFVJOs-0Wk0zwq5zIJxVD?usp=sharing" target="_blank">
    <img src="https://img.shields.io/badge/Versi%20Terbaru-Siap%20Pasang%20(APK)-00685F?style=for-the-badge&logo=android&logoColor=white" alt="Versi Terbaru Siap Pasang" />
  </a>
</p>

---

## 📌 Ringkasan Eksekutif & Sorotan Utama

**Presensi Siswa SMKN 8 Jakarta** adalah aplikasi Android *native* modern yang dirancang khusus untuk memfasilitasi pencatatan presensi harian secara akurat, terverifikasi, dan transparan. Dibangun dengan memadukan validasi radius lokasi geografis (*geofencing*) dan deteksi biometrik wajah *real-time*, aplikasi ini mengeliminasi celah kecurangan presensi (*fake GPS/spoofing* maupun titip absensi).

Aplikasi mengadopsi arsitektur **Role-Based Access Control (RBAC)** berkinerja tinggi, menghubungkan ekosistem sekolah dalam satu basis data terintegrasi: mulai dari siswa, wali kelas, guru mata pelajaran, hingga administrator sistem.

### 🌟 Fitur Utama Unggulan:
* **🛡️ Dual-Verification Anti-Fraud:** Validasi radius GPS presisi tinggi (lingkar geofencing 50 meter dari gerbang sekolah) dipadukan dengan *Liveness Face Detection* via CameraX & Google ML Kit.
* **⚡ Kapsul Tunggal Role Switcher:** Antarmuka login bergaya modern dengan sistem *Single Capsule Toggle* bebas *glitch*, dilengkapi fitur *Remember Me* untuk prefill kredensial instan.
* **📑 Paperless Leave Approval:** Pengajuan izin/sakit mandiri oleh siswa disertai unggah bukti surat dokter/wali, dengan antrean verifikasi persetujuan (*Accept/Reject*) oleh Wali Kelas.
* **📊 Multi-Format Reporting Hub:** Kemampuan ekspor rekap absensi ke format **PDF Lembar Resmi**, **XLSX Standar Dapodik**, dan **CSV**, tersimpan langsung ke folder *Downloads* publik melalui Android `MediaStore API`.
* **🌓 Tema Adaptif (Dark Mode):** Desain visual berbasis Material 3 dengan palet warna khas *Deep Teal* (`#00685F`) yang mendukung mode terang dan gelap demi kenyamanan mata pengguna.

---

## 👥 Fitur Utama Berdasarkan Peran Pengguna (RBAC)

Aplikasi memiliki 4 portal akses spesifik dengan hak otorisasi yang terisolasi secara aman:

```
                  ┌────────────────────────────────────────┐
                  │          Splash Screen & Auth          │
                  │   (LoginActivity + Prefill Session)    │
                  └───────────────────┬────────────────────┘
                                      │
        ┌───────────────┬─────────────┴─────────────┬────────────────┐
        ▼               ▼                           ▼                ▼
┌──────────────┐ ┌──────────────┐            ┌──────────────┐ ┌──────────────┐
│ Portal Siswa │ │ Portal Walas │            │  Guru Mapel  │ │ Super Admin  │
│(MainActivity)│ │ (WalasMain)  │            │  (GuruMain)  │ │ (AdminMain)  │
└──────────────┘ └──────────────┘            └──────────────┘ └──────────────┘
```

### 1. 🎓 Portal Siswa (`MainActivity`)
* **Presensi Kamera & GPS:** Tombol absensi masuk/pulang aktif hanya jika koordinat GPS siswa berada di dalam radius gerbang sekolah dan wajah terdeteksi tegak lurus pada bingkai panduan oval.
* **Dasbor Disiplin Harian:** Informasi persentase kehadiran bulanan, total hadir tepat waktu, jam keterlambatan, izin, sakit, dan alpa.
* **Riwayat Kehadiran Mandiri:** Kalender dan daftar histori presensi harian lengkap dengan status validasi, catatan verifikasi, dan tombol **Unduh Rekap PDF Pribadi**.
* **Form Pengajuan Izin/Sakit:** Pengajuan permohonan ketidakhadiran dengan rentang tanggal, keterangan alasan, serta pengambilan foto surat bukti otentik.
* **Profil Siswa:** Detail NISN, Rombongan Belajar (Rombel), nama Wali Kelas, preferensi tema gelap, dan kendali keluar akun (*Logout*).

### 2. 🪪 Portal Wali Kelas (`WalasMainActivity`)
* **Pemantau Real-Time Rombel Binaan:** Monitoring kehadiran live untuk kelas yang diampu (misal: *XII AKL 1*), menampilkan persentase siswa hadir hari ini secara visual.
* **Antrean Verifikasi Surat Izin:** Daftar permohonan izin/sakit siswa yang menunggu tindak lanjut wali kelas, dilengkapi pratinjau surat bukti dan aksi satu-ketukan (*Terima / Tolak*).
* **Direktori Master Siswa Kelas:** Buku induk mini kelas berisi daftar nomor absen, NISN, nama lengkap, kelengkapan data, dan tautan panggilan cepat ke wali murid.
* **Cetak Lembar Rekap PDF Resmi:** Generator dokumen PDF siap cetak untuk laporan bulanan/semesteran ke tata usaha, lengkap dengan kolom tanda tangan wali kelas.

### 3. 📚 Portal Guru Mata Pelajaran (`GuruMainActivity`)
* **Mode Pantau Lintas Rombel (Read-Only):** Memantau absensi siswa pada seluruh 10 rombongan belajar Kelas XII (AKL, MPLB, PM, PPLG, TJKT, DKV) tanpa sekat perwalian.
* **Audit Kehadiran Jam Pelajaran (KBM):** Memastikan siswa yang mengikuti kelas tatap muka telah terdata hadir secara sah di sistem sekolah pada hari yang bersangkutan.
* **Rekapitulasi Kehadiran Mapel:** Analisis kehadiran siswa per periode mata pelajaran yang diampu.

### 4. ⚙️ Portal Super Admin (`AdminMainActivity`)
* **Audit Presensi Lintas Sekolah:** Pemantauan menyeluruh tingkat kehadiran seluruh rombel, rekapitulasi keterlambatan, dan riwayat presensi guru & tendik.
* **Pusat Ekspor Laporan Multi-Format:** Ekspor basis data presensi ke berbagai format:
  * **PDF:** Format cetak resmi berlogo sekolah untuk pelaporan dinas/kepala sekolah.
  * **XLSX (Excel):** Format tabel dinamis kolom terstruktur untuk sinkronisasi Dapodik.
  * **CSV:** Format *raw data* ringkas untuk integrasi basis data eksternal.
* **Manajemen Master Data:** Tambah, mutasi kelas siswa, perbarui status akun, dan reset PIN presensi siswa yang terkendala.
* **Kalibrasi Perimeter Geofencing:** Pengaturan titik koordinat pusat gerbang sekolah (Latitude/Longitude) dan toleransi radius meter secara fleksibel.

---

## 🛠️ Arsitektur Teknologi & Dependensi

Proyek ini dibangun menggunakan arsitektur **Native Android ViewBinding** dengan prinsip *Single Source of Truth* dan konsumsi REST API yang terisolasi rapi:

### Komponen Inti & Dependensi Gradle

| Kategori | Teknologi / Pustaka | Versi | Peran dalam Aplikasi |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | `2.1.0` | Bahasa pemrograman utama berparadigma OOP + Fungsional |
| **UI System** | Material Components Android | `1.14.0` | Material Design 3 tokens, CardView, Button, TextFields |
| **View Binding** | Android Gradle ViewBinding | `Enabled` | Pengikatan layout XML tipe-aman tanpa `findViewById` |
| **Networking** | Square Retrofit 2 | `2.11.0` | Klien REST API berbasis deklaratif |
| **JSON Parser** | Google Gson Converter | `2.11.0` | Serialisasi dan deserialisasi data model Supabase |
| **HTTP Logging** | OkHttp Logging Interceptor | `4.12.0` | Debugging lalu lintas request/response HTTP |
| **Camera Engine** | AndroidX CameraX (Core/Camera2) | `1.4.1` | Pengambilan citra selfie presensi perangkat keras kamera |
| **Biometrics** | Google ML Kit Face Detection | `16.1.7` | Analisis orientasi dan kontur wajah secara *on-device* |
| **Location** | Google Play Services Location | `21.3.0` | Penyedia koordinat lintang/bujur (FusedLocationProviderClient) |
| **Document Engine**| Android Native `PdfDocument` | SDK Built-in | Pembuatan berkas PDF dinamis berbasis Android Canvas |
| **File Storage** | Android `MediaStore API` | SDK Built-in | Penyimpanan berkas unduhan ke memori publik tanpa izin berbahaya |

---

## 📁 Struktur Direktori Proyek

```text
app/src/main/
├── AndroidManifest.xml                # Pendaftaran aktivitas, izin GPS, kamera, & launcher
├── java/com/andev/absensiswaku/
│   ├── SplashActivity.kt             # Layar pembuka beranimasi lembut (Launcher)
│   ├── LoginActivity.kt              # Layar autentikasi multi-role & smart prefill
│   ├── MainActivity.kt               # Shell penampung portal siswa
│   ├── WalasMainActivity.kt          # Shell penampung portal wali kelas
│   ├── GuruMainActivity.kt           # Shell penampung portal guru mapel
│   ├── AdminMainActivity.kt          # Shell penampung portal super admin
│   ├── data/
│   │   ├── network/
│   │   │   ├── ApiClient.kt          # Inisialisasi HTTP client umum
│   │   │   ├── SupabaseClient.kt     # Konfigurasi Retrofit & Header Token Supabase
│   │   │   ├── SupabaseService.kt    # Definisi kontrak query RESTful PostgREST
│   │   │   └── *Response.kt / Model  # DTO (Data Transfer Objects) respons API
│   │   └── pref/
│   │       └── SessionManager.kt     # Pengelola sesi login aktif di SharedPreferences
│   └── ui/
│       ├── presensi/                 # Presensi kamera selfie & kalkulator radius GPS
│       ├── riwayat/                  # Riwayat kehadiran & eksportir PDF mandiri siswa
│       ├── pengajuan/                # Formulir izin/sakit & pengunggah surat bukti
│       ├── setelan/                  # Pengaturan akun, dark mode, dan logout siswa
│       ├── walas/                    # Monitoring kelas, verifikasi izin, rekap walas
│       ├── guru/                     # Mode pemantau KBM lintas rombel & rekap mapel
│       ├── admin/                    # Master data, kalibrasi gerbang, & ExportReportHelper
│       └── custom/
│           └── BiometricOverlayView.kt# Panduan oval deteksi wajah kamera presensi
└── res/
    ├── anim/                         # Animasi transisi teks & logo splash screen
    ├── drawable/                     # Vektor grafis monokrom MD3 & latar belakang kartu
    ├── layout/                       # Berkas tata letak XML terikat ViewBinding
    └── values/                       # Definisi tema, warna Deep Teal, dan string
```

---

## 🚀 Panduan Instalasi & Menjalankan Proyek

### 📥 Opsi 1: Unduh & Pasang Langsung APK (Siap Pakai / Non-Developer)
Bagi siswa, guru, wali murid, atau penilai yang ingin langsung menguji aplikasi di smartphone Android tanpa perlu memasang Android Studio:
1. **Akses Tautan Berkas APK:**
   👉 [**Download APK Presensi SMKN 8 Jakarta (Google Drive)**](https://drive.google.com/drive/folders/1LcCKqBAG9QNBFVJOs-0Wk0zwq5zIJxVD?usp=sharing)
2. **Unduh Berkas APK:** Pilih berkas APK rilis terbaru di dalam folder Google Drive tersebut lalu simpan ke penyimpanan ponsel.
3. **Pemasangan di Ponsel Android:**
   * Buka berkas `.apk` yang baru diunduh.
   * Jika sistem menampilkan peringatan keamanan, aktifkan opsi **"Izinkan Penginstalan dari Sumber Ini / Sumber Tidak Dikenal"** (*Install Unknown Apps*).
   * Ketuk **Instal** dan tunggu hingga proses selesai.
4. **Izin Perangkat yang Diperlukan:**
   * **Kamera:** Wajib diizinkan untuk verifikasi biometrik wajah (*Face Detection*).
   * **Lokasi (GPS Presisi):** Wajib diizinkan dengan opsi *Tepat/Precise* untuk validasi radius perimeter gerbang sekolah (*Geofencing*).
5. **Autentikasi & Masuk Akun:**
   * Buka aplikasi, pilih tab peran yang sesuai (*Siswa* atau *Guru & Tendik*), lalu masuk menggunakan kredensial Anda.

> [!WARNING]
> **PEMBERITAHUAN PRIVASI & KEAMANAN KREDENSIAL:**
> Demi mematuhi prinsip kerahasiaan data dan UU Pelindungan Data Pribadi (UU PDP), **KATA SANDI & KREDENSIAL ASLI SELURUH SISWA/GURU SENGAJA TIDAK DIBAGIKAN** di repositori publik ini.
> * **Siswa & Guru Resmi SMKN 8 Jakarta:** Silakan gunakan kredensial resmi yang telah dibagikan secara tertutup oleh Tim Kurikulum / Administrator IT Sekolah.
> * **Peninjau / Pengembang Eksternal:** Gunakan akun simulasi pengujian pada tabel demo di bawah.

---

### 💻 Opsi 2: Kompilasi Mandiri dari Kode Sumber (Untuk Developer)

#### 1. Prasyarat Lingkungan Pengembangan
* **Android Studio:** Ladybug (2024.2.1) / Koala / Hedgehog atau versi yang lebih baru.
* **Java Development Kit (JDK):** JDK 11 atau JDK 17 (disarankan JDK 17 LTS).
* **Android SDK:** Platform SDK 34 s/d 37.
* **Perangkat Pengujian:** Ponsel Android fisik dengan fitur Kamera & GPS aktif (disarankan) atau Emulator Android API Level 26+ yang telah diatur koordinat lokasinya.

#### 2. Kloning Repositori
Buka terminal dan jalankan perintah:
```bash
git clone https://github.com/atorii01/Absen-Siswaku.git
cd Absen-Siswaku
```

#### 3. Konfigurasi Kredensial Backend (Supabase)
Buka berkas `SupabaseClient.kt` di direktori:
`app/src/main/java/com/andev/absensiswaku/data/network/SupabaseClient.kt`

Sesuaikan URL endpoint dan Anon Key proyek Supabase Anda:
```kotlin
object SupabaseClient {
    // Ganti dengan URL Project Supabase Anda sendiri:
    const val BASE_URL = "https://PASTE_YOUR_SUPABASE_PROJECT_ID.supabase.co/rest/v1/"

    // Ganti dengan API Anon Key proyek Supabase Anda:
    private const val ANON_KEY = "PASTE_YOUR_SUPABASE_ANON_KEY"
    ...
}
```

> **Keamanan Data:** Jangan pernah melakukan *commit* atau mempublikasikan Anon Key produksi Anda ke repositori publik.

#### 4. Sinkronisasi & Kompilasi
1. Buka Android Studio, pilih **Open** lalu arahkan ke folder repositori `Absen-Siswaku`.
2. Tunggu proses **Gradle Sync** selesai mengunduh seluruh dependensi.
3. Jalankan verifikasi kompilasi melalui terminal Android Studio:
   ```powershell
   # Pada sistem operasi Windows (PowerShell):
   .\gradlew.bat compileDebugKotlin
   ```
4. Pasang aplikasi ke perangkat/emulator:
   * Tekan tombol hijau **Run 'app'** (`Shift + F10`) di toolbar Android Studio, atau
   * Jalankan `.\gradlew.bat installDebug`.

---

## 🧪 Akun Uji Coba Demo (Simulasi Lingkungan Uji)

> [!IMPORTANT]
> **Kebijakan Keamanan Akun & Perlindungan Data:**
> Data pada tabel berikut adalah **data tiruan (mock/dummy data)** yang disediakan semata-mata untuk menguji alur logika aplikasi, validasi form, dan pergantian peran (*role switcher*).
> **Kata sandi, NISN, dan data akun riil SMKN 8 Jakarta TIDAK DIBOCORKAN atau dicantumkan di sini** demi menjaga integritas data sekolah dan privasi seluruh warga belajar.

Untuk keperluan peninjauan dan pengujian fitur tanpa menggunakan identitas nyata, gunakan kredensial tiruan berikut:

| Role Pengguna | Kategori Tab | ID Masuk / Username / NISN (Demo) | Kata Sandi / PIN (Demo) | Cakupan Hak Akses |
| :--- | :--- | :--- | :--- | :--- |
| **Siswa Demo** | Tab *Siswa* | `0081234567` | `12052007` | Presensi wajah/GPS, form izin sakit, riwayat PDF siswa |
| **Wali Kelas** | Tab *Guru & Tendik* | `walas_akl1` | `Walas8#2026` | Monitoring XII AKL 1, verifikasi acc izin, rekap kelas |
| **Guru Mapel** | Tab *Guru & Tendik* | `guru_matematika` | `Guru8#2026` | Mode pantau Read-Only seluruh 10 rombel Kelas XII |
| **Super Admin** | Tab *Guru & Tendik* | `admin_utama` | `Admin8#2026` | Audit sekolah, ekspor PDF/XLSX/CSV, kalibrasi GPS |

> **Catatan Uji Presensi:** Jika menguji menggunakan Emulator, pastikan titik lokasi GPS pada panel *Extended Controls (Location)* diset ke koordinat sekitar lingkungan SMKN 8 Jakarta agar tidak terkena peringatan *Di Luar Jangkauan Radius Gerbang*.

---

## 📄 Lisensi & Hak Cipta

Seluruh kode sumber dan aset antarmuka dalam repositori ini dikembangkan untuk kebutuhan operasional presensi digital terpadu **SMK Negeri 8 Jakarta**.

*Dikembangkan dengan standar arsitektur bersih, performa tinggi, dan komitmen perlindungan data oleh Tim Pengembang Android Studio.*
