# 恢复快照

## 主线目标
Android 跳绳计数器 App 完整实现，含首页模式选择、定时/定数/视频计数三种运动模式。

## 正在做什么
首页设计已完成。`./gradlew assembleDebug` 编译通过，零错误零警告。

## 关键上下文
- 技术栈：Kotlin + Jetpack Compose + CameraX + MediaPipe BlazePose + Room
- 包名：com.tiaosheng.counter，最低 API 26，目标 API 34
- 模型文件 `pose_landmarker_lite.task` 已存在于 assets/
- 首页新增 3 种运动模式入口：定时模式(倒计时)、定数模式(目标次数)、视频计数(从视频文件识别)
- MainScreen 重构为支持 ExerciseMode 参数(FREE/TIMED/COUNT)
- 视频计数使用 IMAGE 模式 PoseLandmarker + MediaMetadataRetriever 逐帧提取
- 共新增 6 个文件，修改 4 个文件

## 下一步
- 真机/模拟器验证：首页 UI 显示、模式配置面板、定时/定数自动结束、视频计数流程
- Could Have 功能未曾实现（目标设定、成就系统、背景音乐、暗色模式、桌面小组件）

## 阻塞项
（无）

## 方案
plans/jump-rope-counter/
