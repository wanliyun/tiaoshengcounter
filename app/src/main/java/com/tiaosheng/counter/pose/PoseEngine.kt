package com.tiaosheng.counter.pose

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.nio.ByteBuffer

class PoseEngine(private val lifecycleOwner: LifecycleOwner) {

    data class Landmark(
        val x: Float,
        val y: Float,
        val z: Float,
        val visibility: Float
    )

    data class PoseResult(
        val landmarks: List<Landmark>,
        val timestampMs: Long
    )

    private var poseLandmarker: PoseLandmarker? = null
    private var initialized = false

    fun initialize(context: Context): Result<Unit> {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_lite.task")
                .setDelegate(Delegate.GPU)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setResultListener { result: PoseLandmarkerResult, _ ->
                    onResult(result)
                }
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            initialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            // Fallback to CPU if GPU unavailable
            try {
                val cpuOptions = BaseOptions.builder()
                    .setModelAssetPath("pose_landmarker_lite.task")
                    .setDelegate(Delegate.CPU)
                    .build()

                val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(cpuOptions)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f)
                    .setResultListener { result: PoseLandmarkerResult, _ ->
                        onResult(result)
                    }
                    .build()

                poseLandmarker = PoseLandmarker.createFromOptions(context, options)
                initialized = true
                Result.success(Unit)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    private var latestResult: PoseResult? = null
    private var resultCallback: ((PoseResult) -> Unit)? = null
    private var lastProcessedTimestamp: Long = 0

    fun processFrame(imageProxy: androidx.camera.core.ImageProxy, callback: (PoseResult) -> Unit) {
        if (!initialized) return

        resultCallback = callback

        val mpImage = imageProxyToMPImage(imageProxy)
        poseLandmarker?.detectAsync(mpImage, imageProxy.imageInfo.timestampMs)
    }

    private fun onResult(result: PoseLandmarkerResult) {
        val landmarks = result.landmarks().firstOrNull()?.map { lm ->
            Landmark(
                x = lm.x(),
                y = lm.y(),
                z = lm.z(),
                visibility = lm.visibility().orElse(0f)
            )
        } ?: emptyList()

        val poseResult = PoseResult(
            landmarks = landmarks,
            timestampMs = result.timestampMs()
        )

        synchronized(this) {
            latestResult = poseResult
            if (landmarks.isNotEmpty()) {
                lastProcessedTimestamp = result.timestampMs()
            }
        }

        resultCallback?.invoke(poseResult)
    }

    fun getLatestResult(): PoseResult? = synchronized(this) {
        latestResult
    }

    fun getLastProcessedTimestamp(): Long = lastProcessedTimestamp

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        initialized = false
    }

    private fun imageProxyToMPImage(imageProxy: androidx.camera.core.ImageProxy): MPImage {
        val bitmap = imageProxyToBitmap(imageProxy)
        return BitmapImageBuilder(bitmap).build()
    }

    private fun imageProxyToBitmap(imageProxy: androidx.camera.core.ImageProxy): android.graphics.Bitmap {
        val yBuffer: ByteBuffer = imageProxy.planes[0].buffer
        val uBuffer: ByteBuffer = imageProxy.planes[1].buffer
        val vBuffer: ByteBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
            80,
            out
        )

        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
