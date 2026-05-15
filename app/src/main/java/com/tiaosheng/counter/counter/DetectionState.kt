package com.tiaosheng.counter.counter

/**
 * 跳跃检测状态机
 */
enum class DetectionState {
    /** 未检测到人体，等待有效姿态 */
    IDLE,

    /** 检测到人体但尚无跳跃，等待首次跳跃触发 */
    READY,

    /** 计数中：正在追踪波峰→波谷周期 */
    COUNTING,

    /** 暂停：连续 5 秒无有效跳跃 */
    PAUSED,

    /** 光线不足警告（不影响计数，仅 UI 提示） */
    LOW_LIGHT
}

/**
 * 跳跃检测结果，每帧回调
 */
data class JumpDetectionResult(
    val state: DetectionState,
    val count: Int,
    val bpm: Float,
    val ankleYFiltered: Double,
    val timestampMs: Long
)
