package me.valse.karabas

import java.io.IOException
import java.io.InputStream
import kotlin.math.min


class LimitedInputStream(private val original: InputStream, private var remaining: Long) :
    InputStream() {
    @Throws(IOException::class)
    override fun read(): Int {
        if (remaining <= 0) {
            return -1
        }
        val result = original.read()
        if (result != -1) {
            remaining--
        }
        return result
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (remaining <= 0) {
            return -1
        }
        val bytesToRead = min(len.toDouble(), remaining.toDouble()) as Int
        val bytesRead = original.read(b, off, bytesToRead)
        if (bytesRead != -1) {
            remaining -= bytesRead.toLong()
        }
        return bytesRead
    }

    @Throws(IOException::class)
    override fun close() {
        original.close()
    }
}
