package com.tiaosheng.counter.util

/**
 * 卡路里计算工具。
 * 使用标准 MET 公式：卡路里 = MET × 体重(kg) × 时长(h)
 * 跳绳中等速度 MET = 12.0
 */
object CalorieCalculator {

    /** 跳绳 MET 值（中等速度 100-120 次/分钟） */
    const val JUMP_ROPE_MET = 12.0

    /**
     * @param totalJumps 有效跳跃次数
     * @param weightKg 用户体重 (kg)
     * @param durationSeconds 有效运动时长 (秒)，排除暂停
     * @return 估算消耗卡路里 (kcal)
     */
    fun calculate(totalJumps: Int, weightKg: Float, durationSeconds: Int): Float {
        if (durationSeconds <= 0 || weightKg <= 0) return 0f
        val durationHours = durationSeconds / 3600f
        return (JUMP_ROPE_MET * weightKg * durationHours).toFloat()
    }

    /**
     * 基于跳跃次数的快速估算（用于实时显示）。
     * 假设平均 100次/分钟，每跳消耗约 0.12 * weightKg / 60 kcal
     */
    fun estimatePerJump(weightKg: Float): Float {
        // MET * weight * (1/60) / 100 jumps_per_min ≈ per-jump estimate
        return (JUMP_ROPE_MET * weightKg / 60f / 100f)
    }
}
