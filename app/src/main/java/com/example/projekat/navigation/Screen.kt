package com.example.projekat.navigation

sealed class Screen(val route: String) {
    data object Notes : Screen("notes")
    data object Tasks : Screen("tasks")
    data object Calendar : Screen("calendar")
    data object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: String) = "note_detail/$noteId"
    }
    data object NoteCreate : Screen("note_create")
    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
    data object TaskCreate : Screen("task_create")
}
