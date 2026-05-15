package com.tiaosheng.counter.counter

/**
 * 一维卡尔曼滤波器，用于平滑脚踝关键点 Y 坐标时间序列。
 * 状态: [position, velocity]
 * 测量: position (ankle_y)
 */
class KalmanFilter(
    private val processNoise: Double = 0.01,
    private val measurementNoise: Double = 0.1
) {
    // State: [position, velocity]
    private var position = 0.0
    private var velocity = 0.0

    // Error covariance matrix (2x2)
    private var p00 = 1.0
    private var p01 = 0.0
    private var p10 = 0.0
    private var p11 = 1.0

    private var initialized = false

    /**
     * @param measurement 当前测量值 (ankle_y)
     * @param dt 距上一帧的时间间隔 (秒)
     * @return 滤波后的估计位置
     */
    fun update(measurement: Double, dt: Double = 1.0 / 30.0): Double {
        if (!initialized) {
            position = measurement
            velocity = 0.0
            initialized = true
            return position
        }

        // Predict step
        // x = F * x
        // P = F * P * F^T + Q
        val predictedPosition = position + velocity * dt
        val predictedVelocity = velocity

        val predP00 = p00 + 2.0 * p01 * dt + p11 * dt * dt + processNoise
        val predP01 = p01 + p11 * dt
        val predP10 = p10 + p11 * dt
        val predP11 = p11 + processNoise

        // Update step
        // y = z - H * x (innovation)
        // S = H * P * H^T + R (innovation covariance)
        // K = P * H^T * S^-1 (Kalman gain)
        val innovation = measurement - predictedPosition
        val innovationCov = predP00 + measurementNoise

        val k0 = predP00 / innovationCov
        val k1 = predP10 / innovationCov

        position = predictedPosition + k0 * innovation
        velocity = predictedVelocity + k1 * innovation

        // P = (I - K * H) * P
        p00 = (1.0 - k0) * predP00
        p01 = (1.0 - k0) * predP01
        p10 = predP10 - k1 * predP00
        p11 = predP11 - k1 * predP01

        return position
    }

    fun reset() {
        position = 0.0
        velocity = 0.0
        p00 = 1.0
        p01 = 0.0
        p10 = 0.0
        p11 = 1.0
        initialized = false
    }
}
