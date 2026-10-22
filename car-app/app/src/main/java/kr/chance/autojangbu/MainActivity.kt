package kr.chance.autojangbu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kr.chance.autojangbu.data.TripStore
import kr.chance.autojangbu.record.RecorderService
import kr.chance.autojangbu.ui.HomeScreen
import kr.chance.autojangbu.ui.AutoJangbuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TripStore.init(this)
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        RecorderService.start(this)
        setContent { AutoJangbuTheme { HomeScreen() } }
    }
}
