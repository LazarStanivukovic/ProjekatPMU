package com.example.projekat.notification

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmNotificationSender @Inject constructor() {
    private val client = OkHttpClient()

    // Change this to your Railway URL after deployment
    private val functionUrl = "https://projekatpmu-production.up.railway.app"

    suspend fun notifyTaskShared(
        recipientEmail: String,
        taskTitle: String,
        senderEmail: String,
        taskId: String
    ) {
        Log.i("FcmNotificationSender", "Sending FCM to $recipientEmail about '$taskTitle'")

        val json = JSONObject().apply {
            put("recipientEmail", recipientEmail)
            put("taskTitle", taskTitle)
            put("senderEmail", senderEmail)
            put("taskId", taskId)
        }

        withContext(Dispatchers.IO) {
            try {
                val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(functionUrl)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body!!.string()
                if (response.isSuccessful) {
                    Log.i("FcmNotificationSender", "Cloud Function responded: $responseBody")
                } else {
                    Log.e("FcmNotificationSender", "Cloud Function error: $responseBody")
                }
            } catch (e: Exception) {
                Log.e("FcmNotificationSender", "Failed to call Cloud Function", e)
            }
        }
    }
}
