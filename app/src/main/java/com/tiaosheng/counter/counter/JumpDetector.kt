package com.tiaosheng.counter.counter

import com.tiaosheng.counter.pose.PoseEngine
import kotlin.math.sqrt

/**
 * 跳跃检测核心算法。
 * 基于脚踝关键点 Y 坐标的峰谷检测 + 卡尔曼滤波。
 *
 * 算法来源：参考 YOLOv8-Pose 跳绳论文的峰谷检测方法，
 * 使用滑动窗口均值和标准差动态设定阈值，自适应不同身高和跳跃幅度。
 */
class JumpDetector {

    /** 灵敏度档位对应的标准差系数 */
    enum class Sensitivity(val thresholdMultiplier: Double) {
        LOW(1.5),
        MEDIUM(1.2),
        HIGH(0.9)
    }

    /** 跳跃模式 */
    enum class JumpMode {
        /** 双脚跳：取左右脚踝 Y 坐标均值 */
        BOTH_FEET,

        /** 交替跳：左右脚踝独立追踪 */
        ALTERNATE
    }

    // 滑动窗口大小：约 3 秒 × 30 FPS
    private val windowSize = 90
    private val yWindow = ArrayDeque<Double>(windowSize)

    // 卡尔曼滤波器（双脚模式共享一个，交替模式各一个）
    private val sharedFilter = KalmanFilter()
    private val leftFilter = KalmanFilter()
    private val rightFilter = KalmanFilter()

    // 最小跳跃间隔 150ms（约 5 帧 @30fps）
    private val minJumpIntervalMs = 150L

    // 自动暂停阈值：5 秒无跳跃
    private val autoPauseThresholdMs = 5000L

    // 状态
    private var state = DetectionState.IDLE
    private var count = 0
    private var mode = JumpMode.BOTH_FEET
    private var sensitivity = Sensitivity.MEDIUM

    // 峰谷检测：追踪当前是在找波峰还是波谷
    private enum class PeakValleyState { TRACKING, VALLEY }
    private var peakValleyState = PeakValleyState.TRACKING

    // 交替跳模式：左右脚独立状态
    private var leftPeakValleyState = PeakValleyState.TRACKING
    private var rightPeakValleyState = PeakValleyState.TRACKING

    // 时间追踪
    private var lastJumpTimestampMs = 0L
    private var sessionStartTimestampMs = 0L

    // BPM：基于最近 5 秒的跳跃次数
    private val bpmWindowMs = 5000L
    private val jumpTimestamps = ArrayDeque<Long>()

    fun setMode(newMode: JumpMode) {
        if (mode != newMode) {
            mode = newMode
            reset()
        }
    }

    fun setSensitivity(newSensitivity: Sensitivity) {
        sensitivity = newSensitivity
    }

    fun getState(): DetectionState = state

    fun getCount(): Int = count

    /**
     * 处理一帧姿态结果，返回当前检测结果。
     * @param poseResult 每帧的姿态关键点数据
     * @return 跳跃检测结果
     */
    fun processFrame(poseResult: PoseEngine.PoseResult): JumpDetectionResult {
        val now = poseResult.timestampMs
        val landmarks = poseResult.landmarks

        // 检查是否检测到人体
        if (landmarks.size < 29) {
            // 关键点不足（需要至少左踝 27, 右踝 28）
            updateStateOnNoDetection(now)
            return buildResult(0.0, now)
        }

        val leftAnkle = landmarks[27]
        val rightAnkle = landmarks[28]

        // 检查关键点可见性
        val leftVisible = leftAnkle.visibility > 0.5f
        val rightVisible = rightAnkle.visibility > 0.5f

        if (!leftVisible && !rightVisible) {
            updateStateOnNoDetection(now)
            return buildResult(0.0, now)
        }

        val dt = 1.0 / 30.0

        return when (mode) {
            JumpMode.BOTH_FEET -> {
                val rawY = if (leftVisible && rightVisible) {
                    (leftAnkle.y + rightAnkle.y) / 2.0
                } else if (leftVisible) {
                    leftAnkle.y.toDouble()
                } else {
                    rightAnkle.y.toDouble()
                }
                val filteredY = sharedFilter.update(rawY, dt)
                detectJump(filteredY, now, peakValleyState) { newState ->
                    peakValleyState = newState
                }
                buildResult(filteredY, now)
            }
            JumpMode.ALTERNATE -> {
                val leftY = if (leftVisible) leftFilter.update(leftAnkle.y.toDouble(), dt) else 0.5
                val rightY = if (rightVisible) rightFilter.update(rightAnkle.y.toDouble(), dt) else 0.5
                val avgY = (leftY + rightY) / 2.0

                if (leftVisible) detectJump(leftY, now, leftPeakValleyState) { leftPeakValleyState = it }
                if (rightVisible) detectJump(rightY, now, rightPeakValleyState) { rightPeakValleyState = it }

                buildResult(avgY, now)
            }
        }
    }

    private fun detectJump(
        filteredY: Double,
        now: Long,
        currentPV: PeakValleyState,
        updatePV: (PeakValleyState) -> Unit
    ) {
        yWindow.addLast(filteredY)
        if (yWindow.size > windowSize) {
            yWindow.removeFirst()
        }

        if (yWindow.size < 10) return // 窗口数据不足

        val mean = yWindow.average()
        val std = stdDev(yWindow, mean)
        val threshold = sensitivity.thresholdMultiplier * std

        when (currentPV) {
            PeakValleyState.TRACKING -> {
                // 找波谷：脚踝 Y 值低于均值减去阈值 → 进入 VALLEY
                if (filteredY < mean - threshold) {
                    updatePV(PeakValleyState.VALLEY)
                }
            }
            PeakValleyState.VALLEY -> {
                // 确认跳跃：脚踝回到均值以上
                if (filteredY > mean + 0.5 * std) {
                    if (now - lastJumpTimestampMs >= minJumpIntervalMs) {
                        onJumpDetected(now)
                    }
                    updatePV(PeakValleyState.TRACKING)
                }
            }
        }

        // 自动暂停检测
        if (state == DetectionState.COUNTING &&
            now - lastJumpTimestampMs > autoPauseThresholdMs
        ) {
            state = DetectionState.PAUSED
        }
    }

    private fun onJumpDetected(now: Long) {
        if (state == DetectionState.READY || state == DetectionState.PAUSED) {
            state = DetectionState.COUNTING
            if (sessionStartTimestampMs == 0L) {
                sessionStartTimestampMs = now
            }
        }

        if (state == DetectionState.COUNTING) {
            count++
            lastJumpTimestampMs = now

            // 更新 BPM 窗口
            jumpTimestamps.addLast(now)
            val windowStart = now - bpmWindowMs
            while (jumpTimestamps.isNotEmpty() && jumpTimestamps.first() < windowStart) {
                jumpTimestamps.removeFirst()
            }
        }
    }

    private fun updateStateOnNoDetection(now: Long) {
        if (state == DetectionState.COUNTING || state == DetectionState.PAUSED) {
            if (now - lastJumpTimestampMs > autoPauseThresholdMs) {
                state = DetectionState.PAUSED
            }
        } else if (state != DetectionState.IDLE) {
            state = DetectionState.IDLE
        }
    }

    private fun buildResult(filteredY: Double, now: Long): JumpDetectionResult {
        val bpm = if (jumpTimestamps.size >= 2) {
            val duration = (jumpTimestamps.last() - jumpTimestamps.first()).coerceAtLeast(1L)
            (jumpTimestamps.size - 1) * 60000f / duration
        } else {
            0f
        }

        return JumpDetectionResult(
            state = state,
            count = count,
            bpm = bpm,
            ankleYFiltered = filteredY,
            timestampMs = now
        )
    }

    fun reset() {
        count = 0
        state = DetectionState.IDLE
        peakValleyState = PeakValleyState.TRACKING
        leftPeakValleyState = PeakValleyState.TRACKING
        rightPeakValleyState = PeakValleyState.TRACKING
        lastJumpTimestampMs = 0L
        sessionStartTimestampMs = 0L
        yWindow.clear()
        jumpTimestamps.clear()
        sharedFilter.reset()
        leftFilter.reset()
        rightFilter.reset()
    }

    fun pause() {
        if (state == DetectionState.COUNTING) {
            state = DetectionState.PAUSED
        }
    }

    fun resume() {
        if (state == DetectionState.PAUSED) {
            state = DetectionState.COUNTING
            // Reset peak detection to avoid false immediate trigger
            peakValleyState = PeakValleyState.TRACKING
            leftPeakValleyState = PeakValleyState.TRACKING
            rightPeakValleyState = PeakValleyState.TRACKING
        }
    }

    fun forceReady() {
        if (state == DetectionState.IDLE) {
            state = DetectionState.READY
        }
    }

    private fun stdDev(values: List<Double>, mean: Double): Double {
        if (values.size < 2) return 0.0
        val sumSq = values.sumOf { (it - mean) * (it - mean) }
        return sqrt(sumSq / (values.size - 1))
    }
}
