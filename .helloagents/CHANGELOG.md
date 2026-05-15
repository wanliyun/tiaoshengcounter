# Changelog

## [1.0.1] - 2026-05-15

### 快速修改
- **[release]**: 新增 Linux/macOS 版 release.sh 打包脚本，复刻 release.bat 的 keystore 生成、签名配置写入、release 构建、APK 复制和签名验证流程 — by wanliyun
  - 类型: 快速修改（无方案包）
  - 文件: release.sh:1-104
- **[video-counting]**: 修复选择视频后直接崩溃的问题，`takePersistableUriPermission` 改为容错处理，视频处理入口增加 URI 打开/时长读取/模型初始化异常兜底，避免文件提供方不兼容时闪退 — by wanliyun
  - 类型: 快速修改（无方案包）
  - 文件: app/src/main/java/com/tiaosheng/counter/ui/video/VideoCountingScreen.kt:61-70, app/src/main/java/com/tiaosheng/counter/ui/video/VideoCountingViewModel.kt:39-120
