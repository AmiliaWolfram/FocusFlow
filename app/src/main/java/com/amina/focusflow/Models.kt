package com.amina.focusflow

// Data models used across tasks, goals, categories and focus sessions.
data class Category(val id: Long = System.currentTimeMillis(), var name: String, var icon: String ="●")
data class Goal(val id: Long = System.currentTimeMillis(), var title: String, var category: String ="Personal", var targetMinutes: Int = 120, var deadline: String ="", var completed: Boolean = false)
data class Task(val id: Long = System.currentTimeMillis(), var title: String, var category: String ="Personal", var duration: Int = 25, var completed: Boolean = false, var priority: String ="Medium", var goalId: Long? = null, var date: String ="", var time: String ="", var focusedMinutes: Int = 0, var categoryId: Long? = null)
data class FocusSession(val id: Long = System.currentTimeMillis(), val taskId: Long, val date: String, val minutes: Int, val distractions: Int = 0, val categoryId: Long? = null, val categoryName: String ="")
