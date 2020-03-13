package android.ut3.aviatio.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.ut3.aviatio.R
import androidx.cardview.widget.CardView

class SelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportActionBar != null) {
            supportActionBar?.hide()
        }
        setContentView(R.layout.activity_selection)
        var cardViewGoToGame: CardView = findViewById(R.id.cardview_goto_game)
        cardViewGoToGame.setOnClickListener { startActivity(Intent(this, GameActivity::class.java)) }
        var cardViewGoToScore: CardView = findViewById(R.id.cardview_goto_score)
        cardViewGoToScore.setOnClickListener { startActivity(Intent(this, ShowScoresActivity::class.java)) }
    }
}
