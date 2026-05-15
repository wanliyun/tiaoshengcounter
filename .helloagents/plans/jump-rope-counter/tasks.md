# 跳绳计数器 — 任务分解

## 任务列表

- [ ] T1: 项目脚手架搭建（涉及文件：Gradle 构建脚本、MainActivity、Application 类、Compose 主题；完成标准：项目编译通过，空 Activity 可运行；验证方式：./gradlew assembleDebug 成功）
- [ ] T2: CameraX 相机集成（涉及文件：camera/CameraManager.kt；完成标准：全屏相机预览显示，前后摄像头可切换；验证方式：在真机/模拟器上看到实时预览画面）
- [ ] T3: MediaPipe BlazePose 集成（涉及文件：pose/PoseEngine.kt、assets/pose_landmarker_lite.task；完成标准：从相机帧中提取 33 个关键点并 log 输出坐标；验证方式：logcat 查看关键点数据是否正常输出）
- [ ] T4: 卡尔曼滤波器实现（涉及文件：counter/KalmanFilter.kt；完成标准：单测验证滤波收敛与噪声抑制；验证方式：./gradlew test --tests KalmanFilterTest）
- [ ] T5: 跳跃检测算法实现（涉及文件：counter/JumpDetector.kt、counter/DetectionState.kt；完成标准：双脚跳模式通过预录视频测试准确率 ≥95%；验证方式：使用标注过的跳绳视频跑批测试）
- [ ] T6: 主界面 HUD 覆盖层（涉及文件：ui/main/MainScreen.kt、ui/main/HudOverlay.kt、ui/main/MainViewModel.kt；完成标准：摄像头预览+半透明 HUD 叠加，状态切换正常；验证方式：可视化检查各状态显示）
- [ ] T7: 交替跳模式支持（涉及文件：counter/JumpDetector.kt 扩展；完成标准：交替跳模式通过预录视频测试准确率 ≥90%；验证方式：批测试）
- [ ] T8: 运动记录与 Room 数据库（涉及文件：data/db/AppDatabase.kt、data/db/ExerciseDao.kt、data/db/ExerciseEntity.kt、data/repository/RecordRepository.kt；完成标准：运动结束后数据写入 DB，重启应用数据仍在；验证方式：./gradlew test + 手动验证）
- [ ] T9: 历史记录页面（涉及文件：ui/history/HistoryScreen.kt、ui/history/HistoryViewModel.kt、ui/history/StatsChart.kt；完成标准：按日/周/月展示柱状图和列表；验证方式：手动添加测试数据，检查页面渲染）
- [ ] T10: 设置页面（涉及文件：data/preferences/SettingsStore.kt、ui/settings/SettingsScreen.kt、ui/settings/SettingsViewModel.kt；完成标准：所有设置项可修改并持久化，影响运行时行为；验证方式：修改设置后重启应用确认生效）
- [ ] T11: TTS 语音播报（涉及文件：util/TtsManager.kt；完成标准：达到设定间隔自动播报当前次数；验证方式：真机测试语音输出）
- [ ] T12: 卡路里计算与自动暂停/恢复（涉及文件：util/CalorieCalculator.kt、counter/JumpDetector.kt 自动暂停逻辑；完成标准：卡路里随计数实时更新；5 秒无跳跃自动暂停；验证方式：手动测试 + 单测）
- [ ] T13: 性能优化与低端机适配（涉及文件：pose/PoseEngine.kt 推理参数调整、CameraManager.kt 帧率控制；完成标准：中端机 ≥25FPS，低端机 ≥15FPS；验证方式：使用 Android Profiler 测量帧处理延迟）
- [ ] T14: 异常处理与健壮性（涉及文件：全局；完成标准：相机权限拒绝/光线不足/存储不足均有用户可见提示，不崩溃；验证方式：逐个触发异常场景验证）
- [ ] T15: 最终集成测试与准确率验证（涉及文件：全部；完成标准：双脚跳 ≥95%、交替跳 ≥90% 准确率，所有功能验收通过；验证方式：真实跳绳场景 3 组 ×100 次手工对比）

## 进度
待开始。
