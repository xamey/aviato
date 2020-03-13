package android.ut3.aviatio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.ut3.aviatio.di.scoreRepository
import android.ut3.aviatio.di.scoreViewModel
import android.ut3.aviatio.view.SelectionActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback

import androidx.core.app.ActivityCompat
import org.koin.core.context.startKoin

class MainActivity : AppCompatActivity() {
    val PERMISSION_ID = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startKoin {
            modules(
                listOf(
                    scoreViewModel,
                    scoreRepository
                )
            )
        }
        if (supportActionBar != null) {
            supportActionBar?.hide()
        }
        setContentView(R.layout.activity_main)


        requestPermissions()
        while (!checkPermissions()) {
            // onStop();
        }

        startActivity(Intent(this, SelectionActivity::class.java))
        finish()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.VIBRATE
            ),
            PERMISSION_ID
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.VIBRATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
}
