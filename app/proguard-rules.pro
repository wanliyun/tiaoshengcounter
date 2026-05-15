# MediaPipe model is bundled in assets, keep it
-keep class com.google.mediapipe.** { *; }

# javax.annotation.processing is compile-time only, not available at runtime
-dontwarn javax.annotation.processing.**

# MediaPipe proto classes are not used at runtime by tasks-vision
-dontwarn com.google.mediapipe.proto.**

# javax.lang.model is compile-time annotation processing, not needed at runtime
-dontwarn javax.lang.model.**
