package com.amina.focusflow

// Статистику собираю из сохранённых tasks + focus sessions и передаю в CustomView-графики.
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {
    private var period = "Week"
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = FrameLayout(this)
        val (sc, body) = Ui.screen(this, botanical = true)
        Ui.header(body, "Insights", "See where your attention goes.")
        val tabs = Ui.row(this)
        listOf("Week", "Month", "Year").forEachIndexed { i, label ->
            val active = label == period
            val tab =
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
                            Ui.dp(this@StatisticsActivity, 12),
                            Ui.dp(this@StatisticsActivity, 11),
                            Ui.dp(this@StatisticsActivity, 12),
                            Ui.dp(this@StatisticsActivity, 11),
                        )
                        setOnClickListener {
                            if (period != label) {
                                period = label
                                render()
                            }
                        }
                    }
            tabs.addView(
                tab,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (i > 0) marginStart = Ui.dp(this@StatisticsActivity, 8)
                },
            )
        }
        body.addView(tabs, Ui.lp(Ui.dp(this, 14)))
        val allSessions = DataManager.sessions(this)
        val allTasks = DataManager.loadTasks(this)
        val sessions = allSessions.filter {
            inPeriod(it.date)
        }

        val tasks = allTasks.filter {
            it.date.isNotBlank() && inPeriod(it.date)
        }
        val completed = tasks.count {
            it.completed
        }

        val planned = tasks.size
        val creditedByDate = creditedMinutesByDate(tasks, sessions)
        val total = creditedByDate.values.sum()
        val taskScore =
            if (planned == 0) 0.0 else (completed.toDouble() / planned.toDouble()) * 40.0
        val plannedMinutes = tasks.sumOf {
            it.duration.coerceAtLeast(0)
        }
        // For completed tasks use max(timer, estimate), иначе одно и то же время посчитается
        // дважды.
        val sessionMinutesByTask =
            sessions
                .groupBy {
                    it.taskId
                }
                .mapValues { (_, xs) ->
                    xs.sumOf {
                        it.minutes
                    }
                }
        val taskIds =
            tasks
                .map {
                    it.id
                }
                .toSet()
        val creditedTaskMinutes = tasks.sumOf { task ->
            val timed = sessionMinutesByTask[task.id] ?: 0
            if (task.completed) maxOf(timed, task.duration.coerceAtLeast(0)) else timed
        }
        val standaloneSessionMinutes =
            sessions
                .filter {
                    it.taskId !in taskIds
                }
                .sumOf {
                    it.minutes
                }
        val creditedFocusMinutes = creditedTaskMinutes + standaloneSessionMinutes
        val focusRatio =
            when {
                plannedMinutes > 0 -> creditedFocusMinutes.toDouble() / plannedMinutes.toDouble()
                creditedFocusMinutes > 0 -> 1.0
                else -> 0.0
            }.coerceIn(0.0, 1.0)
        val focusScorePart = focusRatio * 35.0
        val periodDistractions = sessions.sumOf {
            it.distractions
        }
        val distractionScore = (25.0 - periodDistractions * 5.0).coerceAtLeast(0.0)
        val score =
            kotlin.math
                .round(taskScore + focusScorePart + distractionScore)
                .toInt()
                .coerceIn(0, 100)
        body.addView(Ui.text(this, periodLabel(), 12f, true, Ui.muted), Ui.lp(Ui.dp(this, 16)))
        val stats = Ui.row(this)
        listOf(
                formatMinutes(total) to "Focus time",
                sessions.size.toString() to "Sessions",
                score.toString() to "Focus score",
            )
            .forEachIndexed { i, (v, l) ->
                val c =
                    Ui.card(this, listOf(Ui.surface, Ui.sage, Ui.blue)[i], 20).apply {
                        gravity = Gravity.CENTER
                    }
                c.addView(
                    Ui.serif(this, v, 25f, true).apply {
                        gravity = Gravity.CENTER
                    }
                )
                c.addView(
                    Ui.text(this, l, 11.5f, false, Ui.muted).apply {
                        gravity = Gravity.CENTER
                    },
                    Ui.lp(Ui.dp(this, 3)),
                )
                stats.addView(
                    c,
                    LinearLayout.LayoutParams(0, Ui.dp(this, 100), 1f).apply {
                        if (i > 0) marginStart = Ui.dp(this@StatisticsActivity, 8)
                    },
                )
            }
        body.addView(stats, Ui.lp(Ui.dp(this, 10)))
        val distractionCount = sessions.sumOf {
            it.distractions
        }
        val attention = Ui.card(this, Ui.surface, 20)
        val ar = Ui.row(this)
        ar.addView(
            Ui.text(this, "Distractions logged", 13f, false, Ui.muted),
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        ar.addView(
            Ui.text(
                this,
                distractionCount.toString(),
                15f,
                true,
                if (distractionCount > 0) Ui.orange else Ui.green,
            )
        )
        attention.addView(ar)
        attention.addView(
            Ui.text(
                this,
                if (distractionCount == 0) "No distractions recorded in this period."
                else "Use this as a signal, not a score — it helps you notice patterns.",
                11.5f,
                false,
                Ui.muted,
            ),
            Ui.lp(Ui.dp(this, 5)),
        )
        body.addView(attention, Ui.lp(Ui.dp(this, 10)))
        body.addView(Ui.sectionTitle(this, "Focus over time"), Ui.lp(Ui.dp(this, 25)))
        val chartCard = Ui.card(this)
        val (vals, labs) = chartData(creditedByDate)
        val chartSummary = Ui.row(this)
        chartSummary.addView(
            Ui.text(this, focusChartSubtitle(total), 12f, false, Ui.muted),
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        chartCard.addView(chartSummary)
        chartCard.addView(
            FocusBarChart(this).apply {
                values = vals
                labels = labs
                highlightIndex = todayChartIndex()
            },
            LinearLayout.LayoutParams(-1, Ui.dp(this, 190)),
        )
        val nonZero =
            vals.withIndex().filter {
                it.value > 0
            }
        val bestIndex =
            vals.indices.maxByOrNull {
                vals[it]
            } ?: -1
        val bestText =
            if (bestIndex >= 0 && vals[bestIndex] > 0)
                "${labs.getOrElse(bestIndex) { "—" }} · ${formatMinutes(vals[bestIndex])}"
            else "—"
        val avgValue = if (nonZero.isEmpty()) 0 else total / nonZero.size
        val summaryDivider =
            android.view.View(this).apply {
                setBackgroundColor(Color.parseColor("#ECE8DF"))
            }
        chartCard.addView(
            summaryDivider,
            LinearLayout.LayoutParams(-1, Ui.dp(this, 1)).apply {
                topMargin = Ui.dp(this@StatisticsActivity, 6)
            },
        )
        val summaryRow =
            Ui.row(this).apply {
                setPadding(0, Ui.dp(this@StatisticsActivity, 12), 0, 0)
            }
        listOf(
                bestPeriodLabel() to bestText,
                "Active-day avg" to formatMinutes(avgValue),
                "Total focus" to formatMinutes(total),
            )
            .forEachIndexed { i, (label, value) ->
                val box =
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        addView(
                            Ui.text(this@StatisticsActivity, value, 12f, true, Ui.dark).apply {
                                gravity = Gravity.CENTER
                            }
                        )
                        addView(
                            Ui.text(this@StatisticsActivity, label, 10f, false, Ui.muted).apply {
                                gravity = Gravity.CENTER
                            },
                            Ui.lp(Ui.dp(this@StatisticsActivity, 2)),
                        )
                    }
                summaryRow.addView(box, LinearLayout.LayoutParams(0, -2, 1f))
            }
        chartCard.addView(summaryRow)
        body.addView(chartCard, Ui.lp(Ui.dp(this, 10)))
        body.addView(Ui.sectionTitle(this, "Where your time went"), Ui.lp(Ui.dp(this, 25)))
        val categories = DataManager.categories(this)
        val taskById = allTasks.associateBy {
            it.id
        }
        val sessionMinutesByTaskForCategories =
            sessions
                .groupBy {
                    it.taskId
                }
                .mapValues { (_, xs) ->
                    xs.sumOf {
                        it.minutes
                    }
                }
        val periodTaskIds =
            tasks
                .map {
                    it.id
                }
                .toSet()
        val categoryRows =
            categories
                .map { cat ->
                    val taskCredit =
                        tasks
                            .filter {
                                it.categoryId == cat.id ||
                                    it.category.equals(cat.name, ignoreCase = true)
                            }
                            .sumOf { task ->
                                val timed = sessionMinutesByTaskForCategories[task.id] ?: 0
                                if (task.completed) maxOf(timed, task.duration.coerceAtLeast(0))
                                else timed
                            }
                    val standaloneCredit =
                        sessions
                            .filter { session ->
                                session.taskId !in periodTaskIds &&
                                    (session.categoryId == cat.id ||
                                        session.categoryName.equals(cat.name, ignoreCase = true))
                            }
                            .sumOf {
                                it.minutes
                            }
                    Triple(
                        cat,
                        taskCredit + standaloneCredit,
                        tasks.filter {
                            it.categoryId == cat.id ||
                                it.category.equals(cat.name, ignoreCase = true)
                        },
                    )
                }
                .filter {
                    it.second > 0
                }
                .sortedByDescending {
                    it.second
                }
        val colors =
            listOf(
                Ui.green,
                Ui.orange,
                Color.parseColor("#4F86B8"),
                Color.parseColor("#B05C91"),
                Ui.green2,
                Ui.red,
            )
        val split = Ui.card(this)
        val categorizedTotal = categoryRows.sumOf {
            it.second
        }
        if (categoryRows.isEmpty() || categorizedTotal == 0) {
            split.addView(
                Ui.text(this, "No focus time recorded for this period yet.", 13f, true, Ui.dark)
            )
            split.addView(
                Ui.text(
                    this,
                    "Finish a focus session and it will appear here automatically.",
                    11.5f,
                    false,
                    Ui.muted,
                ),
                Ui.lp(Ui.dp(this, 6)),
            )
        } else {
            val top = Ui.row(this)
            top.addView(
                DonutChart(this).apply {
                    values = categoryRows.map {
                        it.second
                    }

                    this.colors = colors
                },
                LinearLayout.LayoutParams(Ui.dp(this, 128), Ui.dp(this, 128)),
            )
            val legend =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(Ui.dp(this@StatisticsActivity, 16), 0, 0, 0)
                }
            categoryRows.forEachIndexed { i, (cat, mins, _) ->
                val r = Ui.row(this)
                val dot =
                    FrameLayout(this).apply {
                        background =
                            Ui.bg(
                                colors[i % colors.size],
                                Ui.dp(this@StatisticsActivity, 5).toFloat(),
                            )
                    }
                r.addView(dot, LinearLayout.LayoutParams(Ui.dp(this, 10), Ui.dp(this, 10)))
                r.addView(
                    Ui.text(this, cat.name, 12f, true),
                    LinearLayout.LayoutParams(0, -2, 1f).apply {
                        marginStart = Ui.dp(this@StatisticsActivity, 8)
                    },
                )
                r.addView(Ui.text(this, formatMinutes(mins), 12f, false, Ui.muted))
                legend.addView(r, Ui.lp(if (i == 0) 0 else Ui.dp(this, 8)))
            }
            top.addView(legend, LinearLayout.LayoutParams(0, -2, 1f))
            split.addView(top)
            categoryRows.forEachIndexed { i, (cat, mins, _) ->
                val r =
                    Ui.row(this).apply {
                        setPadding(0, Ui.dp(this@StatisticsActivity, 10), 0, 0)
                    }
                r.addView(Ui.text(this, cat.name, 13f, true), LinearLayout.LayoutParams(0, -2, 1f))
                val pct = ((mins.toDouble() / categorizedTotal.toDouble()) * 100.0).toInt()
                r.addView(Ui.text(this, "${formatMinutes(mins)}  ·  $pct%", 12f, false, Ui.muted))
                split.addView(r)
            }
        }
        body.addView(split, Ui.lp(Ui.dp(this, 10)))
        body.addView(Ui.sectionTitle(this, "Tasks"), Ui.lp(Ui.dp(this, 25)))
        val taskCard = Ui.card(this)
        val taskRow = Ui.row(this)
        listOf(
                completed.toString() to "completed",
                planned.toString() to "planned",
                (if (planned == 0) 0 else completed * 100 / planned).toString() + "%" to
                    "completion",
            )
            .forEachIndexed { i, (v, l) ->
                val box =
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        addView(
                            Ui.serif(this@StatisticsActivity, v, 23f, true).apply {
                                gravity = Gravity.CENTER
                            }
                        )
                        addView(
                            Ui.text(this@StatisticsActivity, l, 11f, false, Ui.muted).apply {
                                gravity = Gravity.CENTER
                            }
                        )
                    }
                taskRow.addView(box, LinearLayout.LayoutParams(0, Ui.dp(this, 66), 1f))
            }

        taskCard.addView(taskRow)
        taskCard.addView(
            Ui.progress(this, if (planned == 0) 0 else completed * 100 / planned, Ui.green),
            Ui.lp(Ui.dp(this, 8)),
        )
        body.addView(taskCard, Ui.lp(Ui.dp(this, 10)))
        body.addView(Ui.sectionTitle(this, "Focus pattern"), Ui.lp(Ui.dp(this, 25)))
        val pattern = Ui.card(this)
        val longest =
            sessions.maxOfOrNull {
                it.minutes
            } ?: 0
        val avg = if (sessions.isEmpty()) 0 else total / sessions.size
        val best = bestDay(sessions)
        listOf(
                "Most focused" to best,
                "Average session" to "$avg min",
                "Longest session" to "$longest min",
            )
            .forEachIndexed { i, (l, v) ->
                val r = Ui.row(this)
                r.addView(
                    Ui.text(this, l, 13f, false, Ui.muted),
                    LinearLayout.LayoutParams(0, -2, 1f),
                )
                r.addView(Ui.text(this, v, 13f, true))
                pattern.addView(r, Ui.lp(if (i == 0) 0 else Ui.dp(this, 12)))
            }

        body.addView(pattern, Ui.lp(Ui.dp(this, 10)))
        Ui.attachNav(root, sc, this, "Insights")
        setContentView(root)
    }

    private fun inPeriod(date: String): Boolean {
        val d =
            try {
                fmt.parse(date)
            } catch (_: Exception) {
                null
            } ?: return false
        val v =
            Calendar.getInstance().apply {
                time = d
            }

        val n = Calendar.getInstance()
        return when (period) {
            "Month" ->
                v.get(Calendar.YEAR) == n.get(Calendar.YEAR) &&
                    v.get(Calendar.MONTH) == n.get(Calendar.MONTH)
            "Year" -> v.get(Calendar.YEAR) == n.get(Calendar.YEAR)
            else -> {
                val s =
                    Calendar.getInstance().apply {
                        firstDayOfWeek = Calendar.MONDAY
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                val e =
                    (s.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, 7)
                    }

                !v.before(s) && v.before(e)
            }
        }
    }

    private fun formatMinutes(m: Int) = if (m < 60) "${m}m" else "${m / 60}h ${m % 60}m"

    private fun periodLabel(): String {
        val n = Calendar.getInstance()
        return when (period) {
            "Year" -> n.get(Calendar.YEAR).toString()
            "Month" -> SimpleDateFormat("MMMM yyyy", Locale.US).format(n.time)
            else -> {
                val s =
                    (n.clone() as Calendar).apply {
                        firstDayOfWeek = Calendar.MONDAY
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    }

                val e =
                    (s.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, 6)
                    }

                SimpleDateFormat("MMM d", Locale.US).format(s.time) +
                    " — " +
                    SimpleDateFormat("MMM d, yyyy", Locale.US).format(e.time)
            }
        }
    }

    private fun creditedMinutesByDate(
        tasks: List<Task>,
        sessions: List<FocusSession>,
    ): Map<String, Int> {
        val byTask = sessions.groupBy {
            it.taskId
        }
        val taskIds =
            tasks
                .map {
                    it.id
                }
                .toSet()
        val out = mutableMapOf<String, Int>()
        tasks.forEach { task ->
            val taskSessions = byTask[task.id].orEmpty()
            val timed = taskSessions.sumOf {
                it.minutes
            }
            val credit = if (task.completed) maxOf(timed, task.duration.coerceAtLeast(0)) else timed
            if (credit > 0) {
                // Completed task credit stays on the task date standalone sessions keep their own
                // date.
                if (task.completed && task.date.isNotBlank())
                    out[task.date] = (out[task.date] ?: 0) + credit
                else
                    taskSessions.forEach {
                        out[it.date] = (out[it.date] ?: 0) + it.minutes
                    }
            }
        }
        sessions
            .filter {
                it.taskId !in taskIds
            }
            .forEach {
                out[it.date] = (out[it.date] ?: 0) + it.minutes
            }
        return out
    }

    private fun chartData(byDate: Map<String, Int>): Pair<List<Int>, List<String>> {
        val now = Calendar.getInstance()
        return when (period) {
            "Year" -> {
                val values =
                    (0..11).map { month ->
                        byDate
                            .filterKeys { key ->
                                parseCalendar(key)?.get(Calendar.MONTH) == month
                            }
                            .values
                            .sum()
                    }
                values to
                    listOf(
                        "Jan",
                        "Feb",
                        "Mar",
                        "Apr",
                        "May",
                        "Jun",
                        "Jul",
                        "Aug",
                        "Sep",
                        "Oct",
                        "Nov",
                        "Dec",
                    )
            }
            "Month" -> {
                val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                val values =
                    (1..daysInMonth).map { day ->
                        byDate
                            .filterKeys { key ->
                                parseCalendar(key)?.get(Calendar.DAY_OF_MONTH) == day
                            }
                            .values
                            .sum()
                    }
                val labels =
                    (1..daysInMonth).map { day ->
                        when {
                            day == 1 ||
                                day == daysInMonth ||
                                day == now.get(Calendar.DAY_OF_MONTH) -> day.toString()
                            day % 5 == 0 -> day.toString()
                            else -> ""
                        }
                    }
                values to labels
            }
            else -> {
                val monday =
                    (now.clone() as Calendar).apply {
                        firstDayOfWeek = Calendar.MONDAY
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    }
                val values =
                    (0..6).map { offset ->
                        val key =
                            fmt.format(
                                (monday.clone() as Calendar)
                                    .apply {
                                        add(Calendar.DAY_OF_YEAR, offset)
                                    }
                                    .time
                            )
                        byDate[key] ?: 0
                    }
                values to listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            }
        }
    }

    private fun parseCalendar(date: String): Calendar? =
        try {
            Calendar.getInstance().apply {
                time = fmt.parse(date)!!
            }
        } catch (_: Exception) {
            null
        }

    private fun bestPeriodLabel(): String =
        when (period) {
            "Year" -> "Best month"
            "Month" -> "Best day"
            else -> "Best day"
        }

    private fun focusChartSubtitle(total: Int): String =
        when (period) {
            "Year" -> "${formatMinutes(total)} focused this year"
            "Month" -> "${formatMinutes(total)} focused this month"
            else -> "${formatMinutes(total)} focused this week"
        }

    private fun todayChartIndex(): Int {
        val now = Calendar.getInstance()
        return when (period) {
            "Year" -> now.get(Calendar.MONTH)
            "Month" -> now.get(Calendar.DAY_OF_MONTH) - 1
            else -> ((now.get(Calendar.DAY_OF_WEEK) + 5) % 7) // RU/EN: понедельник / Monday = 0
        }
    }

    private fun bestDay(s: List<FocusSession>): String {
        if (s.isEmpty()) return "—"
        val e =
            s.groupBy {
                    it.date
                }
                .maxByOrNull {
                    it.value.sumOf { x ->
                        x.minutes
                    }
                } ?: return "—"
        return SimpleDateFormat("EEEE", Locale.US).format(fmt.parse(e.key)!!) +
            " · " +
            formatMinutes(
                e.value.sumOf {
                    it.minutes
                }
            )
    }
}
