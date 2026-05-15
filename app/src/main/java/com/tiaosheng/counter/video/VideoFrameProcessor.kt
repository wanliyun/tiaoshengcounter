package com.tiaosheng.counter.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

class VideoFrameProcessor(
    private val context: Context,
    private val uri: Uri,
    private val targetFps: Int = 15
) {
    private var retriever: MediaMetadataRetriever? = null
    private var released = false

    val durationMs: Long by lazy {
        val r = MediaMetadataRetriever()
        r.setDataSource(context, uri)
        val durationStr = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        r.release()
        durationStr?.toLongOrNull() ?: 0L
    }

    val totalFrames: Int by lazy {
        ((durationMs / 1000.0) * targetFps).toInt()
    }

    fun open() {
        retriever = MediaMetadataRetriever().apply {
            setDataSource(context, uri)
        }
    }

    /**
     * 按顺序提取下一帧，无更多帧时返回 null。
     * 调用方负责按 targetFps 控制提取间隔。
     */
    fun getFrameAtTime(timeUs: Long, maxWidth: Int = 480): Bitmap? {
        if (released) return null
        return retriever?.getScaledFrameAtTime(
            timeUs,
            MediaMetadataRetriever.OPTION_CLOSEST,
            maxWidth,
            maxWidth * 16 / 9
        )
    }

    fun release() {
        released = true
        retriever?.release()
        retriever = null
    }
}
