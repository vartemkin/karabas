import android.content.Context
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class WebViewBridge {
    private var context: Context? = null

    fun CacheApi(context: Context?) {
        this.context = context
    }

    @get:JavascriptInterface
    val cachedFiles: String
        get() {
            val cacheDir = context!!.cacheDir
            val files = cacheDir.listFiles()
            val jsonArray = JSONArray()

            if (files != null) {
                for (file in files) {
                    if (!file.name.startsWith("temp")) {
                        val jsonObject = JSONObject()
                        try {
                            jsonObject.put("name", file.name)
                            jsonObject.put("size", file.length())
                            jsonObject.put("path", file.absolutePath)
                            jsonArray.put(jsonObject)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            return jsonArray.toString()
        }

    @JavascriptInterface
    fun deleteFile(filename: String): Boolean {
        val file = File(context!!.cacheDir, filename)
        return file.delete()
    }

    @JavascriptInterface
    fun clearCache() {
        val cacheDir = context!!.cacheDir
        val files = cacheDir.listFiles()

        if (files != null) {
            for (file in files) {
                if (!file.name.startsWith("temp")) {
                    file.delete()
                }
            }
        }
    }

    @get:JavascriptInterface
    val cacheSize: Long
        get() {
            val cacheDir = context!!.cacheDir
            val files = cacheDir.listFiles()
            var size: Long = 0

            if (files != null) {
                for (file in files) {
                    if (!file.name.startsWith("temp")) {
                        size += file.length()
                    }
                }
            }

            return size
        }
}
