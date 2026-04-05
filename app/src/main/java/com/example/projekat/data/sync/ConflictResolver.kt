package com.example.projekat.data.sync

import com.example.projekat.data.model.Note
import com.example.projekat.data.model.Task

/**
 * Conflict resolver using last-write-wins strategy based on updatedAt timestamp.
 */
object ConflictResolver {

    /**
     * Resolve conflict between local and cloud note.
     * Returns the note that should be kept (the one with more recent updatedAt).
     */
    fun resolveNoteConflict(local: Note, cloud: Note): Note {
        return if (local.updatedAt >= cloud.updatedAt) local else cloud
    }

    /**
     * Resolve conflict between local and cloud task.
     * Returns the task that should be kept (the one with more recent updatedAt).
     */
    fun resolveTaskConflict(local: Task, cloud: Task): Task {
        return if (local.updatedAt >= cloud.updatedAt) local else cloud
    }
}
