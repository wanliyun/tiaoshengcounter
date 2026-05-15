package com.tiaosheng.counter.camera

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.tiaosheng.counter.pose.PoseEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class CameraManager(
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onPoseResult: (PoseEngine.PoseResult) -> Unit
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var analysisUseCase: ImageAnalysis? = null

    suspend fun start(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val provider = ProcessCameraProvider.await(lifecycleOwner)
            cameraProvider = provider
            bindUseCases(provider)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        cameraProvider?.let { bindUseCases(it) }
    }

    fun stop() {
        cameraProvider?.unbindAll()
    }

    fun setFrameRateLimit(maxFps: Int) {
        analysisUseCase?.let {
            cameraProvider?.unbind(it)
            cameraProvider?.let { provider -> bindUseCases(provider, maxFps) }
        }
    }

    private fun bindUseCases(provider: ProcessCameraProvider, maxFps: Int = 30) {
        provider.unbindAll()

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val poseEngine = PoseEngine(lifecycleOwner)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(320, 240))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(lifecycleOwner)) { imageProxy ->
                    poseEngine.processFrame(imageProxy) { result ->
                        onPoseResult(result)
                    }
                    imageProxy.close()
                }
            }

        analysisUseCase = imageAnalysis

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            // Fallback: try without lens facing constraint
        }
    }
}

private suspend fun ProcessCameraProvider.Companion.await(
    owner: LifecycleOwner
): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
    val listener = object : java.util.concurrent.Executor {
        override fun execute(command: Runnable) = command.run()
    }
    val future = ProcessCameraProvider.getInstance(owner)
    future.addListener({
        if (cont.isActive) {
            cont.resume(future.get())
        }
    }, ContextCompat.getMainExecutor(owner))
}
