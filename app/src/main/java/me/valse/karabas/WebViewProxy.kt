package me.valse.karabas

import android.content.Context
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

    val contextSS = context;

    private fun getCacheDir(context: Context): File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "KaraCache")
    }

    private fun getCachedFile(url: String): File {
        return File(getCacheDir(contextSS), generateFilenameFromUrl(url))
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
        return name
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .takeIf { it.contains('.') && it.length <= 255 }
    }

    private fun determineMimeType(response: Response): String {
        return response.header("Content-Type")?.substringBefore(";")?.trim()
            ?: response.body?.contentType()?.type
            ?: response.request.url.toString().substringAfterLast('.').let { ext ->
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
        url: String,
        request: WebResourceRequest
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
        //val tempFile = File.createTempFile("temp", null, getCacheDir(contextSS))
        val cachedFile = getCachedFile(url)
        val fileOutputStream = FileOutputStream(cachedFile)


        // Читаем и записываем файл одновременно
        val teeInputStream: TeeInputStream = TeeInputStream(inputStream, fileOutputStream)


        // Создаем ответ для WebView
        val webResourceResponse = WebResourceResponse(
            determineMimeType(response),
            determineCharset(response),
            response.code,
            response.message,
            convertHeaders(response.headers),
            teeInputStream
        )


        /* // После завершения чтения переименовываем временный файл
        Thread {
            try {
                // Дожидаемся завершения чтения
                teeInputStream.close()
                fileOutputStream.close()


                // Переименовываем временный файл в постоянный
                val cachedFile = getCachedFile(url)
                tempFile.renameTo(cachedFile)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()

         */

        return webResourceResponse
    }

    @Throws(IOException::class)
    private fun createResponseFromFile(file: File, request: WebResourceRequest): WebResourceResponse {
        val randomAccessFile = RandomAccessFile(file, "r")


        // Обработка Range-заголовков
        val rangeHeader = request.requestHeaders["Range"]
        val fileLength = file.length()
        var start: Long = 0
        var end = fileLength - 1

        if (rangeHeader != null) {
            // Парсим Range-заголовок (пример: "bytes=0-499")
            val ranges =
                rangeHeader.replace("bytes=", "").split("-".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            start = ranges[0].toLong()
            if (ranges.size > 1) {
                end = ranges[1].toLong()
            }
        }

        val contentLength = end - start + 1
        randomAccessFile.seek(start)


        // Создаем ограниченный поток
        val limitedInputStream: InputStream = LimitedInputStream(
            FileInputStream(randomAccessFile.fd),
            contentLength
        )


        // Формируем заголовки ответа
        val headers: MutableMap<String, String> = HashMap()
        if (rangeHeader != null) {
            headers["Content-Range"] = "bytes $start-$end/$fileLength"
            headers["Accept-Ranges"] = "bytes"
            return WebResourceResponse(
                "audio/mpeg",
                "utf-8",
                206,
                "Partial Content",
                headers,
                limitedInputStream
            )
        } else {
            headers["Content-Length"] = fileLength.toString()
            return WebResourceResponse(
                "audio/mpeg",
                "utf-8",
                200,
                "OK",
                headers,
                limitedInputStream
            )
        }
    }




    fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {

        val shouldBlock = false // filterNetworkRequests(webView, request);
        Log.d("WebView", request.url.toString())
        if (shouldBlock) {
            return WebResourceResponse("text/javascript", "UTF-8", null)
        }
        return null
    }
}