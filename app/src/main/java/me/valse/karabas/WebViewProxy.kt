package me.valse.karabas

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile


class WebViewProxy(context: Context) {

    // private val contextSS = context

    /*private fun getCacheDir2(context: Context): File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "KaraCache")
    }*/

    private fun shouldCacheRequest(request: WebResourceRequest): Boolean {
        val rangeHeader = request.requestHeaders["Range"]
        return when {
            rangeHeader == null -> true // Полный файл запрашивается
            rangeHeader.startsWith("bytes=0-") -> true // Начало файла
            else -> false // Пропускаем частичные запросы (докачка)
        }
    }

    fun getDownloadsDir(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Для Android 10+
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            // Альтернативный способ
            File(Environment.getExternalStorageDirectory(), "Download")
        }
    }


    /*private fun getCacheDir3(context: Context): File {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d("makfa zazaza", "pp")
            // Для Android 10+ используем специальный API
            var ttt =
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "KaraCache")
            Log.d("makfa zazaza", ttt.toString())
            return ttt
        } else {
            // Старый способ для версий ниже Android 10
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            Log.d("makfa zazaza", downloadsDir.toString())
            return File(downloadsDir, "ValseDlp")
        }
    }*/

    private fun getCacheDir(): File {
        val appCacheDir = File(getDownloadsDir(), "KaraCache").apply {
            if (!exists()) mkdirs()
        }
        return appCacheDir
    }

    private fun getTempFile(url: String): File {
        val uu = url.replace(".mp3", ".temp").replace(".flac", ".temp")
        var result = File(getCacheDir(), generateFilenameFromUrl(uu))
        Log.d("KaraLog: temp", result.toString())
        return result
    }

    private fun getCachedFile(url: String): File {
        val result = File(getCacheDir(), generateFilenameFromUrl(url))
        Log.d("KaraLog: cache", result.toString())
        return result
    }

    private fun generateFilenameFromUrl(url: String): String {
        // Извлекаем базовое имя файла
        val baseName = url
            .substringAfterLast('/')
            .substringBefore('?')
            .substringBefore('#')
            .takeIf { it.isNotBlank() }

        // Проверяем и очищаем имя файла
        return when {
            baseName == null -> "file_${url.hashCode()}"
            isValidFilename(baseName) -> baseName
            else -> sanitizeFilename(baseName) ?: "file_${url.hashCode()}"
        }
    }

    private fun isValidFilename(name: String): Boolean {
        val pattern = Regex("^[a-zA-Z0-9._-]+\\.[a-zA-Z0-9]+$")
        return pattern.matches(name) && name.length <= 255
    }

    private fun sanitizeFilename(name: String): String? {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .takeIf { it.contains('.') && it.length <= 255 }
    }

    private fun determineMimeType(response: Response): String {
        return response.header("Content-Type")?.substringBefore(";")?.trim()
            ?: response.body?.contentType()?.type ?: response.request.url.toString()
                .substringAfterLast('.').let { ext ->
                    when (ext.lowercase()) {
                        "mp3" -> "audio/mpeg"
                        "mp4" -> "video/mp4"
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        else -> "application/octet-stream"
                    }
                }
    }

    private fun determineCharset(response: Response): String {
        return response.header("Content-Type")?.substringAfter("charset=", "")?.trim()
            ?: response.body?.contentType()?.charset()?.name()
            ?: "UTF-8"
    }

    private fun convertHeaders(headers: Headers): Map<String, String> {
        return headers.toMultimap().mapValues { (_, values) ->
            values.first()
        }
    }


    @Throws(IOException::class)
    private fun downloadAndCacheResponse(
        url: String, request: WebResourceRequest
    ): WebResourceResponse {

        val client = OkHttpClient()
        val requestBuilder = Request.Builder().url(url)

        // Поддержка Range-заголовков для докачки
        val rangeHeader = request.requestHeaders["Range"]
        if (rangeHeader != null) {
            requestBuilder.addHeader("Range", rangeHeader)
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (!response.isSuccessful) {
            throw IOException("Failed to download file: $response")
        }

        // Получаем поток для чтения ответа
        val body = response.body
        val inputStream = body!!.byteStream()

        // Создаем временный файл для кеширования
        val tempFile = getTempFile(url)
        val fileOutputStream = FileOutputStream(tempFile)

        // Читаем и записываем файл одновременно
        val teeInputStream = TeeInputStream(inputStream, fileOutputStream, onComplete = {
            fileOutputStream.close()
            tempFile.renameTo(getCachedFile(url))
        })

        // Создаем ответ для WebView
        val webResourceResponse = WebResourceResponse(
            determineMimeType(response),
            determineCharset(response),
            response.code,
            response.message.takeIf { it.isNotEmpty() } ?: "OK", // Обязательно непустая строка
            convertHeaders(response.headers),
            teeInputStream)

        return webResourceResponse
    }

    @Throws(IOException::class)
    private fun createResponseFromFile(
        file: File, request: WebResourceRequest
    ): WebResourceResponse {
        val randomAccessFile = RandomAccessFile(file, "r")


        // Обработка Range-заголовков
        val rangeHeader = request.requestHeaders["Range"]
        val fileLength = file.length()
        var start: Long = 0
        var end = fileLength - 1

        if (rangeHeader != null) {
            // Парсим Range-заголовок (пример: "bytes=0-499")
            val ranges = rangeHeader.replace("bytes=", "").split("-".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            start = ranges[0].toLong()
            if (ranges.size > 1) {
                end = ranges[1].toLong()
            }
        }

        val contentLength = end - start + 1
        randomAccessFile.seek(start)


        // Создаем ограниченный поток
        val limitedInputStream: InputStream = LimitedInputStream(
            FileInputStream(randomAccessFile.fd), contentLength
        )


        // Формируем заголовки ответа
        val headers: MutableMap<String, String> = HashMap()
        if (rangeHeader != null) {
            headers["Content-Range"] = "bytes $start-$end/$fileLength"
            headers["Accept-Ranges"] = "bytes"
            return WebResourceResponse(
                "audio/mpeg", "utf-8", 206, "Partial Content", headers, limitedInputStream
            )
        } else {
            headers["Content-Length"] = fileLength.toString()
            return WebResourceResponse(
                "audio/mpeg", "utf-8", 200, "OK", headers, limitedInputStream
            )
        }
    }


    fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {

        val url = request.url.toString()

        // Перехватываем только mp3 и flac файлы
        if (url.endsWith(".mp3") || url.endsWith(".flac")) {
            try {
                // Проверяем наличие файла в кеше
                val cachedFile = getCachedFile(url)
                if (cachedFile.exists()) {
                    // Если файл есть в кеше - отдаем его
                    // return createResponseFromFile(cachedFile, request)
                    return null
                } else {
                    // Если файла нет - загружаем, кешируем и отдаем
                    // Кешируем только те файлы, которые запрашиваются с начала (Range: bytes=0-)
                    if (shouldCacheRequest(request)) {
                        return downloadAndCacheResponse(url, request)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }
}