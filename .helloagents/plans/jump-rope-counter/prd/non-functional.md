# 跳绳计数器 — 非功能需求

## 性能要求
- 推理帧率：中端机型（骁龙 7 系 / 天玑 8000）≥25 FPS；低端机型（骁龙 6 系）≥15 FPS
- 帧处理延迟：从 CameraX 输出帧到 UI 更新延迟 ≤80ms（含推理 + 算法处理）
- 应用冷启动时间 ≤2 秒（不含首次模型加载）；首次模型加载 ≤5 秒
- APK 体积 ≤30MB（含内嵌 BlazePose Lite 模型 ~5MB）
- 运行时内存占用 ≤150MB（含 Camera preview + MediaPipe Graph + UI）

## 安全需求
- 无网络权限声明（android.permission.INTERNET 可选但默认不请求），用户数据完全不出设备
- 仅请求 CAMERA 权限，运行时动态申请，拒绝时引导用户到系统设置
- 不收集、不传输任何个人身份信息 (PII)
- 摄像头数据仅用于本地姿态推理，不存储原始帧到磁盘
- 隐私政策声明所有处理均在设备本地完成（required for Google Play listing）
- Room 数据库存储在本应用私有目录，其他应用不可读

## 兼容性与容灾
- 最低 Android 8.0 (API 26)，目标 Android 14 (API 34)
- 支持 ARM64 (arm64-v8a) 架构；ARM32 (armeabi-v7a) 若有 MediaPipe 预编译库则支持，否则降级说明
- GPU 委托不可用时自动降级到 CPU 推理（性能下降但功能正常）
- 相机不可用时显示明确错误提示（而非崩溃）
- 存储空间不足时(可用 <50MB)提示用户清理，暂停写入历史记录但不中断计数

## 可扩展性
- 算法模块(JumpDetector)基于接口设计，后续可替换为 MNN/YOLOv8 推理引擎
- 运动类型预留扩展点：JumpDetector 接口可派生出其他运动检测器
- 数据导出预留 CSV/JSON 双格式支持

## 离线要求
- 模型文件 (pose_landmarker_lite.task) 打包在 APK assets 中，无需首次下载
- 首次启动 MediaPipe 从 assets 加载模型到内部存储（若需要）
- TTS 语音播报使用系统内置 TTS 引擎，不依赖网络语音合成
- 完全不声明 INTERNET 权限（可选但建议不声明以明确离线定位）

## 电池与功耗
- 使用 CameraX ImageAnalysis 背压策略 STRATEGY_KEEP_ONLY_LATEST 避免帧积压
- 推理用 GPU 委托降低 CPU 负载和功耗
- 运动暂停状态 5 秒后自动降低帧采样频率至 5 FPS（低功耗检测恢复）
- 目标：30 分钟连续运动功耗 ≤15%(4000mAh 电池中端机型参考值)

## 无障碍
- 语音播报满足视觉障碍用户基本使用需求
- 所有按钮提供 contentDescription
- 触摸目标最小 48dp（符合 Material Design 无障碍规范）
- 支持系统字体缩放（sp 单位）

## 隐私合规
- 首次启动显示简洁隐私声明弹窗："本应用所有数据仅在设备本地处理，不上传任何信息"
- Google Play Store listing 明确标注 "No data collected"
- 应用不需任何特殊权限（仅 CAMERA，属于高风险权限需提供使用说明）
