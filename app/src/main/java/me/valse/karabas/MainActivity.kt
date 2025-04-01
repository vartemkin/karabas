package me.valse.karabas

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import me.valse.karabas.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webViewModule: WebViewModule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        webViewModule = WebViewModule(this, binding.webView)
    }

    override fun onBackPressed() {
        if (webViewModule.onBack()) {
            return;
        }
        return super.onBackPressed();
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (webViewModule.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event)
    }
}