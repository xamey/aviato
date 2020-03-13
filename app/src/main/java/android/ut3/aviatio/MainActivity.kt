package android.ut3.aviatio

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.ut3.aviatio.di.scoreRepository
import android.ut3.aviatio.di.scoreViewModel
import android.ut3.aviatio.helper.getHumanTimeFormatFromMilliseconds
import android.ut3.aviatio.view.ShowScoresActivity
import android.ut3.aviatio.view.VictoryActivity
import org.koin.core.context.startKoin

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startKoin {
            modules(
                listOf(
                    scoreViewModel,
                    scoreRepository
                )
            )
        }
        startActivity(Intent(this, ShowScoresActivity::class.java))
//        val intent = Intent(this, VictoryActivity::class.java)
//        intent.putExtra("score", 2500)
//        startActivity(intent)
        finish()

    }
}
