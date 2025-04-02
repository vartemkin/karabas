package me.valse.karabas

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.valse.karabas.databinding.ActivityMainBinding

private const val REQUEST_CODE_STORAGE_PERMISSION = 1001  // Можно любое уникальное число
private const val REQUEST_CODE = 1002  // Можно любое уникальное число

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webViewModule: WebViewModule

    //private lateinit var mediaSession: MediaSessionCompat
    //private lateinit var mediaController: MediaControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        webViewModule = WebViewModule(this, binding.webView)

        webViewModule.cleanupTempFiles()
        checkStoragePermission()
        checkPermissions()


        /* // Инициализация MediaSession
        mediaSession = MediaSessionCompat(this, "WebViewMediaSession").apply {
            setCallback(webViewModule.mediaSessionCallback)
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true
        }

        // Создаем MediaController для управления
        mediaController = MediaControllerCompat(this, mediaSession).apply {
            MediaControllerCompat.setMediaController(this@MainActivity, this)
        }*/
    }

    override fun onDestroy() {
        webViewModule.cleanupTempFiles()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack();
            return
        }
        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event!!.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                        return true;
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }



    // Проверка и запрос разрешений
    fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Для Android 11+
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            // Для Android 6.0-10
            if (checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
            }
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
//        super.onCreate(savedInstanceState, persistentState)
//
//        var tt = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        if (tt != PackageManager.PERMISSION_GRANTED) {
//            // Запрашиваем разрешение
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                REQUEST_CODE_STORAGE_PERMISSION);
//        }
//    }


//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        // Проверяем разрешение после небольшой задержки
//        Handler(Looper.getMainLooper()).postDelayed({
//            checkStoragePermission()
//        }, 300) // Небольшая задержка для лучшего UX
//    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                // Разрешение уже есть
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startAppWork()
                }

                // Нужно показать объяснение
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) -> {
                    showPermissionRationale()
                }

                // Запрашиваем разрешение
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CODE_STORAGE_PERMISSION
                    )
                }
            }
        } else {
            // Для версий ниже Android 6.0 разрешения даются при установке
            startAppWork()
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Требуется доступ к хранилищу")
            .setMessage("Для сохранения аудиофайлов необходимо разрешение")
            .setPositiveButton("OK") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
                showPermissionDeniedMessage()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startAppWork()
                } else {
                    showPermissionDeniedMessage()
                }
            }
        }
    }

    private fun startAppWork() {
        // Основная логика приложения
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(this, "Без разрешения некоторые функции недоступны", Toast.LENGTH_LONG).show()
    }
}