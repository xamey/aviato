package android.ut3.aviatio

import android.content.Intent
import android.os.Bundle
import android.ut3.aviatio.di.scoreRepository
import android.ut3.aviatio.di.scoreViewModel
import android.ut3.aviatio.view.SelectionActivity
import androidx.appcompat.app.AppCompatActivity
import org.koin.core.context.startKoin

class MainActivity : AppCompatActivity() {

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
        startActivity(Intent(this, SelectionActivity::class.java))
        finish()
    }
}
