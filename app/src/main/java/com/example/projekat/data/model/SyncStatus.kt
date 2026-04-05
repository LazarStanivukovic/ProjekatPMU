package com.example.projekat.data.model

/**
 * Represents the synchronization status of a Note or Task.
 */
enum class SyncStatus {
    /** Item exists only locally, has never been synced */
    LOCAL_ONLY,
    
    /** Item is in sync with cloud */
    SYNCED,
    
    /** Local changes need to be uploaded to cloud */
    PENDING_UPLOAD,
    
    /** Item is marked for deletion on cloud */
    PENDING_DELETE
}
