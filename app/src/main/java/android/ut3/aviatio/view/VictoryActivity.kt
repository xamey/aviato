package android.ut3.aviatio.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.ut3.aviatio.R
import android.ut3.aviatio.helper.getHumanTimeFormatFromMilliseconds
import android.ut3.aviatio.model.Score
import android.ut3.aviatio.viewmodel.ScoreViewModel
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import org.koin.android.ext.android.inject

class VictoryActivity : AppCompatActivity() {

    private val scoreViewModel: ScoreViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportActionBar != null) {
            supportActionBar?.hide()
        }
        val score = intent.getIntExtra("score", 0)
        setContentView(R.layout.activity_victory)
        var textView: TextView = findViewById(R.id.scoreToBeShownId)
        textView.setText(getHumanTimeFormatFromMilliseconds(score))
        var editName: EditText = findViewById(R.id.enteredName)
        var sendVictoryBtn: ImageButton = findViewById(R.id.sendVictoryButton)
        sendVictoryBtn.setOnClickListener {
            if (editName.text.toString().equals("")) {
                Toast.makeText(this, "Vous devez rentrer un nom d'utilisateur", Toast.LENGTH_SHORT).show();
            }
            else {
                scoreViewModel.saveScore(Score(Nom = editName.text.toString(), Temps = score))
                startActivity(Intent(this, ShowScoresActivity::class.java))
                finish()
            }
        }
    }
}
