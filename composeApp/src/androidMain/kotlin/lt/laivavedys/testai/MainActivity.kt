package lt.laivavedys.testai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import lt.laivavedys.testai.data.AndroidStorageDir

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidStorageDir.filesDir = applicationContext.filesDir
        setContent {
            App()
        }
    }
}
