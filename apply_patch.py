path = r"app/src/main/java/com/andev/absensiswaku/ui/presensi/PresensiFragment.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. evaluatePresensiState
s1 = """            !isFaceInsideCircle -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = false
                    text = "\u2715 Wajah Di Luar Radius Lingkaran Panduan"
                    val ctx = context ?: return@apply
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.btn_disabled_bg))
                }
            }
            !inRadius -> {"""

r1 = """            !isFaceInsideCircle -> {
                binding.btnSubmitPresensi.apply {
                    isEnabled = false
                    text = "\u2715 Wajah Di Luar Radius Lingkaran Panduan"
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
            !inRadius -> {"""
assert s1 in content, "s1 not found"
content = content.replace(s1, r1, 1)

# 2. processImageFrameForFaceDetection
s2 = """                    lastBiometricScore = (baseScore + eyeBonus + smileBonus).coerceIn(85.0, 99.2)
                    val scoreStr = String.format(Locale.US, "%.1f", lastBiometricScore)

                    if (isFaceInsideCircle) {
                        binding.tvStatusKameraChip.text = "\u2022 Kamera Aktif \u2022 Wajah Terdeteksi"
                        updateFaceWarningBadge(FaceWarningBadgeState.MATCHED, scoreStr)
                    } else {
                        binding.tvStatusKameraChip.text = "\u2022 Kamera Aktif \u2022 Wajah Di Luar Radius"
                        updateFaceWarningBadge(FaceWarningBadgeState.OUTSIDE_CIRCLE)
                    }
                } else {
                    isFaceDetected = false
                    isFaceInsideCircle = false
                    lastBiometricScore = 0.0

                    binding.tvStatusKameraChip.text = "\u2022 Kamera Aktif \u2022 Wajah Tidak Terdeteksi"
                    updateFaceWarningBadge(FaceWarningBadgeState.NOT_DETECTED)
                }"""

r2 = """                    lastBiometricScore = (baseScore + eyeBonus + smileBonus).coerceIn(85.0, 99.2)
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
                            binding.tvStatusKameraChip.text = "\u2022 Kamera Aktif \u2022 Wajah Terverifikasi (Live)"
                            updateFaceWarningBadge(FaceWarningBadgeState.MATCHED, "$scoreStr% (Live \u2713)")
                        } else {
                            binding.tvStatusKameraChip.text = "\u2022 Kedipkan Mata Untuk Verifikasi \u2022"
                            updateFaceWarningBadge(FaceWarningBadgeState.NEED_BLINK, scoreStr)
                        }
                    } else {
                        hasEyesClosed = false
                        isLivenessVerified = false
                        binding.tvStatusKameraChip.text = "\u2022 Kamera Aktif \u2022 Wajah Di Luar Radius"
                        updateFaceWarningBadge(FaceWarningBadgeState.OUTSIDE_CIRCLE)
                    }
                } else {
                    isFaceDetected = false
                    isFaceInsideCircle = false
                    lastBiometricScore = 0.0
                    hasEyesClosed = false
                    isLivenessVerified = false

                    binding.tvStatusKameraChip.text = "\u2022 Kamera Aktif \u2022 Wajah Tidak Terdeteksi"
                    updateFaceWarningBadge(FaceWarningBadgeState.NOT_DETECTED)
                }"""
assert s2 in content, "s2 not found"
content = content.replace(s2, r2, 1)

# 3. FaceWarningBadgeState
s3 = """    private enum class FaceWarningBadgeState {
        MATCHED,
        OUTSIDE_CIRCLE,
        NOT_DETECTED
    }"""
r3 = """    private enum class FaceWarningBadgeState {
        MATCHED,
        NEED_BLINK,
        OUTSIDE_CIRCLE,
        NOT_DETECTED
    }"""
assert s3 in content, "s3 not found"
content = content.replace(s3, r3, 1)

# 4. updateFaceWarningBadge
s4 = """        when (state) {
            FaceWarningBadgeState.MATCHED -> {
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
                binding.tvStatusWajah.text = "AI Match $scoreStr%"
            }
            FaceWarningBadgeState.OUTSIDE_CIRCLE -> {
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
        }"""

r4 = """        when (state) {
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
        }"""
assert s4 in content, "s4 not found"
content = content.replace(s4, r4, 1)

# 5. setupSubmitButton
s5 = """            if (!isFaceInsideCircle) {
                Toast.makeText(ctx, "Presensi ditolak: Posisikan wajah Anda tepat di dalam lingkaran panduan hijau!", Toast.LENGTH_LONG).show()
                return@applyBounceEffect
            }"""
r5 = """            if (!isFaceInsideCircle) {
                Toast.makeText(ctx, "Presensi ditolak: Posisikan wajah Anda tepat di dalam lingkaran panduan hijau!", Toast.LENGTH_LONG).show()
                return@applyBounceEffect
            }

            if (!isLivenessVerified) {
                Toast.makeText(ctx, "Presensi ditolak: Silakan kedipkan mata untuk verifikasi biometrik asli!", Toast.LENGTH_LONG).show()
                return@applyBounceEffect
            }"""
assert s5 in content, "s5 not found"
content = content.replace(s5, r5, 1)

# 6. handleSuccessPresensi
s6 = """        val ctx = context ?: return
        isAlreadyPresensi = true"""
r6 = """        val ctx = context ?: return
        isAlreadyPresensi = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        } else {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }"""
assert s6 in content, "s6 not found"
content = content.replace(s6, r6, 1)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("SUCCESSFULLY APPLIED ALL ENHANCEMENTS")
