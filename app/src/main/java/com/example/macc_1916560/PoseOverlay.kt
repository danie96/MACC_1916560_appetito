package com.example.macc_1916560

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class PoseOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var pose: Pose? = null
    private var imageWidth = 0
    private var imageHeight = 0
    private var imageRotation = 0  // used to check if the image is rotated

    private val pointPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 10f
    }

    private val linePaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    fun setPose(pose: Pose, width: Int, height: Int, rotation: Int) {
        this.pose = pose

        // Swap width/height if the image is rotated 90 or 270
        if (rotation == 0 || rotation == 180) {
            this.imageWidth = width
            this.imageHeight = height
        } else {
            this.imageWidth = height
            this.imageHeight = width
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val landmarks = pose?.allPoseLandmarks ?: return


        for (landmark in landmarks) {

            val point = translate(landmark.position.x, landmark.position.y)
            canvas.drawCircle(point.x, point.y, 8f, pointPaint)
        }

        // Draw connecting lines
        drawLine(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawLine(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawLine(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawLine(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawLine(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawLine(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        drawLine(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawLine(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawLine(canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawLine(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
    }

    private fun drawLine(canvas: Canvas, start: Int, end: Int) {
        val pose = pose ?: return
        val startLandmark = pose.getPoseLandmark(start) ?: return
        val endLandmark = pose.getPoseLandmark(end) ?: return

        val start = translate(startLandmark.position.x, startLandmark.position.y)
        val end = translate(endLandmark.position.x, endLandmark.position.y)
        canvas.drawLine(start.x, start.y, end.x, end.y, linePaint)
    }

    private fun translate(x: Float, y: Float): PointF {
        return when (imageRotation) { // round about way to check if the image is rotated
            90 -> PointF(y * width / imageHeight, (imageWidth - x) * height / imageWidth)
            270 -> PointF((imageHeight - y) * width / imageHeight, x * height / imageWidth)
            180 -> PointF((imageWidth - x) * width / imageWidth, (imageHeight - y) * height / imageHeight)
            else -> PointF(x * width / imageWidth, y * height / imageHeight)
        }
    }

}
