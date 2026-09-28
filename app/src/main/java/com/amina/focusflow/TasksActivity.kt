package com.amina.focusflow

// RecyclerView здесь удобнее обычного списка: карточки переиспользуются при прокрутке.
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TasksActivity : AppCompatActivity() {
    private var tab = "Today"
    private val taskEditor =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val changedId = result.data?.getLongExtra(TaskActivity.RESULT_TASK_ID, -1L) ?: -1L
                if (changedId != -1L)
                    Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show()
            }
            render()
        }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        render()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun openTask(id: Long? = null) {
        val i = Intent(this, TaskActivity::class.java)
        id?.let {
            i.putExtra("TASK_ID", it)
        }
        taskEditor.launch(i)
    }

    private fun render() {
        val root = FrameLayout(this)
        val (sc, body) = Ui.screen(this)
        Ui.header(body, "Tasks", "Plan it. Focus on it. Finish it.")
        val tabs = Ui.row(this)
        listOf("Today", "Upcoming", "Completed").forEachIndexed { i, label ->
            val active = label == tab
            val v =
                Ui.chip(
                        this,
                        label,
                        if (active) Ui.green else Ui.surface,
                        if (active) Color.WHITE else Ui.dark,
                    )
                    .apply {
                        isClickable = true
                        isFocusable = true
                        setPadding(
                            Ui.dp(this@TasksActivity, 10),
                            Ui.dp(this@TasksActivity, 11),
                            Ui.dp(this@TasksActivity, 10),
                            Ui.dp(this@TasksActivity, 11),
                        )
                        setOnClickListener {
                            if (tab != label) {
                                tab = label
                                render()
                            }
                        }
                    }
            tabs.addView(
                v,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (i > 0) marginStart = Ui.dp(this@TasksActivity, 8)
                },
            )
        }
        body.addView(tabs, Ui.lp(Ui.dp(this, 16)))
        val today = DataManager.today()
        val tasks =
            DataManager.loadTasks(this)
                .filter {
                    when (tab) {
                        "Upcoming" -> !it.completed && it.date.isNotBlank() && it.date > today
                        "Completed" -> it.completed
                        else -> !it.completed && (it.date.isBlank() || it.date <= today)
                    }
                }
                .sortedWith(
                    compareBy<Task> {
                            it.date.ifBlank {
                                today
                            }
                        }
                        .thenBy {
                            it.time
                        }
                )
        if (tasks.isEmpty()) {
            val empty =
                Ui.card(this, Ui.surface, 24).apply {
                    gravity = Gravity.CENTER
                    setPadding(
                        Ui.dp(this@TasksActivity, 18),
                        Ui.dp(this@TasksActivity, 28),
                        Ui.dp(this@TasksActivity, 18),
                        Ui.dp(this@TasksActivity, 28),
                    )
                }
            empty.addView(
                Ui.text(
                        this,
                        when (tab) {
                            "Upcoming" -> "Nothing upcoming"
                            "Completed" -> "No completed tasks yet"
                            else -> "Your day is clear"
                        },
                        16f,
                        true,
                    )
                    .apply {
                        gravity = Gravity.CENTER
                    }
            )
            empty.addView(
                Ui.text(
                        this,
                        if (tab == "Completed") "Finished tasks will appear here."
                        else "Add a task when you’re ready.",
                        12.5f,
                        false,
                        Ui.muted,
                    )
                    .apply {
                        gravity = Gravity.CENTER
                    },
                Ui.lp(Ui.dp(this, 6)),
            )
            body.addView(empty, Ui.lp(Ui.dp(this, 14)))
        } else {
            val list =
                RecyclerView(this).apply {
                    layoutManager = LinearLayoutManager(this@TasksActivity)
                    adapter = TaskAdapter(tasks)
                    isNestedScrollingEnabled = false
                    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                }
            body.addView(list, Ui.lp(Ui.dp(this, 8)))
        }
        val add =
            Ui.button(this, "Add task", Ui.green).apply {
                setOnClickListener {
                    openTask()
                }
            }
        body.addView(add, Ui.lp(Ui.dp(this, 18)))
        Ui.attachNav(root, sc, this, "Tasks")
        setContentView(root)
    }

    private inner class TaskAdapter(private val items: List<Task>) :
        RecyclerView.Adapter<TaskAdapter.Holder>() {
        inner class Holder(val card: LinearLayout) : RecyclerView.ViewHolder(card)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(
                Ui.card(this@TasksActivity).apply {
                    layoutParams =
                        RecyclerView.LayoutParams(-1, -2).apply {
                            topMargin = Ui.dp(this@TasksActivity, 9)
                        }
                }
            )

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: Holder, position: Int) {
            val t = items[position]
            h.card.removeAllViews()
            val r = Ui.row(this@TasksActivity)
            val check =
                TextView(this@TasksActivity).apply {
                    text = if (t.completed) "✓" else "○"
                    textSize = 23f
                    setTextColor(Ui.green)
                    gravity = Gravity.CENTER
                    setPadding(Ui.dp(this@TasksActivity, 4), 0, Ui.dp(this@TasksActivity, 8), 0)
                    setOnClickListener {
                        toggleComplete(t.id)
                    }
                }
            r.addView(check)
            val col =
                LinearLayout(this@TasksActivity).apply {
                    orientation = LinearLayout.VERTICAL
                }
            col.addView(Ui.text(this@TasksActivity, t.title, 15f, true))
            col.addView(
                Ui.text(
                    this@TasksActivity,
                    "${t.category}  •  ${if(t.date.isBlank()) "Any day" else DataManager.prettyDate(t.date)} ${t.time}",
                    11f,
                    false,
                    Ui.muted,
                ),
                Ui.lp(Ui.dp(this@TasksActivity, 4)),
            )
            r.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
            r.addView(
                Ui.chip(
                    this@TasksActivity,
                    "${t.duration} min",
                    if (t.completed) Ui.sage else Ui.blue,
                )
            )
            h.card.addView(r)
            h.card.setOnClickListener {
                openTask(t.id)
            }
        }
    }

    private fun toggleComplete(id: Long) {
        val all = DataManager.loadTasks(this)
        all.find {
                it.id == id
            }
            ?.let {
                it.completed = !it.completed
            }

        DataManager.saveTasks(this, all)
        render()
    }
}
