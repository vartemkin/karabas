package me.valse.karabas

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class TeeInputStream(
    private val source: InputStream,
    private val branch: OutputStream,
    private val onComplete: () -> Unit
) : InputStream() {

    private var isCompleted = false

    @Throws(IOException::class)
    override fun read(): Int {
        val value = source.read()
        if (value != -1) {
            branch.write(value)
        } else {
            complete()
        }
        return value
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        val count = source.read(b)
        if (count != -1) {
            branch.write(b, 0, count)
        } else {
            complete()
        }
        return count
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val count = source.read(b, off, len)
        if (count != -1) {
            branch.write(b, off, count)
        } else {
            complete()
        }
        return count
    }

    @Throws(IOException::class)
    override fun close() {
        try {
            source.close()
        } finally {
            branch.close()
        }
    }

    private fun complete() {
        if (!isCompleted) {
            isCompleted = true
            onComplete()
        }
    }
}
