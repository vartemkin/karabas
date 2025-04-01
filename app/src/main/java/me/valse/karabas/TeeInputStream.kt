package me.valse.karabas

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class TeeInputStream(private val original: InputStream, private val branch: OutputStream) :
    InputStream() {
    @Throws(IOException::class)
    override fun read(): Int {
        val ch = original.read()
        if (ch != -1) {
            branch.write(ch)
        }
        return ch
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        val count = original.read(b)
        if (count != -1) {
            branch.write(b, 0, count)
        }
        return count
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val count = original.read(b, off, len)
        if (count != -1) {
            branch.write(b, off, count)
        }
        return count
    }

    @Throws(IOException::class)
    override fun close() {
        try {
            original.close()
        } finally {
            branch.close()
        }
    }
}
