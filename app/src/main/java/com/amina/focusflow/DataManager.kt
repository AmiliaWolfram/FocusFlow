package com.amina.focusflow

// Local persistence for the project. SharedPreferences + JSON is enough here,
// потому что данные небольшие и отдельная БД для этой версии приложения не нужна.
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

object DataManager {
    private const val P = "focusflow_v3"

    private fun p(c: Context) = c.getSharedPreferences(P, Context.MODE_PRIVATE)

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun prettyDate(s: String): String =
        try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s)!!
            SimpleDateFormat("EEE, d MMM", Locale.US).format(d)
        } catch (_: Exception) {
            s
        }

    fun categories(c: Context): MutableList<Category> {
        val raw =
            p(c).getString("categories", null)
                ?: return mutableListOf(
                    Category(1, "University", "school"),
                    Category(2, "Work", "work"),
                    Category(3, "Freelance", "laptop"),
                    Category(4, "Sport", "activity"),
                )
        return try {
            val a = JSONArray(raw)
            MutableList(a.length()) { i ->
                a.getJSONObject(i).let {
                    Category(it.getLong("id"), it.getString("name"), it.optString("icon", "●"))
                }
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveCategories(c: Context, x: List<Category>) {
        val a = JSONArray()
        x.forEach {
            a.put(JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon))
        }

        p(c).edit().putString("categories", a.toString()).apply()
    }

    fun loadGoals(c: Context): MutableList<Goal> {
        val raw =
            p(c).getString("goals", null)
                ?: return mutableListOf(Goal(1, "Finish FocusFlow", "University", 300, ""))
        return try {
            val a = JSONArray(raw)
            MutableList(a.length()) { i ->
                a.getJSONObject(i).let {
                    Goal(
                        it.getLong("id"),
                        it.getString("title"),
                        it.optString("category", "Personal"),
                        it.optInt("target", 120),
                        it.optString("deadline", ""),
                        it.optBoolean("completed", false),
                    )
                }
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveGoals(c: Context, x: List<Goal>) {
        val a = JSONArray()
        x.forEach {
            a.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("category", it.category)
                    .put("target", it.targetMinutes)
                    .put("deadline", it.deadline)
                    .put("completed", it.completed)
            )
        }

        p(c).edit().putString("goals", a.toString()).apply()
    }

    fun loadTasks(c: Context): MutableList<Task> {
        val raw = p(c).getString("tasks", null) ?: return mutableListOf()
        return try {
            val a = JSONArray(raw)
            MutableList(a.length()) { i ->
                a.getJSONObject(i).let {
                    Task(
                        it.getLong("id"),
                        it.getString("title"),
                        it.optString("category", "Personal"),
                        it.optInt("duration", 25),
                        it.optBoolean("completed", false),
                        it.optString("priority", "Medium"),
                        if (it.isNull("goalId")) null else it.optLong("goalId"),
                        it.optString("date", ""),
                        it.optString("time", ""),
                        it.optInt("focused", 0),
                        if (it.isNull("categoryId")) null else it.optLong("categoryId"),
                    )
                }
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun tasks(c: Context) = loadTasks(c)

    fun saveTasks(c: Context, x: List<Task>) {
        val a = JSONArray()
        x.forEach {
            a.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("category", it.category)
                    .put("duration", it.duration)
                    .put("completed", it.completed)
                    .put("priority", it.priority)
                    .put("goalId", it.goalId ?: JSONObject.NULL)
                    .put("date", it.date)
                    .put("time", it.time)
                    .put("focused", it.focusedMinutes)
                    .put("categoryId", it.categoryId ?: JSONObject.NULL)
            )
        }

        p(c).edit().putString("tasks", a.toString()).apply()
    }

    fun sessions(c: Context): MutableList<FocusSession> {
        val raw = p(c).getString("focus_sessions", null) ?: return mutableListOf()
        return try {
            val a = JSONArray(raw)
            MutableList(a.length()) { i ->
                a.getJSONObject(i).let {
                    FocusSession(
                        it.getLong("id"),
                        it.getLong("taskId"),
                        it.getString("date"),
                        it.getInt("minutes"),
                        it.optInt("distractions", 0),
                        if (it.isNull("categoryId")) null else it.optLong("categoryId"),
                        it.optString("categoryName", ""),
                    )
                }
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun saveSessions(c: Context, x: List<FocusSession>) {
        val a = JSONArray()
        x.forEach {
            a.put(
                JSONObject()
                    .put("id", it.id)
                    .put("taskId", it.taskId)
                    .put("date", it.date)
                    .put("minutes", it.minutes)
                    .put("distractions", it.distractions)
                    .put("categoryId", it.categoryId ?: JSONObject.NULL)
                    .put("categoryName", it.categoryName)
            )
        }

        p(c).edit().putString("focus_sessions", a.toString()).apply()
    }

    fun addSession(c: Context, taskId: Long, minutes: Int, distractions: Int) {
        val t = loadTasks(c)
        val task = t.find {
            it.id == taskId
        }

        val s = sessions(c)
        s.add(
            FocusSession(
                taskId = taskId,
                date = today(),
                minutes = minutes,
                distractions = distractions,
                categoryId = task?.categoryId,
                categoryName = task?.category ?: "Uncategorized",
            )
        )
        saveSessions(c, s)
        task?.focusedMinutes = (task?.focusedMinutes ?: 0) + minutes
        saveTasks(c, t)
    }

    fun focusMinutes(c: Context) =
        sessions(c)
            .filter {
                it.date == today()
            }
            .sumOf {
                it.minutes
            }

    fun totalFocus(c: Context): Int {
        val allTasks = loadTasks(c)
        val allSessions = sessions(c)
        val byTask =
            allSessions
                .groupBy {
                    it.taskId
                }
                .mapValues { (_, xs) ->
                    xs.sumOf {
                        it.minutes
                    }
                }
        val taskIds =
            allTasks
                .map {
                    it.id
                }
                .toSet()
        val taskCredit = allTasks.sumOf { t ->
            val timed = byTask[t.id] ?: 0
            if (t.completed) maxOf(timed, t.duration.coerceAtLeast(0)) else timed
        }
        val standalone =
            allSessions
                .filter {
                    it.taskId !in taskIds
                }
                .sumOf {
                    it.minutes
                }
        return taskCredit + standalone
    }

    fun sessionCount(c: Context) = sessions(c).size

    data class FocusScoreBreakdown(
        val completed: Int,
        val planned: Int,
        val taskPoints: Int,
        val focusedMinutes: Int,
        val plannedMinutes: Int,
        val focusPoints: Int,
        val distractions: Int,
        val distractionPoints: Int,
        val total: Int,
    )

    fun focusScoreBreakdown(c: Context): FocusScoreBreakdown {
        val todayTasks =
            loadTasks(c).filter {
                it.date.isBlank() || it.date == today()
            }
        val todaySessions =
            sessions(c).filter {
                it.date == today()
            }
        val completed = todayTasks.count {
            it.completed
        }
        val taskPoints =
            if (todayTasks.isEmpty()) 0
            else kotlin.math.round((completed.toDouble() / todayTasks.size) * 40.0).toInt()
        val byTask =
            todaySessions
                .groupBy {
                    it.taskId
                }
                .mapValues { (_, xs) ->
                    xs.sumOf {
                        it.minutes
                    }
                }
        val taskIds =
            todayTasks
                .map {
                    it.id
                }
                .toSet()
        val credited = todayTasks.sumOf { t ->
            val timed = byTask[t.id] ?: 0
            if (t.completed) maxOf(timed, t.duration.coerceAtLeast(0)) else timed
        }
        +todaySessions
            .filter {
                it.taskId !in taskIds
            }
            .sumOf {
                it.minutes
            }
        val plannedMinutes = todayTasks.sumOf {
            it.duration.coerceAtLeast(0)
        }
        val ratio =
            when {
                plannedMinutes > 0 -> credited.toDouble() / plannedMinutes
                credited > 0 -> 1.0
                else -> 0.0
            }.coerceIn(0.0, 1.0)
        val focusPoints = kotlin.math.round(ratio * 35.0).toInt()
        val distractions = todaySessions.sumOf {
            it.distractions
        }
        val distractionPoints = (25 - distractions * 5).coerceAtLeast(0)
        val total = (taskPoints + focusPoints + distractionPoints).coerceIn(0, 100)
        return FocusScoreBreakdown(
            completed,
            todayTasks.size,
            taskPoints,
            credited,
            plannedMinutes,
            focusPoints,
            distractions,
            distractionPoints,
            total,
        )
    }

    fun productivityScore(c: Context): Int = focusScoreBreakdown(c).total

    fun streak(c: Context): Int {
        val days =
            sessions(c)
                .map {
                    it.date
                }
                .toSet()
        var n = 0
        val cal = Calendar.getInstance()
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        while (days.contains(f.format(cal.time))) {
            n++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        return n
    }
}
