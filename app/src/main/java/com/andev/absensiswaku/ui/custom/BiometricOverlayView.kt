package com.andev.absensiswaku.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BiometricOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40000000") // 25% dark overlay
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val ovalGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34D399")
        style = Paint.Style.STROKE
        strokeWidth = 3f * context.resources.displayMetrics.density
    }

    private val cornerBracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34D399")
        style = Paint.Style.STROKE
        strokeWidth = 5f * context.resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
    }

    private val faceRect = RectF()
    private val cornerPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0 || height <= 0) return

        // Calculate face oval dimensions centered in view
        val ovalWidth = width * 0.65f
        val ovalHeight = height * 0.68f
        val left = (width - ovalWidth) / 2f
        val top = (height - ovalHeight) / 2f
        val right = left + ovalWidth
        val bottom = top + ovalHeight

        faceRect.set(left, top, right, bottom)

        // Draw dim overlay background with cut-out face oval
        val layerId = canvas.saveLayer(0f, 0f, width, height, null)
        canvas.drawRect(0f, 0f, width, height, maskPaint)
        canvas.drawOval(faceRect, clearPaint)
        canvas.restoreToCount(layerId)

        // Draw oval guide line
        canvas.drawOval(faceRect, ovalGuidePaint)

        // Draw 4 corner focus brackets around the face oval box
        val bracketLen = 28f * context.resources.displayMetrics.density
        val margin = 8f * context.resources.displayMetrics.density
        val bLeft = left - margin
        val bTop = top - margin
        val bRight = right + margin
        val bBottom = bottom + margin

        cornerPath.reset()

        // Top-Left Corner
        cornerPath.moveTo(bLeft, bTop + bracketLen)
        cornerPath.lineTo(bLeft, bTop)
        cornerPath.lineTo(bLeft + bracketLen, bTop)

        // Top-Right Corner
        cornerPath.moveTo(bRight - bracketLen, bTop)
        cornerPath.lineTo(bRight, bTop)
        cornerPath.lineTo(bRight, bTop + bracketLen)

        // Bottom-Right Corner
        cornerPath.moveTo(bRight, bBottom - bracketLen)
        cornerPath.lineTo(bRight, bBottom)
        cornerPath.lineTo(bRight - bracketLen, bBottom)

        // Bottom-Left Corner
        cornerPath.moveTo(bLeft + bracketLen, bBottom)
        cornerPath.lineTo(bLeft, bBottom)
        cornerPath.lineTo(bLeft, bBottom - bracketLen)

        canvas.drawPath(cornerPath, cornerBracketPaint)
    }
}
