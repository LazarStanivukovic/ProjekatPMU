package com.example.projekat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.projekat.ui.screens.calendar.CalendarScreen
import com.example.projekat.ui.screens.notes.NoteDetailScreen
import com.example.projekat.ui.screens.notes.NotesScreen
import com.example.projekat.ui.screens.tasks.TaskDetailScreen
import com.example.projekat.ui.screens.tasks.TasksScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Notes.route,
        modifier = modifier
    ) {
        composable(Screen.Notes.route) {
            NotesScreen(
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                },
                onCreateNote = {
                    navController.navigate(Screen.NoteCreate.route)
                }
            )
        }

        composable(Screen.Tasks.route) {
            TasksScreen(
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onCreateTask = {
                    navController.navigate(Screen.TaskCreate.route)
                }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                }
            )
        }

        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
            NoteDetailScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NoteCreate.route) {
            NoteDetailScreen(
                noteId = null,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            TaskDetailScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() },
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                }
            )
        }

        composable(Screen.TaskCreate.route) {
            TaskDetailScreen(
                taskId = null,
                onBack = { navController.popBackStack() },
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                }
            )
        }
    }
}
