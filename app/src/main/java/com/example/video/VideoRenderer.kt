package com.example.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.example.data.VideoProject
import com.example.model.SubtitleStyle
import com.example.model.VisualTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.sin

data class RenderResult(
    val file: File,
    val uri: Uri,
    val durationSeconds: Int,
    val frameCount: Int,
    val width: Int,
    val height: Int
)

class VideoRenderer(private val context: Context) {

    suspend fun renderVideo(
        project: VideoProject,
        imageUris: List<Uri>,
        audioUri: Uri?,
        onProgress: (percent: Int, message: String) -> Unit
    ): RenderResult = withContext(Dispatchers.IO) {
        // Validate selected audio before allocating bitmaps or encoding hundreds of frames.
        // The current muxer copies encoded samples and does not transcode MP3/WAV/PCM.
        if (audioUri != null) validateAudioForMp4(audioUri)

        val isShorts = project.format == "SHORTS"
        val width = if (isShorts) 720 else 1280
        val height = if (isShorts) 1280 else 720
        val fps = 30

        onProgress(5, "آماده‌سازی لایه‌های بصری و سناریو...")

        // 1. Respect the project's requested duration. Shorts are capped at 60 seconds;
        // long-form projects may render longer videos. If audio is selected, trim it to the
        // requested duration rather than silently forcing every format to 60 seconds.
        val maxDurationSec = if (isShorts) 60 else 60 * 60
        val requestedDurationSec = project.targetDurationSec.coerceIn(5, maxDurationSec)
        val audioDurationSec = audioUri?.let { getAudioDurationSeconds(it) }
        val targetDurationSec = if (audioDurationSec != null) {
            audioDurationSec.coerceAtLeast(5).coerceAtMost(requestedDurationSec)
        } else {
            requestedDurationSec
        }
        val totalFrames = targetDurationSec * fps

        // 2. Decode user images or prepare themed background slides
        val loadedBitmaps = mutableListOf<Bitmap>()
        for (uri in imageUris) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val original = BitmapFactory.decodeStream(stream)
                    if (original != null) {
                        val scaled = scaleCenterCrop(original, width, height)
                        // scaleCenterCrop returns a distinct cropped bitmap; release the decoded source
                        // immediately so projects with many images do not retain duplicate full-size bitmaps.
                        if (scaled !== original && !original.isRecycled) original.recycle()
                        loadedBitmaps.add(scaled)
                    }
                }
            } catch (e: Exception) {
                Log.e("VideoRenderer", "Failed to load image: $uri", e)
            }
        }

        // 3. Subtitle lines breakdown
        val scriptLines = parseScriptLines(project)

        // 4. Output File
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir
        if (!moviesDir.exists()) moviesDir.mkdirs()
        val outputFile = File(moviesDir, "yt_factory_${project.id}_${System.currentTimeMillis()}.mp4")

        onProgress(10, "پیکربندی انکودر سخت‌افزاری MediaCodec...")

        // 5. Setup MediaCodec & MediaMuxer
        val colorFormat = getSupportedColorFormat()
        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, 2_200_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoEncoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoTrackIndex = -1
        var muxerStarted = false

        val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(frameBitmap)
        val pixelArray = IntArray(width * height)
        val yuvBuffer = ByteArray(width * height * 3 / 2)

        val bufferInfo = MediaCodec.BufferInfo()
        val timeoutUs = 10_000L

        try {
            // Encode Frames
            for (frameIndex in 0 until totalFrames) {
                val currentSeconds = frameIndex / fps.toFloat()
                val currentProgress = 10 + ((frameIndex.toFloat() / totalFrames) * 80).toInt()

                if (frameIndex % 15 == 0) {
                    onProgress(currentProgress, "در حال انکود فریم $frameIndex از $totalFrames با MediaCodec...")
                }

                // Render visual contents onto frameBitmap
                drawFrame(
                    canvas = canvas,
                    width = width,
                    height = height,
                    frameIndex = frameIndex,
                    currentSeconds = currentSeconds,
                    totalSeconds = targetDurationSec.toFloat(),
                    loadedBitmaps = loadedBitmaps,
                    scriptLines = scriptLines,
                    project = project
                )

                // Convert to YUV420SemiPlanar / NV12
                frameBitmap.getPixels(pixelArray, 0, width, 0, 0, width, height)
                rgbToYuv420sp(pixelArray, width, height, yuvBuffer, colorFormat)

                // Feed to MediaCodec
                var inputIndex = videoEncoder.dequeueInputBuffer(timeoutUs)
                while (inputIndex < 0) {
                    drainEncoder(videoEncoder, muxer, bufferInfo, timeoutUs) { track ->
                        videoTrackIndex = track
                        muxerStarted = true
                    }
                    inputIndex = videoEncoder.dequeueInputBuffer(timeoutUs)
                }

                val inputBuffer = videoEncoder.getInputBuffer(inputIndex)!!
                inputBuffer.clear()
                inputBuffer.put(yuvBuffer)
                val presentationTimeUs = (frameIndex * 1_000_000L) / fps
                videoEncoder.queueInputBuffer(inputIndex, 0, yuvBuffer.size, presentationTimeUs, 0)

                // Drain output
                drainEncoder(videoEncoder, muxer, bufferInfo, 0) { track ->
                    videoTrackIndex = track
                    muxerStarted = true
                }
            }

            // Signal End of Stream
            val eosInputIndex = videoEncoder.dequeueInputBuffer(timeoutUs)
            if (eosInputIndex >= 0) {
                val lastPtsUs = (totalFrames * 1_000_000L) / fps
                videoEncoder.queueInputBuffer(eosInputIndex, 0, 0, lastPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            // Drain remaining
            while (true) {
                val status = drainEncoder(videoEncoder, muxer, bufferInfo, timeoutUs) { track ->
                    videoTrackIndex = track
                    muxerStarted = true
                }
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
                if (!status) break
            }

            onProgress(92, "در حال میکس صوت و نهایی‌سازی ساختار MP4...")

        } finally {
            try {
                videoEncoder.stop()
                videoEncoder.release()
            } catch (e: Exception) {
                Log.e("VideoRenderer", "Error stopping encoder", e)
            }
            try {
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
            } catch (e: Exception) {
                Log.e("VideoRenderer", "Error stopping muxer", e)
            }
            frameBitmap.recycle()
            for (b in loadedBitmaps) {
                if (!b.isRecycled) b.recycle()
            }
        }

        // Add audio in a second pass because MediaMuxer cannot add tracks after start().
        if (audioUri != null) {
            onProgress(94, "در حال ترکیب صدای انتخاب‌شده با ویدئو...")
            muxVideoWithAudio(context, outputFile, audioUri, targetDurationSec * 1_000_000L)
        }

        onProgress(97, "ثبت فایل نهایی در گالری دستگاه...")

        // Export/Register in MediaStore for immediate user accessibility
        val publicUri = registerInMediaStore(context, outputFile, project.title)

        onProgress(100, "ویدیوی واقعی MP4 آماده شد!")

        RenderResult(
            file = outputFile,
            uri = publicUri ?: Uri.fromFile(outputFile),
            durationSeconds = targetDurationSec,
            frameCount = totalFrames,
            width = width,
            height = height
        )
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        timeoutUs: Long,
        onMuxerStart: (videoTrackIndex: Int) -> Unit
    ): Boolean {
        var hadOutput = false
        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> return hadOutput
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = encoder.outputFormat
                    val trackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    onMuxerStart(trackIndex)
                    hadOutput = true
                }
                outputIndex >= 0 -> {
                    val encodedData = encoder.getOutputBuffer(outputIndex)
                    if (encodedData != null && bufferInfo.size > 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(0, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    hadOutput = true
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        return hadOutput
                    }
                }
            }
        }
    }

    private fun drawFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        frameIndex: Int,
        currentSeconds: Float,
        totalSeconds: Float,
        loadedBitmaps: List<Bitmap>,
        scriptLines: List<String>,
        project: VideoProject
    ) {
        val visualTheme = VisualTheme.fromString(project.visualTheme)
        val subtitleStyle = SubtitleStyle.fromString(project.subtitleStyle)

        // 1. Draw Background Image or Theme Graphic
        if (loadedBitmaps.isNotEmpty()) {
            val imageIndex = ((currentSeconds / totalSeconds) * loadedBitmaps.size).toInt().coerceIn(0, loadedBitmaps.size - 1)
            val currentBitmap = loadedBitmaps[imageIndex]
            canvas.drawBitmap(currentBitmap, 0f, 0f, null)

            // Dim overlay for subtitle readability
            val dimPaint = Paint().apply {
                color = Color.argb(80, 0, 0, 0)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        } else {
            // Render themed procedural graphics
            drawProceduralTheme(canvas, width, height, visualTheme, frameIndex)
        }

        // 2. Draw Top Watermark / Badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 10, 15, 29)
        }
        val badgeRect = RectF(24f, 24f, (width - 24).toFloat(), 96f)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgePaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.parseColor("#00E5FF")
        }
        canvas.drawRoundRect(badgeRect, 18f, 18f, borderPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val displayTitle = if (project.title.length > 38) project.title.take(38) + "..." else project.title
        canvas.drawText(displayTitle, 40f, 66f, titlePaint)

        // 3. Draw Subtitles at bottom third
        if (scriptLines.isNotEmpty()) {
            val subIndex = ((currentSeconds / totalSeconds) * scriptLines.size).toInt().coerceIn(0, scriptLines.size - 1)
            val activeSubtitle = scriptLines[subIndex]

            drawSubtitleBox(
                canvas = canvas,
                width = width,
                height = height,
                text = activeSubtitle,
                style = subtitleStyle
            )
        }
    }

    private fun drawProceduralTheme(
        canvas: Canvas,
        width: Int,
        height: Int,
        theme: VisualTheme,
        frameIndex: Int
    ) {
        val bgPaint = Paint()
        when (theme) {
            VisualTheme.CYBERPUNK -> {
                val shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.parseColor("#0A0E1A"), Color.parseColor("#150B28"), Color.parseColor("#00283A")),
                    null,
                    Shader.TileMode.CLAMP
                )
                bgPaint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // Neon Wave Visualizer Lines
                val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00E5FF")
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                }
                val waveY = height * 0.65f
                for (x in 0 until width step 8) {
                    val offset = (frameIndex * 0.15f) + (x * 0.03f)
                    val h = (sin(offset.toDouble()) * 35.0).toFloat()
                    canvas.drawLine(x.toFloat(), waveY, x.toFloat(), waveY + h, wavePaint)
                }
            }
            VisualTheme.MINIMAL_CLEAN -> {
                val shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.parseColor("#0F172A"), Color.parseColor("#1E293B")),
                    null,
                    Shader.TileMode.CLAMP
                )
                bgPaint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
            VisualTheme.DARK_STUDIO -> {
                val shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.parseColor("#050505"), Color.parseColor("#1F1135")),
                    null,
                    Shader.TileMode.CLAMP
                )
                bgPaint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
        }
    }

    private fun drawSubtitleBox(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        style: SubtitleStyle
    ) {
        val horizontalMargin = 32f
        val boxWidth = width - (horizontalMargin * 2)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setStyle(Paint.Style.STROKE)
        }

        when (style) {
            SubtitleStyle.BOLD_YELLOW -> {
                textPaint.color = Color.parseColor("#FFD600")
                textPaint.textSize = if (width < 800) 34f else 40f
                boxPaint.color = Color.argb(230, 0, 0, 0)
                strokePaint.color = Color.parseColor("#FFD600")
                strokePaint.strokeWidth = 3f
            }
            SubtitleStyle.CYAN_NEON -> {
                textPaint.color = Color.parseColor("#00E5FF")
                textPaint.textSize = if (width < 800) 32f else 38f
                boxPaint.color = Color.argb(230, 5, 16, 30)
                strokePaint.color = Color.parseColor("#00E5FF")
                strokePaint.strokeWidth = 2.5f
            }
            SubtitleStyle.MINIMAL_WHITE -> {
                textPaint.color = Color.WHITE
                textPaint.textSize = if (width < 800) 30f else 36f
                boxPaint.color = Color.argb(210, 15, 23, 42)
                strokePaint.color = Color.argb(120, 255, 255, 255)
                strokePaint.strokeWidth = 1.5f
            }
        }

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, (boxWidth - 40).toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, textPaint, (boxWidth - 40).toInt(), Layout.Alignment.ALIGN_CENTER, 1.1f, 0f, false)
        }

        val textHeight = staticLayout.height
        val boxHeight = textHeight + 36f
        val bottomMargin = 90f
        val boxTop = height - bottomMargin - boxHeight
        val rectF = RectF(horizontalMargin, boxTop, horizontalMargin + boxWidth, boxTop + boxHeight)

        canvas.drawRoundRect(rectF, 16f, 16f, boxPaint)
        canvas.drawRoundRect(rectF, 16f, 16f, strokePaint)

        canvas.save()
        canvas.translate(horizontalMargin + 20f, boxTop + 18f)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun parseScriptLines(project: VideoProject): List<String> {
        val raw = if (project.fullScript.isNotBlank()) project.fullScript else "${project.scriptHook}\n${project.scriptIntro}\n${project.scriptBody}\n${project.scriptCta}"
        val lines = raw.split("\n", "،", "؛", ".")
            .map { it.trim() }
            .filter { it.length > 5 }
        return if (lines.isNotEmpty()) lines else listOf(project.title, project.topic, "YouTube AI Factory")
    }

    private fun scaleCenterCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height
        val targetRatio = targetWidth.toFloat() / targetHeight
        val srcRatio = srcWidth.toFloat() / srcHeight

        val scale = if (srcRatio > targetRatio) {
            targetHeight.toFloat() / srcHeight
        } else {
            targetWidth.toFloat() / srcWidth
        }

        val scaledWidth = (srcWidth * scale).toInt()
        val scaledHeight = (srcHeight * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)

        val cropX = ((scaledWidth - targetWidth) / 2).coerceAtLeast(0)
        val cropY = ((scaledHeight - targetHeight) / 2).coerceAtLeast(0)

        val cropped = Bitmap.createBitmap(scaled, cropX, cropY, targetWidth, targetHeight)
        if (scaled != cropped && scaled != source) {
            scaled.recycle()
        }
        return cropped
    }

    private fun rgbToYuv420sp(
        argb: IntArray,
        width: Int,
        height: Int,
        yuv: ByteArray,
        colorFormat: Int
    ) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var index = 0

        val isSemiPlanar = colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar

        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[index++]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff

                val y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                val u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                val v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128

                yuv[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    if (isSemiPlanar) {
                        // NV12 format: U then V interleaved
                        yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                        yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                    } else {
                        // Planar I420
                        val uOffset = frameSize + (j / 2) * (width / 2) + (i / 2)
                        val vOffset = frameSize + (frameSize / 4) + (j / 2) * (width / 2) + (i / 2)
                        if (uOffset < yuv.size) yuv[uOffset] = u.coerceIn(0, 255).toByte()
                        if (vOffset < yuv.size) yuv[vOffset] = v.coerceIn(0, 255).toByte()
                    }
                }
            }
        }
    }

    private fun getSupportedColorFormat(): Int {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecList.codecInfos) {
            if (!info.isEncoder) continue
            val types = info.supportedTypes
            for (type in types) {
                if (type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)) {
                    val caps = info.getCapabilitiesForType(type)
                    for (format in caps.colorFormats) {
                        if (format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                            return format
                        }
                    }
                }
            }
        }
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }

    private fun validateAudioForMp4(audioUri: Uri) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, audioUri, null)
            var foundAudioTrack = false
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("audio/")) continue
                foundAudioTrack = true
                if (mime != "audio/mp4a-latm" && mime != "audio/aac") {
                    throw IllegalArgumentException(
                        "Unsupported audio format ($mime). Choose an AAC audio track in an M4A/MP4 file; MP3, WAV and PCM are not converted yet."
                    )
                }
                return
            }
            if (!foundAudioTrack) {
                throw IllegalArgumentException("The selected file does not contain an audio track.")
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Could not read the selected audio file. Choose a valid M4A/MP4 file containing AAC audio.",
                e
            )
        } finally {
            try { extractor.release() } catch (_: Exception) { }
        }
    }

    private fun getAudioDurationSeconds(audioUri: Uri): Int? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, audioUri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            retriever.release()
            durationMs?.let { (it / 1000).toInt() }
        } catch (e: Exception) {
            null
        }
    }

    private fun muxVideoWithAudio(
        context: Context,
        videoFile: File,
        audioUri: Uri,
        maxDurationUs: Long
    ) {
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        val tempFile = File(videoFile.parentFile, videoFile.nameWithoutExtension + "_muxed.mp4")
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            audioExtractor.setDataSource(context, audioUri, null)

            var videoSourceTrack = -1
            var audioSourceTrack = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                if ((format.getString(MediaFormat.KEY_MIME) ?: "").startsWith("video/")) {
                    videoSourceTrack = i
                    videoFormat = format
                    break
                }
            }
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    if (!mime.contains("aac") && !mime.contains("mp4a")) {
                        throw IllegalArgumentException("Selected audio must be AAC/M4A for MP4 output.")
                    }
                    audioSourceTrack = i
                    audioFormat = format
                    break
                }
            }
            if (videoSourceTrack < 0 || videoFormat == null) {
                throw IllegalStateException("Rendered video track was not found.")
            }
            if (audioSourceTrack < 0 || audioFormat == null) {
                throw IllegalArgumentException("No supported audio track was found in the selected file.")
            }

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val videoTrack = muxer.addTrack(videoFormat)
            val audioTrack = muxer.addTrack(audioFormat)
            videoExtractor.selectTrack(videoSourceTrack)
            audioExtractor.selectTrack(audioSourceTrack)
            muxer.start()
            muxerStarted = true

            val videoBufferSize = if (videoFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(256 * 1024) else 2 * 1024 * 1024
            val audioBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024) else 256 * 1024
            val videoBuffer = ByteBuffer.allocate(videoBufferSize)
            val audioBuffer = ByteBuffer.allocate(audioBufferSize)
            val info = MediaCodec.BufferInfo()
            var videoDone = false
            var audioDone = false

            while (!videoDone || !audioDone) {
                val videoTime = if (videoDone) Long.MAX_VALUE else videoExtractor.sampleTime
                val audioTime = if (audioDone) Long.MAX_VALUE else audioExtractor.sampleTime
                if (videoTime < 0L || videoTime > maxDurationUs) videoDone = true
                if (audioTime < 0L || audioTime > maxDurationUs) audioDone = true
                if (videoDone && audioDone) break

                val useVideo = !videoDone && (audioDone || videoTime <= audioTime)
                val extractor = if (useVideo) videoExtractor else audioExtractor
                val buffer = if (useVideo) videoBuffer else audioBuffer
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) {
                    if (useVideo) videoDone = true else audioDone = true
                    continue
                }
                info.offset = 0
                info.size = size
                info.presentationTimeUs = extractor.sampleTime
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(if (useVideo) videoTrack else audioTrack, buffer, info)
                extractor.advance()
            }
            muxer.stop()
            muxerStarted = false
            muxer.release()
            muxer = null

            // Replace only after muxing is complete, keeping a rollback copy on rename failure.
            val backupFile = File(videoFile.parentFile, videoFile.nameWithoutExtension + "_backup.mp4")
            if (backupFile.exists() && !backupFile.delete()) {
                throw IllegalStateException("Could not clear the previous video backup.")
            }
            if (!videoFile.renameTo(backupFile)) {
                throw IllegalStateException("Could not preserve the original rendered video.")
            }
            if (!tempFile.renameTo(videoFile)) {
                val restored = backupFile.renameTo(videoFile)
                if (!restored) {
                    Log.e("VideoRenderer", "Could not restore backup video at " + backupFile.absolutePath)
                }
                throw IllegalStateException("Could not finalize the audio-mixed MP4; original video restore " + (if (restored) "succeeded" else "failed") + ".")
            }
            if (backupFile.exists() && !backupFile.delete()) {
                Log.w("VideoRenderer", "Mixed video saved but temporary backup remains: " + backupFile.absolutePath)
            }
        } catch (e: Exception) {
            Log.e("VideoRenderer", "Audio muxing failed", e)
            throw IllegalStateException("Could not add selected audio to the MP4: ${e.message ?: "unsupported audio file"}", e)
        } finally {
            try { if (muxerStarted) muxer?.stop() } catch (_: Exception) { }
            try { muxer?.release() } catch (_: Exception) { }
            try { videoExtractor.release() } catch (_: Exception) { }
            try { audioExtractor.release() } catch (_: Exception) { }
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun registerInMediaStore(context: Context, videoFile: File, title: String): Uri? {
        var insertedUri: Uri? = null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.TITLE, title)
                put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/YouTubeAIFactory")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(collection, values)
                ?: throw IllegalStateException("MediaStore could not create an output entry.")
            insertedUri = uri

            val output = context.contentResolver.openOutputStream(uri, "w")
                ?: throw IllegalStateException("Could not open the exported video destination.")
            output.use { stream ->
                videoFile.inputStream().use { input -> input.copyTo(stream) }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publishValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                val updated = context.contentResolver.update(uri, publishValues, null, null)
                if (updated <= 0) throw IllegalStateException("Could not finalize the exported video.")
            }
            insertedUri = null
            uri
        } catch (e: Exception) {
            insertedUri?.let { uri ->
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
            Log.e("VideoRenderer", "Failed to register in MediaStore", e)
            null
        }
    }
}
