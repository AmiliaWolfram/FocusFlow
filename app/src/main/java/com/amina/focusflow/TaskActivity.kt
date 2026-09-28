package com.amina.focusflow

// Create/edit task screen. После Save обновляю общее локальное хранилище.
import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class TaskActivity : AppCompatActivity() {
    companion object {
        const val RESULT_TASK_ID = "RESULT_TASK_ID"
    }

    private fun returnResult(id: Long) {
        setResult(Activity.RESULT_OK, Intent().putExtra(RESULT_TASK_ID, id))
        finish()
    }

    private lateinit var body: LinearLayout
    private var chosenDate = ""

    private var chosenTime = "14:00"

    private var chosenDuration = 45

    private var chosenPriority = "Medium"

    private var chosenCat = 0

    private var chosenGoal = 0

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        render()
    }

    private fun render() {
        val id = intent.getLongExtra("TASK_ID", -1)
        val existing =
            DataManager.loadTasks(this).find {
                it.id == id
            }

        val (sc, b) = Ui.screen(this)
        body = b
        val top = Ui.row(this)
        val back = Ui.icon(this, R.drawable.ic_back, Ui.dark, 24)
        back.setOnClickListener {
            finish()
        }

        top.addView(back)
        top.addView(Ui.spacer(this, 15))
        top.addView(
            Ui.text(this, if (existing == null) "New task" else "Task details", 25f, true),
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        body.addView(top)
        body.addView(
            Ui.text(
                this,
                if (existing == null) "Capture it now. Refine the details only when useful."
                else "A clear task makes starting easier.",
                12f,
                false,
                Ui.muted,
            ),
            Ui.lp(Ui.dp(this, 8)),
        )
        val title =
            EditText(this).apply {
                hint = "What do you want to accomplish?"
                setText(existing?.title ?: "")
                textSize = 17f
                setTextColor(Ui.dark)
                setHintTextColor(Ui.muted)
                background = Ui.bg(Ui.surface, Ui.dp(this@TaskActivity, 20).toFloat(), 1, Ui.line)
                setPadding(
                    Ui.dp(this@TaskActivity, 16),
                    Ui.dp(this@TaskActivity, 15),
                    Ui.dp(this@TaskActivity, 16),
                    Ui.dp(this@TaskActivity, 15),
                )
                minHeight = Ui.dp(this@TaskActivity, 64)
            }

        body.addView(title, Ui.lp(Ui.dp(this, 22)))
        body.addView(Ui.text(this, "DETAILS", 11f, true, Ui.muted), Ui.lp(Ui.dp(this, 22)))
        val cats = DataManager.categories(this)
        var goals = DataManager.loadGoals(this)
        chosenCat =
            existing?.let { e ->
                cats
                    .indexOfFirst {
                        it.id == e.categoryId || it.name == e.category
                    }
                    .coerceAtLeast(0)
            } ?: 0
        chosenGoal =
            existing?.goalId?.let { gid ->
                goals.indexOfFirst {
                    it.id == gid
                }
                +1
            } ?: 0
        chosenDate =
            existing?.date?.ifBlank {
                DataManager.today()
            } ?: DataManager.today()
        chosenTime =
            existing?.time?.ifBlank {
                "14:00"
            } ?: "14:00"
        chosenDuration = existing?.duration ?: 45
        chosenPriority = existing?.priority ?: "Medium"
        val detailCard = Ui.card(this, Ui.surface, 22)
        fun addDivider() {
            detailCard.addView(
                android.view.View(this).apply {
                    setBackgroundColor(Ui.line)
                },
                LinearLayout.LayoutParams(-1, 1),
            )
        }
        val catRow =
            Ui.infoRow(
                this,
                R.drawable.ic_category,
                "Category",
                cats.getOrNull(chosenCat)?.name ?: "None",
            )
        catRow.setOnClickListener {
            pick(
                "Category",
                cats.map {
                    it.name
                },
                chosenCat,
            ) {
                chosenCat = it
                (catRow.getChildAt(3) as TextView).text = cats[it].name
            }
        }

        detailCard.addView(catRow)
        addDivider()
        fun goalNames() =
            listOf("No goal") +
                goals.map {
                    it.title
                }
        val goalRow =
            Ui.infoRow(
                this,
                R.drawable.ic_target,
                "Goal",
                goalNames().getOrElse(chosenGoal) {
                    "No goal"
                },
            )
        fun openGoalPicker() {
            val wrap =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(
                        Ui.dp(this@TaskActivity, 22),
                        Ui.dp(this@TaskActivity, 10),
                        Ui.dp(this@TaskActivity, 22),
                        Ui.dp(this@TaskActivity, 16),
                    )
                }
            wrap.addView(Ui.serif(this, "Choose a goal", 24f, true))
            wrap.addView(
                Ui.text(this, "Connect this task to a bigger outcome.", 13f, false, Ui.muted),
                Ui.lp(Ui.dp(this, 6)),
            )
            val dialog = AlertDialog.Builder(this).setView(wrap).create()
            goalNames().forEachIndexed { i, name ->
                val row =
                    Ui.row(this).apply {
                        setPadding(
                            Ui.dp(this@TaskActivity, 4),
                            Ui.dp(this@TaskActivity, 13),
                            Ui.dp(this@TaskActivity, 4),
                            Ui.dp(this@TaskActivity, 13),
                        )
                        isClickable = true
                    }
                val radio =
                    RadioButton(this).apply {
                        isChecked = i == chosenGoal
                        buttonTintList = android.content.res.ColorStateList.valueOf(Ui.green)
                        isClickable = false
                    }
                row.addView(radio)
                row.addView(
                    Ui.text(this, name, 16f, i == chosenGoal),
                    LinearLayout.LayoutParams(0, -2, 1f).apply {
                        marginStart = Ui.dp(this@TaskActivity, 8)
                    },
                )
                row.setOnClickListener {
                    chosenGoal = i
                    (goalRow.getChildAt(3) as TextView).text = name
                    dialog.dismiss()
                }
                wrap.addView(row)
            }
            val create =
                Ui.button(this, "+  Create new goal", Ui.green).apply {
                    setTextColor(android.graphics.Color.WHITE)
                }
            create.setOnClickListener {
                dialog.dismiss()
                val form =
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(
                            Ui.dp(this@TaskActivity, 22),
                            Ui.dp(this@TaskActivity, 8),
                            Ui.dp(this@TaskActivity, 22),
                            0,
                        )
                    }
                val titleInput =
                    EditText(this).apply {
                        hint = "Goal title"
                        textSize = 16f
                        setTextColor(Ui.dark)
                        setHintTextColor(Ui.muted)
                        background =
                            Ui.bg(Ui.surface, Ui.dp(this@TaskActivity, 16).toFloat(), 1, Ui.line)
                        setPadding(
                            Ui.dp(this@TaskActivity, 14),
                            Ui.dp(this@TaskActivity, 13),
                            Ui.dp(this@TaskActivity, 14),
                            Ui.dp(this@TaskActivity, 13),
                        )
                    }
                val minutesInput =
                    EditText(this).apply {
                        hint = "Target focus minutes (optional)"
                        inputType = InputType.TYPE_CLASS_NUMBER
                        textSize = 15f
                        setTextColor(Ui.dark)
                        setHintTextColor(Ui.muted)
                        background =
                            Ui.bg(Ui.surface, Ui.dp(this@TaskActivity, 16).toFloat(), 1, Ui.line)
                        setPadding(
                            Ui.dp(this@TaskActivity, 14),
                            Ui.dp(this@TaskActivity, 13),
                            Ui.dp(this@TaskActivity, 14),
                            Ui.dp(this@TaskActivity, 13),
                        )
                    }
                form.addView(titleInput)
                form.addView(minutesInput, Ui.lp(Ui.dp(this, 10)))
                val createDialog =
                    AlertDialog.Builder(this)
                        .setTitle("New goal")
                        .setView(form)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Create", null)
                        .create()
                createDialog.setOnShowListener {
                    createDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Ui.green)
                    createDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val t = titleInput.text.toString().trim()
                        if (t.isBlank()) {
                            titleInput.error = "Add a goal title"
                            return@setOnClickListener
                        }
                        val g =
                            Goal(
                                title = t,
                                targetMinutes =
                                    minutesInput.text.toString().toIntOrNull()?.coerceAtLeast(1)
                                        ?: 120,
                            )
                        goals.add(g)
                        DataManager.saveGoals(this, goals)
                        chosenGoal = goals.size
                        (goalRow.getChildAt(3) as TextView).text = g.title
                        createDialog.dismiss()
                    }
                }
                createDialog.show()
            }
            wrap.addView(create, Ui.lp(Ui.dp(this, 10)))
            dialog.show()
        }
        goalRow.setOnClickListener {
            openGoalPicker()
        }

        detailCard.addView(goalRow)
        addDivider()
        val dateRow =
            Ui.infoRow(
                this,
                R.drawable.ic_calendar,
                "Date",
                if (chosenDate == DataManager.today()) "Today"
                else DataManager.prettyDate(chosenDate),
            )
        dateRow.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                    this,
                    R.style.Theme_FocusFlow_Picker,
                    { _, y, m, d ->
                        chosenDate = "%04d-%02d-%02d".format(y, m + 1, d)
                        dateRow.getChildAt(3).let {
                            (it as TextView).text = DataManager.prettyDate(chosenDate)
                        }
                    },
                    c.get(1),
                    c.get(2),
                    c.get(5),
                )
                .show()
        }

        detailCard.addView(dateRow)
        addDivider()
        val timeRow = Ui.infoRow(this, R.drawable.ic_clock, "Time", chosenTime)
        timeRow.setOnClickListener {
            TimePickerDialog(
                    this,
                    R.style.Theme_FocusFlow_Picker,
                    { _, h, m ->
                        chosenTime = "%02d:%02d".format(h, m)
                        (timeRow.getChildAt(3) as TextView).text = chosenTime
                    },
                    14,
                    0,
                    true,
                )
                .show()
        }

        detailCard.addView(timeRow)
        addDivider()
        val priRow = Ui.infoRow(this, R.drawable.ic_flag, "Priority", chosenPriority)
        priRow.setOnClickListener {
            val p = listOf("Low", "Medium", "High")
            pick("Priority", p, p.indexOf(chosenPriority)) {
                chosenPriority = p[it]
                (priRow.getChildAt(3) as TextView).text = chosenPriority
            }
        }

        detailCard.addView(priRow)
        body.addView(detailCard, Ui.lp(Ui.dp(this, 9)))
        body.addView(Ui.text(this, "FOCUS DURATION", 11f, true, Ui.muted), Ui.lp(Ui.dp(this, 22)))
        val durationRow = Ui.row(this)
        listOf(25, 45, 60, 90).forEachIndexed { i, d ->
            val ch =
                Ui.chip(
                    this,
                    "$d min",
                    if (d == chosenDuration) Ui.green else Ui.surface,
                    if (d == chosenDuration) Color.WHITE else Ui.dark,
                )
            ch.setOnClickListener {
                chosenDuration = d
                for (j in 0 until durationRow.childCount) {
                    val v = durationRow.getChildAt(j) as TextView
                    val active = j == i
                    v.background =
                        Ui.bg(if (active) Ui.green else Ui.surface, Ui.dp(this, 16).toFloat())
                    v.setTextColor(if (active) Color.WHITE else Ui.dark)
                }
            }

            durationRow.addView(
                ch,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (i > 0) marginStart = Ui.dp(this@TaskActivity, 7)
                },
            )
        }

        body.addView(durationRow, Ui.lp(Ui.dp(this, 9)))
        if (existing != null) {
            val focus = Ui.button(this, "Start focus", Ui.green)
            focus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0)
            focus.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            focus.compoundDrawablePadding = Ui.dp(this, 8)
            focus.setOnClickListener {
                startActivity(
                    Intent(this, FocusActivity::class.java).putExtra("TASK_ID", existing.id)
                )
            }

            body.addView(focus, Ui.lp(Ui.dp(this, 24)))
        }
        if (existing != null) {
            val done =
                Ui.button(
                    this,
                    if (existing.completed) "Mark as active" else "Mark completed",
                    if (existing.completed) Ui.green2 else Ui.green,
                )
            done.setOnClickListener {
                val all = DataManager.loadTasks(this)
                all.find {
                        it.id == existing.id
                    }
                    ?.completed = !existing.completed
                DataManager.saveTasks(this, all)
                returnResult(existing.id)
            }

            body.addView(done, Ui.lp(Ui.dp(this, 12)))
            val del =
                Ui.button(this, "Delete task", Ui.surface).apply {
                    setTextColor(Ui.red)
                    background =
                        Ui.bg(Ui.surface, Ui.dp(this@TaskActivity, 28).toFloat(), 1, Ui.line)
                }

            del.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete task?")
                    .setMessage("This removes the task. Saved focus sessions remain in Insights.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        val all = DataManager.loadTasks(this)
                        all.removeAll {
                            it.id == existing.id
                        }

                        DataManager.saveTasks(this, all)
                        returnResult(existing.id)
                    }
                    .show()
            }

            body.addView(del, Ui.lp(Ui.dp(this, 10)))
        }
        val save =
            Ui.button(this, if (existing == null) "Create task" else "Save changes", Ui.orange)
        save.setOnClickListener {
            if (title.text.isBlank()) {
                title.error = "Add a task title"
                return@setOnClickListener
            }

            val all = DataManager.loadTasks(this)
            val c = cats.getOrNull(chosenCat)
            val g = goals.getOrNull(chosenGoal - 1)
            if (existing == null)
                all.add(
                    Task(
                        title = title.text.toString().trim(),
                        category = c?.name ?: "Personal",
                        duration = chosenDuration,
                        priority = chosenPriority,
                        goalId = g?.id,
                        date = chosenDate,
                        time = chosenTime,
                        categoryId = c?.id,
                    )
                )
            else
                all.find {
                        it.id == existing.id
                    }
                    ?.apply {
                        this.title = title.text.toString().trim()
                        category = c?.name ?: category
                        categoryId = c?.id
                        duration = chosenDuration
                        priority = chosenPriority
                        goalId = g?.id
                        date = chosenDate
                        time = chosenTime
                    }

            DataManager.saveTasks(this, all)
            val savedId = existing?.id ?: all.last().id
            returnResult(savedId)
        }

        body.addView(save, Ui.lp(Ui.dp(this, 12)))
        setContentView(sc)
    }

    private fun pick(title: String, items: List<String>, selected: Int, onPick: (Int) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(items.toTypedArray(), selected) { d, w ->
                d.dismiss()
                onPick(w)
            }
            .show()
    }
}
