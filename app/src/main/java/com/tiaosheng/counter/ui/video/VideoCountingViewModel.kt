package com.tiaosheng.counter.ui.video

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.tiaosheng.counter.counter.JumpDetector
import com.tiaosheng.counter.pose.PoseEngine
import com.tiaosheng.counter.video.VideoFrameProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoCountingViewModel(application: Application) : AndroidViewModel(application) {

    enum class Phase { IDLE, PROCESSING, COMPLETED, ERROR }

    data class VideoUiState(
        val phase: Phase = Phase.IDLE,
        val progress: Float = 0f,
        val count: Int = 0,
        val avgBpm: Float = 0f,
        val durationSeconds: Int = 0,
        val videoName: String = "",
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private var processingJob: Job? = null
    private var processor: VideoFrameProcessor? = null
    private var poseLandmarker: PoseLandmarker? = null

    fun startProcessing(uri: Uri) {
        if (_uiState.value.phase == Phase.PROCESSING) return

        processingJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(phase = Phase.PROCESSING, progress = 0f, count = 0, error = null)

            val context = getApplication<Application>()
            val proc = VideoFrameProcessor(context, uri, targetFps = 15)
            processor = proc
            proc.open()

            val landmarker = createPoseLandmarker(context)
            if (landmarker.isFailure) {
                _uiState.value = _uiState.value.copy(
                    phase = Phase.ERROR,
                    error = "模型初始化失败: ${landmarker.exceptionOrNull()?.message}"
                )
                proc.release()
                return@launch
            }
            poseLandmarker = landmarker.getOrThrow()

            val detector = JumpDetector()
            val durationMs = proc.durationMs
            val durationSec = (durationMs / 1000f)
            val frameIntervalUs = (1_000_000L / 15) // 15fps

            var processedFrames = 0

            try {
                var timeUs = 0L
                while (isActive && timeUs < durationMs * 1000L) {
                    val bitmap = withContext(Dispatchers.IO) {
                        proc.getFrameAtTime(timeUs)
                    }

                    if (bitmap != null) {
                        val result = detectPose(poseLandmarker!!, bitmap)
                        bitmap.recycle()

                        if (result != null) {
                            detector.processFrame(result)
                        }
                    }

                    processedFrames++
                    timeUs += frameIntervalUs

                    if (processedFrames % 10 == 0) {
                        val progress = (timeUs.toFloat() / (durationMs * 1000L)).coerceAtMost(1f)
                        val jumpCount = detector.getCount()
                        val elapsedMs = if (jumpCount > 0) (timeUs / 1000L) else 0L
                        val bpm = if (durationSec > 0 && jumpCount > 0) {
                            jumpCount / (elapsedMs / 60000f).coerceAtLeast(0.01f)
                        } else 0f

                        _uiState.value = _uiState.value.copy(
                            progress = progress,
                            count = jumpCount,
                            avgBpm = bpm,
                            durationSeconds = (elapsedMs / 1000).toInt()
                        )
                    }
                }

                val finalCount = detector.getCount()
                val totalSec = proc.durationMs / 1000
                val finalBpm = if (totalSec > 0) finalCount * 60f / totalSec else 0f

                _uiState.value = _uiState.value.copy(
                    phase = Phase.COMPLETED,
                    progress = 1f,
                    count = finalCount,
                    avgBpm = finalBpm,
                    durationSeconds = totalSec.toInt()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    phase = Phase.ERROR,
                    error = "处理出错: ${e.message}"
                )
            } finally {
                proc.release()
                poseLandmarker?.close()
            }
        }
    }

    fun cancel() {
        processingJob?.cancel()
        processor?.release()
        poseLandmarker?.close()
        _uiState.value = _uiState.value.copy(phase = Phase.IDLE)
    }

    fun reset() {
        cancel()
        _uiState.value = VideoUiState()
    }

    override fun onCleared() {
        super.onCleared()
        cancel()
    }

    private fun createPoseLandmarker(context: android.content.Context): Result<PoseLandmarker> {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_lite.task")
                .setDelegate(Delegate.GPU)
                .build()
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .build()
            Result.success(PoseLandmarker.createFromOptions(context, options))
        } catch (e: Exception) {
            try {
                val cpuOptions = BaseOptions.builder()
                    .setModelAssetPath("pose_landmarker_lite.task")
                    .setDelegate(Delegate.CPU)
                    .build()
                val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(cpuOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f)
                    .build()
                Result.success(PoseLandmarker.createFromOptions(context, options))
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    private fun detectPose(landmarker: PoseLandmarker, bitmap: Bitmap): PoseEngine.PoseResult? {
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detect(mpImage)
            val landmarks = result.landmarks().firstOrNull()?.map { lm ->
                PoseEngine.Landmark(
                    x = lm.x(),
                    y = lm.y(),
                    z = lm.z(),
                    visibility = lm.visibility().orElse(0f)
                )
            } ?: return null
            PoseEngine.PoseResult(
                landmarks = landmarks,
                timestampMs = result.timestampMs()
            )
        } catch (e: Exception) {
            null
        }
    }
}
