package com.example.audio

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance 16-bit 44.1kHz Mono PCM WAV Audio Recorder
 * Writes live synthesis output directly to disk with proper RIFF headers.
 */
class WavAudioRecorder {
    private var outputStream: BufferedOutputStream? = null
    private var targetFile: File? = null
    private var totalBytesWritten: Long = 0L

    @Volatile
    var isRecording: Boolean = false
        private set

    val recordedDurationSeconds: Float
        get() = (totalBytesWritten / 2) / 44100.0f

    @Synchronized
    fun start(file: File): Boolean {
        if (isRecording) return false
        return try {
            file.parentFile?.mkdirs()
            targetFile = file
            totalBytesWritten = 0L
            val fos = FileOutputStream(file)
            outputStream = BufferedOutputStream(fos, 32768)

            // Write 44-byte placeholder WAV header
            writeHeaderPlaceholder()
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            outputStream = null
            targetFile = null
            false
        }
    }

    /**
     * Called from audio processing thread for every PCM block
     */
    fun writeSamples(buffer: ShortArray, count: Int) {
        if (!isRecording) return
        val stream = outputStream ?: return

        try {
            val byteBuffer = ByteBuffer.allocate(count * 2)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until count) {
                byteBuffer.putShort(buffer[i])
            }
            val bytes = byteBuffer.array()
            stream.write(bytes)
            totalBytesWritten += bytes.size
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun stop(): File? {
        if (!isRecording) return null
        isRecording = false
        val file = targetFile

        try {
            outputStream?.flush()
            outputStream?.close()
            outputStream = null

            // Update WAV header with accurate sizes
            if (file != null && file.exists()) {
                updateWavHeader(file, totalBytesWritten)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val result = targetFile
        targetFile = null
        totalBytesWritten = 0L
        return result
    }

    private fun writeHeaderPlaceholder() {
        val stream = outputStream ?: return
        val placeholder = ByteArray(44)
        stream.write(placeholder)
    }

    private fun updateWavHeader(file: File, pcmDataLength: Long) {
        val totalDataLen = pcmDataLength + 36
        val sampleRate = 44100L
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmDataLength and 0xff).toByte()
        header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
        header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
        header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        raf.write(header)
        raf.close()
    }
}
