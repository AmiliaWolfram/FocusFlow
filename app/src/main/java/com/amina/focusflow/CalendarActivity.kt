package com.amina.focusflow

// Group tasks by date — так план на неделю проще быстро просмотреть.
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class CalendarActivity : AppCompatActivity() {
    private lateinit var details: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        render()
    }

    private fun render() {
        val root = FrameLayout(this)
        val (sc, body) = Ui.screen(this)
        Ui.header(body, "Calendar", "See the shape of your days.")
        val cal =
            CalendarView(this).apply {
                setBackgroundColor(Ui.surface)
                dateTextAppearance = android.R.style.TextAppearance_Material_Medium
                weekDayTextAppearance = android.R.style.TextAppearance_Material_Medium
            }

        body.addView(
            cal,
            LinearLayout.LayoutParams(-1, Ui.dp(this, 330)).apply {
                topMargin = Ui.dp(this@CalendarActivity, 14)
            },
        )
        details = Ui.card(this)
        body.addView(details, Ui.lp(Ui.dp(this, 14)))
        show(DataManager.today())
        cal.setOnDateChangeListener { _, y, m, d ->
            show("%04d-%02d-%02d".format(y, m + 1, d))
        }

        Ui.attachNav(root, sc, this, "Calendar")
        setContentView(root)
    }

    private fun show(date: String) {
        details.removeAllViews()
        val tasks =
            DataManager.loadTasks(this).filter {
                it.date == date
            }

        val mins =
            DataManager.sessions(this)
                .filter {
                    it.date == date
                }
                .sumOf {
                    it.minutes
                }

        details.addView(Ui.text(this, DataManager.prettyDate(date), 18f, true))
        details.addView(
            Ui.text(this, "${tasks.size} tasks  •  ${mins}m focused", 12f, false, Ui.muted),
            Ui.lp(Ui.dp(this, 5)),
        )
        if (tasks.isEmpty())
            details.addView(
                Ui.text(this, "A quiet day — nothing planned.", 13f, false, Ui.muted),
                Ui.lp(Ui.dp(this, 12)),
            )
        else
            tasks.forEach { t ->
                val r = Ui.row(this)
                r.addView(
                    Ui.text(this, "${if(t.completed)"✓" else "○"}  ${t.title}", 14f, true),
                    LinearLayout.LayoutParams(0, -2, 1f),
                )
                r.addView(Ui.chip(this, "${t.duration}m", Ui.blue))
                details.addView(r, Ui.lp(Ui.dp(this, 10)))
            }
    }
}
