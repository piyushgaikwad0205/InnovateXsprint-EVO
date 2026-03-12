package com.sagar.app;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class FirebaseHelper {
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TASKS = "tasks";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Gets the collection reference for the current user's tasks.
     * Path: users/{userId}/tasks/
     */
    public CollectionReference getTasksCollection() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return null;

        return db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .collection(COLLECTION_TASKS);
    }

    /**
     * Adds a new task to Firestore.
     */
    public void addTask(TaskModel task) {
        CollectionReference col = getTasksCollection();
        if (col != null) {
            col.add(task.toMap());
        }
    }

    /**
     * Retrieves tasks sorted by timestamp.
     */
    public Query getTasksQuery() {
        CollectionReference col = getTasksCollection();
        if (col == null)
            return null;

        return col.orderBy("timestamp", Query.Direction.DESCENDING);
    }

    /**
     * Updates a task's completion status.
     */
    public void updateTaskStatus(String taskId, boolean isCompleted) {
        CollectionReference col = getTasksCollection();
        if (col != null) {
            col.document(taskId).update("completed", isCompleted);
        }
    }

    /**
     * Deletes a task.
     */
    public void deleteTask(String taskId) {
        CollectionReference col = getTasksCollection();
        if (col != null) {
            col.document(taskId).delete();
        }
    }
}
