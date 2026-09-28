package com.amina.focusflow

// Goal progress считаю по focus-минутам связанных задач.
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class GoalsActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        render()
    }

    private fun render() {
        val (sc, body) = Ui.screen(this)
        Ui.header(body, "Goals", "Give your focus a destination.")
        val tabs = Ui.row(this)
        tabs.addView(
            Ui.chip(this, "Active", Ui.green, android.graphics.Color.WHITE),
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        tabs.addView(
            Ui.chip(this, "Completed", Ui.surface),
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = Ui.dp(this@GoalsActivity, 8)
            },
        )
        body.addView(tabs, Ui.lp(Ui.dp(this, 14)))
        val tasks = DataManager.loadTasks(this)
        DataManager.loadGoals(this).forEach { g ->
            val linked = tasks.filter {
                it.goalId == g.id
            }

            val mins = linked.sumOf {
                it.focusedMinutes
            }

            val pct =
                if (g.targetMinutes > 0) (mins * 100 / g.targetMinutes).coerceAtMost(100) else 0
            val c = Ui.card(this)
            val r = Ui.row(this)
            r.addView(Ui.text(this, "${g.title}", 17f, true), LinearLayout.LayoutParams(0, -2, 1f))
            r.addView(Ui.chip(this, "$pct%", Ui.sage))
            c.addView(r)
            c.addView(
                Ui.text(
                    this,
                    "${g.category}  •  ${linked.count{it.completed}} of ${linked.size} tasks",
                    12f,
                    false,
                    Ui.muted,
                ),
                Ui.lp(Ui.dp(this, 7)),
            )
            c.addView(Ui.progress(this, pct), Ui.lp(Ui.dp(this, 12)))
            body.addView(c, Ui.lp(Ui.dp(this, 10)))
        }

        val add = Ui.button(this, "New goal", Ui.green)
        add.setOnClickListener {
            create()
        }

        body.addView(add, Ui.lp(Ui.dp(this, 16)))
        setContentView(sc)
    }

    private fun create() {
        val box =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        val title =
            EditText(this).apply {
                hint = "Goal title"
            }

        val mins =
            EditText(this).apply {
                hint = "Target focus minutes"
                inputType = InputType.TYPE_CLASS_NUMBER
            }

        box.addView(title)
        box.addView(mins)
        AlertDialog.Builder(this)
            .setTitle("New goal")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                if (title.text.isNotBlank()) {
                    val x = DataManager.loadGoals(this)
                    x.add(
                        Goal(
                            title = title.text.toString(),
                            targetMinutes = mins.text.toString().toIntOrNull() ?: 120,
                        )
                    )
                    DataManager.saveGoals(this, x)
                    render()
                }
            }
            .show()
    }
}
