package android.ut3.aviatio.adapter

import android.ut3.aviatio.R
import android.ut3.aviatio.helper.getHumanTimeFormatFromMilliseconds
import android.ut3.aviatio.model.RankedScore
import android.ut3.aviatio.model.StoredScore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.cardview_score.view.*

class ScoreListAdapter(private val scoresList: List<RankedScore>): RecyclerView.Adapter<ScoreListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.cardview_score, parent,
            false
        )
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return scoresList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var rankedScore = scoresList.get(position)
        holder.bind(rankedScore)
    }


    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        fun bind(rankedScore: RankedScore) {
            (itemView.findViewById(R.id.rank) as TextView).setText(rankedScore.rank.toString())
            (itemView.findViewById(R.id.name) as TextView).setText(rankedScore.name)
            (itemView.findViewById(R.id.time) as TextView).setText(getHumanTimeFormatFromMilliseconds(rankedScore.time))
        }
    }
}