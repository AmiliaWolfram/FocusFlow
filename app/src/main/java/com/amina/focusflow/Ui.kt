package com.amina.focusflow

// Shared UI helpers — чтобы не копировать одни и те же cards, spacing и navigation по Activities.
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object Ui {
    val cream = Color.parseColor("#F8F4EA")
    val surface = Color.parseColor("#FFFDF9")
    val green = Color.parseColor("#15583F")
    val green2 = Color.parseColor("#39755A")
    val dark = Color.parseColor("#0C3B2B")
    val orange = Color.parseColor("#FF6828")
    val sage = Color.parseColor("#E4EDDF")
    val paleGreen = Color.parseColor("#EFF3E9")
    val blue = Color.parseColor("#E5F0F2")
    val muted = Color.parseColor("#737B76")
    val line = Color.parseColor("#ECE8DF")
    val red = Color.parseColor("#D15C48")
    val sand = Color.parseColor("#FFF0E6")
    val white = Color.parseColor("#FFFDF9")

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    fun bg(color: Int, r: Float = 22f, stroke: Int = 0, strokeColor: Int = line) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = r
            if (stroke > 0) setStroke(dpStroke(stroke), strokeColor)
        }

    private fun dpStroke(v: Int) = v.coerceAtLeast(1)

    fun text(c: Context, s: String, size: Float = 16f, bold: Boolean = false, color: Int = dark) =
        TextView(c).apply {
            text = s
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            letterSpacing = if (size <= 12f) .01f else 0f
        }

    fun serif(c: Context, s: String, size: Float = 28f, bold: Boolean = true, color: Int = dark) =
        TextView(c).apply {
            text = s
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            typeface = Typeface.create("serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            setLineSpacing(0f, .94f)
        }

    fun showFocusScoreInfo(c: Context, b: DataManager.FocusScoreBreakdown) {
        val wrap =
            LinearLayout(c).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(c, 24), dp(c, 8), dp(c, 24), dp(c, 8))
            }
        wrap.addView(serif(c, "How your Focus Score works", 24f, true))
        wrap.addView(
            text(c, "Your score is built from three parts.", 13f, false, muted),
            lp(dp(c, 8)),
        )
        fun section(title: String, points: String, formula: String) {
            val row = row(c)
            row.addView(text(c, title, 14f, true), LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(text(c, points, 14f, true, green))
            wrap.addView(row, lp(dp(c, 18)))
            wrap.addView(text(c, formula, 12f, false, muted), lp(dp(c, 5)))
        }
        section(
            "Task completion",
            "${b.taskPoints}/40",
            "${b.completed}/${b.planned} tasks completed × 40 points",
        )
        section(
            "Focused work",
            "${b.focusPoints}/35",
            "${b.focusedMinutes} min credited / ${b.plannedMinutes} min planned, capped at 35 points",
        )
        section(
            "Distraction control",
            "${b.distractionPoints}/25",
            "25 points − 5 for each distraction (${b.distractions} logged)",
        )
        val total =
            row(c).apply {
                setPadding(0, dp(c, 18), 0, 0)
                addView(text(c, "Total", 16f, true), LinearLayout.LayoutParams(0, -2, 1f))
                addView(serif(c, "${b.total}/100", 22f, true))
            }
        wrap.addView(total)
        AlertDialog.Builder(c).setView(wrap).setPositiveButton("Got it", null).show()
    }

    fun icon(c: Context, res: Int, tint: Int = dark, size: Int = 22) =
        ImageView(c).apply {
            setImageResource(res)
            imageTintList = ColorStateList.valueOf(tint)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LinearLayout.LayoutParams(dp(c, size), dp(c, size))
        }

    fun button(c: Context, s: String, color: Int = green) =
        Button(c).apply {
            text = s
            isAllCaps = false
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            background = bg(color, dp(c, 30).toFloat())
            stateListAnimator = null
            minHeight = 0
            minWidth = 0
            setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16))
        }

    fun card(c: Context, color: Int = surface, r: Int = 24) =
        LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            padding(18)
            background = bg(color, dp(c, r).toFloat(), if (color == surface) 1 else 0, line)
        }

    fun LinearLayout.padding(v: Int) {
        val d = dp(context, v)
        setPadding(d, d, d, d)
    }

    fun lp(top: Int = 0) =
        LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = top
        }

    fun row(c: Context) =
        LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

    fun spacer(c: Context, w: Int = 1, h: Int = 1) =
        Space(c).apply {
            layoutParams = LinearLayout.LayoutParams(dp(c, w), dp(c, h))
        }

    fun progress(c: Context, pct: Int, color: Int = green) =
        ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = pct
            progressTintList = ColorStateList.valueOf(color)
            progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#E9E5DC"))
            layoutParams = LinearLayout.LayoutParams(-1, dp(c, 7))
        }

    fun chip(c: Context, s: String, color: Int = paleGreen, textColor: Int = dark) =
        TextView(c).apply {
            text = s
            textSize = 13f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = bg(color, dp(c, 18).toFloat())
            setPadding(dp(c, 13), dp(c, 7), dp(c, 13), dp(c, 7))
        }

    fun setupWindow(a: Activity) {
        // Respect safe area: контент не должен уходить под status/navigation bars.
        WindowCompat.setDecorFitsSystemWindows(a.window, false)
        a.window.statusBarColor = cream
        a.window.navigationBarColor = cream
        @Suppress("DEPRECATION")
        a.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    fun screen(a: Activity, botanical: Boolean = true): Pair<ScrollView, LinearLayout> {
        setupWindow(a)
        val sc =
            ScrollView(a).apply {
                if (botanical) setBackgroundResource(R.drawable.home_botanical_bg)
                else setBackgroundColor(cream)
                isFillViewport = true
                clipToPadding = false
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                isVerticalScrollBarEnabled = false
                isSmoothScrollingEnabled = true
                descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            }
        val body =
            LinearLayout(a).apply {
                orientation = LinearLayout.VERTICAL
                // Horizontal spacing is fixed; top/bottom comes from system insets.
                setPadding(dp(a, 24), dp(a, 16), dp(a, 24), dp(a, 116))
                layoutParams = FrameLayout.LayoutParams(-1, -2)
            }
        sc.addView(body, FrameLayout.LayoutParams(-1, -2))
        // Один safe-area calculation для всех экранов. ignoringVisibility нужен для edge-to-edge
        // на Android 15/16, где при старте Activity inset иногда успевает прийти как 0.
        ViewCompat.setOnApplyWindowInsetsListener(sc) { view, insets ->
            val types =
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            val safe = insets.getInsetsIgnoringVisibility(types)
            val safeTop = safe.top.coerceAtLeast(dp(a, 24))
            view.setPadding(0, safeTop + dp(a, 12), 0, 0)
            insets
        }
        sc.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    ViewCompat.requestApplyInsets(v)
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            }
        )
        return sc to body
    }

    fun header(body: LinearLayout, title: String, subtitle: String? = null) {
        body.addView(serif(body.context, title, 31f, true))
        subtitle?.let {
            body.addView(text(body.context, it, 14f, false, muted), lp(dp(body.context, 7)))
        }
    }

    fun sectionTitle(
        c: Context,
        title: String,
        action: String? = null,
        onAction: (() -> Unit)? = null,
    ): LinearLayout {
        val r = row(c)
        r.gravity = Gravity.CENTER_VERTICAL
        r.addView(serif(c, title, 28f, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (action != null) {
            val a = text(c, action, 14f, true, orange)
            a.setPadding(dp(c, 8), dp(c, 8), 0, dp(c, 8))
            a.setOnClickListener {
                onAction?.invoke()
            }
            r.addView(a)
            r.addView(icon(c, R.drawable.ic_chevron, orange, 15))
        }
        return r
    }

    fun infoRow(c: Context, iconRes: Int, label: String, value: String): LinearLayout {
        val r =
            row(c).apply {
                setPadding(dp(c, 2), dp(c, 14), dp(c, 2), dp(c, 14))
            }
        val box =
            FrameLayout(c).apply {
                background = bg(paleGreen, dp(c, 13).toFloat())
                addView(
                    icon(c, iconRes, green, 19),
                    FrameLayout.LayoutParams(dp(c, 19), dp(c, 19), Gravity.CENTER),
                )
            }
        r.addView(box, LinearLayout.LayoutParams(dp(c, 40), dp(c, 40)))
        r.addView(spacer(c, 12))
        r.addView(text(c, label, 14f, true), LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(text(c, value, 14f, false, muted))
        r.addView(spacer(c, 8))
        r.addView(icon(c, R.drawable.ic_chevron, muted, 16))
        return r
    }

    fun categoryIcon(name: String) =
        when {
            name.contains("univers", true) ||
                name.contains("study", true) ||
                name.contains("education", true) -> R.drawable.ic_school
            name.contains("work", true) || name.contains("hammel", true) -> R.drawable.ic_work
            name.contains("sport", true) ||
                name.contains("run", true) ||
                name.contains("workout", true) -> R.drawable.ic_activity
            name.contains("hiking", true) -> R.drawable.ic_mountain
            name.contains("freelance", true) -> R.drawable.ic_laptop
            else -> R.drawable.ic_category
        }

    fun bottomNav(a: Activity, current: String): LinearLayout {
        val bar =
            LinearLayout(a).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = bg(surface, dp(a, 31).toFloat(), 1, Color.parseColor("#E9E3D9"))
                setPadding(dp(a, 6), dp(a, 7), dp(a, 6), dp(a, 6))
                elevation = dp(a, 12).toFloat()
            }
        val items =
            listOf(
                Triple("Home", R.drawable.ic_home, MainActivity::class.java),
                Triple("Tasks", R.drawable.ic_tasks, TasksActivity::class.java),
                Triple("Focus", R.drawable.ic_focus, FocusActivity::class.java),
                Triple("Calendar", R.drawable.ic_calendar, CalendarActivity::class.java),
                Triple("Insights", R.drawable.ic_chart, StatisticsActivity::class.java),
            )
        items.forEach { (key, ico, cls) ->
            val active = key == current
            val col =
                LinearLayout(a).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(a, 2), dp(a, 3), dp(a, 2), dp(a, 2))
                    if (active && key != "Focus") background = bg(sand, dp(a, 23).toFloat())
                    if (key == "Focus") {
                        val circle =
                            FrameLayout(a).apply {
                                background = bg(green, dp(a, 24).toFloat())
                                elevation = dp(a, 5).toFloat()
                                addView(
                                    icon(a, ico, Color.WHITE, 23),
                                    FrameLayout.LayoutParams(dp(a, 23), dp(a, 23), Gravity.CENTER),
                                )
                            }
                        addView(
                            circle,
                            LinearLayout.LayoutParams(dp(a, 48), dp(a, 48)).apply {
                                bottomMargin = dp(a, 1)
                            },
                        )
                    } else {
                        addView(
                            icon(a, ico, if (active) orange else Color.parseColor("#606963"), 21),
                            LinearLayout.LayoutParams(dp(a, 25), dp(a, 25)),
                        )
                    }
                    addView(
                        text(
                                a,
                                key,
                                11.5f,
                                active,
                                if (active) orange else Color.parseColor("#48524C"),
                            )
                            .apply {
                                gravity = Gravity.CENTER
                                typeface =
                                    Typeface.create(
                                        "sans-serif",
                                        if (active) Typeface.BOLD else Typeface.NORMAL,
                                    )
                            },
                        lp(dp(a, 3)),
                    )
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        if (key == "Focus") {
                            val nextTask =
                                DataManager.loadTasks(a)
                                    .filter {
                                        !it.completed
                                    }
                                    .sortedWith(
                                        compareBy<Task> {
                                                it.date.ifBlank {
                                                    "9999-99-99"
                                                }
                                            }
                                            .thenBy {
                                                it.time
                                            }
                                    )
                                    .firstOrNull()
                            if (nextTask != null) {
                                a.startActivity(
                                    Intent(a, FocusActivity::class.java)
                                        .putExtra("TASK_ID", nextTask.id)
                                )
                            } else {
                                a.startActivity(Intent(a, TaskActivity::class.java))
                            }
                        } else if (key != current) {
                            a.startActivity(Intent(a, cls))
                            a.finish()
                        }
                    }
                }
            bar.addView(col, LinearLayout.LayoutParams(0, dp(a, 68), 1f))
        }
        return bar
    }

    fun attachNav(root: FrameLayout, content: View, a: Activity, current: String) {
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        val nav = bottomNav(a, current)
        val params =
            FrameLayout.LayoutParams(-1, dp(a, 82), Gravity.BOTTOM).apply {
                marginStart = dp(a, 14)
                marginEnd = dp(a, 14)
                bottomMargin = dp(a, 10)
            }
        root.addView(nav, params)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bottom =
                insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom
            (nav.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = bottom + dp(a, 10)
                nav.layoutParams = this
            }
            insets
        }
        root.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    ViewCompat.requestApplyInsets(v)
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            }
        )
    }
}
