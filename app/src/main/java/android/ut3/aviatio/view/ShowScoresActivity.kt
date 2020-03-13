package android.ut3.aviatio.view

import android.os.Bundle
import android.ut3.aviatio.R
import android.ut3.aviatio.adapter.ScoreListAdapter
import android.ut3.aviatio.helper.getHumanTimeFormatFromMilliseconds
import android.ut3.aviatio.model.RankedScore
import android.ut3.aviatio.viewmodel.ScoreViewModel
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koin.android.ext.android.inject

class ShowScoresActivity : AppCompatActivity() {

    private val scoreViewModel: ScoreViewModel by inject()
    lateinit var scoreListAdapter: ScoreListAdapter
    private var scoresToShow: List<RankedScore> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportActionBar != null) {
            supportActionBar?.hide()
        }
        setContentView(R.layout.activity_show_scores)

        val loader: ProgressBar = findViewById(R.id.loader_scores)
        val content: LinearLayout = findViewById(R.id.ShowScoresContentLayout)

        content.visibility = View.GONE
        val recyclerView = findViewById(R.id.rvScores) as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        scoreViewModel.getStoredScore().observe(this, androidx.lifecycle.Observer {
            var sortedList = it.sortedWith(compareBy({it.Temps}))
            scoresToShow = sortedList.mapIndexed { index, storedScore -> RankedScore(index+1, storedScore.Nom, storedScore.Date, storedScore.Temps) }
            (findViewById(R.id.top_name) as TextView).setText(sortedList.get(0).Nom)
            (findViewById(R.id.top_temps) as TextView).setText(getHumanTimeFormatFromMilliseconds(sortedList.get(0).Temps)+", trop puissant!")
            loader.visibility = View.GONE
            content.visibility = View.VISIBLE


            scoreListAdapter = ScoreListAdapter(scoresToShow)
            recyclerView.adapter = scoreListAdapter
        })

        scoreListAdapter = ScoreListAdapter(scoresToShow)
        recyclerView.adapter = scoreListAdapter

    }

}
