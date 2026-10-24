package com.example.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Generates real offline/device TTS narration and encodes the engine's PCM WAV output as AAC in M4A.
 * Voice availability and quality depend on the speech engine and installed language data.
 */
class SpeechAudioGenerator(private val context: Context) {
    suspend fun generate(
        text: String,
        languageCode: String,
        speechRate: Float
    ): File = withContext(Dispatchers.IO) {
        val normalized = text.trim()
        require(normalized.isNotBlank()) { "Add a script before generating narration." }
        require(normalized.length <= 20_000) { "Narration text is too long. Split it into shorter sections." }

        val id = UUID.randomUUID().toString()
        val wavFile = File(context.cacheDir, "narration_$id.wav")
        val m4aFile = File(context.getExternalFilesDir(null) ?: context.filesDir, "narration_$id.m4a")
        val engine = withContext(Dispatchers.Main) { createTts() }
        try {
            engine.setSpeechRate(speechRate.coerceIn(0.75f, 1.5f))
            val locale = if (languageCode.equals("fa", ignoreCase = true)) Locale("fa", "IR") else Locale.US
            val localeResult = engine.setLanguage(locale)
            if (localeResult == TextToSpeech.LANG_MISSING_DATA || localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                throw IllegalStateException(
                    if (languageCode.equals("fa", true))
                        "Persian TTS language data is not installed or supported by the device speech engine."
                    else
                        "English TTS language data is not installed or supported by the device speech engine."
                )
            }

            val result = CompletableDeferred<Unit>()
            val utteranceId = "narration_$id"
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(doneId: String?) {
                    if (doneId == utteranceId) result.complete(Unit)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(errorId: String?) {
                    if (errorId == utteranceId) result.completeExceptionally(
                        IllegalStateException("The device speech engine failed to synthesize narration.")
                    )
                }
                override fun onError(errorId: String?, errorCode: Int) {
                    if (errorId == utteranceId) result.completeExceptionally(
                        IllegalStateException("The device speech engine failed (code $errorCode).")
                    )
                }
            })
            val status = engine.synthesizeToFile(normalized, android.os.Bundle(), wavFile, utteranceId)
            if (status != TextToSpeech.SUCCESS) {
                throw IllegalStateException("The device speech engine rejected the narration request.")
            }
            withTimeout(TimeUnit.MINUTES.toMillis(3)) { result.await() }
            if (!wavFile.isFile || wavFile.length() < 44) {
                throw IllegalStateException("The speech engine returned an empty or unsupported audio file.")
            }
            encodeWavAsAac(wavFile, m4aFile)
            if (!m4aFile.isFile || m4aFile.length() <= 0L) {
                throw IllegalStateException("AAC narration encoding produced no output.")
            }
            m4aFile
        } catch (e: Exception) {
            m4aFile.delete()
            throw e
        } finally {
            wavFile.delete()
            withContext(Dispatchers.Main) {
                runCatching { engine.stop() }
                runCatching { engine.shutdown() }
            }
        }
    }

    private suspend fun createTts(): TextToSpeech {
        val signal = CompletableDeferred<Int>()
        val engine = TextToSpeech(context.applicationContext) { status ->
            if (!signal.isCompleted) signal.complete(status)
        }
        val status = try {
            withTimeout(TimeUnit.SECONDS.toMillis(30)) { signal.await() }
        } catch (e: Exception) {
            engine.shutdown()
            throw e
        }
        if (status != TextToSpeech.SUCCESS) {
            engine.shutdown()
            throw IllegalStateException("Could not initialize the Android text-to-speech engine.")
        }
        return engine
    }

    private fun encodeWavAsAac(wavFile: File, outputFile: File) {
        val pcm = readPcmWav(wavFile)
        require(pcm.bitsPerSample == 16) { "Speech engine must produce 16-bit PCM audio." }
        require(pcm.channels in 1..2) { "Speech engine returned unsupported channel count." }
        require(pcm.sampleRate in 8_000..48_000) { "Speech engine returned an unsupported sample rate." }

        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            pcm.sampleRate,
            pcm.channels
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1
        val info = MediaCodec.BufferInfo()
        val timeoutUs = 10_000L
        var totalPcmBytesFed = 0L
        var inputFinished = false
        var eosSeen = false
        var emptyDrainAttempts = 0
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bytesPerFrame = pcm.channels * 2
            var offset = 0
            while (!inputFinished) {
                val inputIndex = encoder.dequeueInputBuffer(timeoutUs)
                if (inputIndex >= 0) {
                    val input = encoder.getInputBuffer(inputIndex)
                        ?: throw IllegalStateException("AAC encoder returned an invalid input buffer.")
                    input.clear()
                    val remaining = pcm.data.size - offset
                    val capacity = input.remaining()
                    val count = minOf(remaining, capacity)
                    val alignedCount = count - (count % bytesPerFrame)
                    if (alignedCount > 0) input.put(pcm.data, offset, alignedCount)
                    offset += alignedCount
                    val ptsUs = (totalPcmBytesFed / bytesPerFrame) * 1_000_000L / pcm.sampleRate
                    totalPcmBytesFed += alignedCount
                    val end = offset >= pcm.data.size
                    encoder.queueInputBuffer(
                        inputIndex, 0, alignedCount, ptsUs,
                        if (end) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    )
                    if (end) inputFinished = true
                }
                val outputIndex = encoder.dequeueOutputBuffer(info, timeoutUs)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) throw IllegalStateException("AAC encoder changed output format more than once.")
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputIndex >= 0 -> {
                        val buffer = encoder.getOutputBuffer(outputIndex)
                        if (buffer != null && info.size > 0) {
                            if (!muxerStarted) throw IllegalStateException("AAC encoder produced data before its output format.")
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            muxer.writeSampleData(trackIndex, buffer, info)
                        }
                        eosSeen = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        encoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
            while (!eosSeen && emptyDrainAttempts < 500) {
                info.flags = 0
                val outputIndex = encoder.dequeueOutputBuffer(info, timeoutUs)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) throw IllegalStateException("AAC encoder changed output format more than once.")
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputIndex >= 0 -> {
                        val buffer = encoder.getOutputBuffer(outputIndex)
                        if (buffer != null && info.size > 0) {
                            if (!muxerStarted) throw IllegalStateException("AAC encoder produced data before its output format.")
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            muxer.writeSampleData(trackIndex, buffer, info)
                        }
                        eosSeen = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        encoder.releaseOutputBuffer(outputIndex, false)
                    }
                    else -> emptyDrainAttempts++
                }
                if (outputIndex >= 0) emptyDrainAttempts = 0
            }
            if (!eosSeen || !muxerStarted) throw IllegalStateException("AAC encoder did not finish a valid M4A stream.")
            muxer.stop()
            muxerStarted = false
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            runCatching { if (muxerStarted) muxer?.stop() }
            runCatching { muxer?.release() }
            if (outputFile.exists() && outputFile.length() == 0L) outputFile.delete()
        }
    }

    private data class PcmWav(
        val data: ByteArray,
        val sampleRate: Int,
        val channels: Int,
        val bitsPerSample: Int
    )

    private fun readPcmWav(file: File): PcmWav {
        RandomAccessFile(file, "r").use { raf ->
            require(raf.length() >= 44L) { "Speech engine returned an invalid WAV file." }
            val header = ByteArray(12)
            raf.readFully(header)
            require(String(header, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                String(header, 8, 4, Charsets.US_ASCII) == "WAVE") {
                "Speech engine output is not a standard RIFF/WAVE file."
            }
            var channels = 0
            var sampleRate = 0
            var bitsPerSample = 0
            var audioFormat = 0
            var dataOffset = -1L
            var dataSize = 0L
            while (raf.filePointer + 8 <= raf.length()) {
                val chunkHeader = ByteArray(8)
                raf.readFully(chunkHeader)
                val chunkId = String(chunkHeader, 0, 4, Charsets.US_ASCII)
                val size = littleEndianInt(chunkHeader, 4).toLong() and 0xffffffffL
                val chunkStart = raf.filePointer
                when (chunkId) {
                    "fmt " -> {
                        require(size >= 16 && size <= 1024) { "WAV format chunk is invalid." }
                        val fmt = ByteArray(size.toInt())
                        raf.readFully(fmt)
                        audioFormat = littleEndianShort(fmt, 0)
                        channels = littleEndianShort(fmt, 2)
                        sampleRate = littleEndianInt(fmt, 4)
                        bitsPerSample = littleEndianShort(fmt, 14)
                    }
                    "data" -> {
                        dataOffset = chunkStart
                        dataSize = minOf(size, raf.length() - dataOffset)
                    }
                }
                val next = chunkStart + size + (size and 1L)
                if (next > raf.length()) break
                raf.seek(next)
                if (dataOffset >= 0 && channels > 0 && sampleRate > 0) break
            }
            require(audioFormat == 1) { "Speech engine must return uncompressed PCM WAV." }
            require(dataOffset >= 0 && dataSize > 0 && dataSize <= Int.MAX_VALUE) {
                "WAV file does not contain usable PCM samples."
            }
            raf.seek(dataOffset)
            val data = ByteArray(dataSize.toInt())
            raf.readFully(data)
            return PcmWav(data, sampleRate, channels, bitsPerSample)
        }
    }

    private fun littleEndianShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            ((bytes[offset + 3].toInt() and 0xff) shl 24)
}
