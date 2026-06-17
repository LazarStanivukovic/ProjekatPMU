const express = require("express");
const admin = require("firebase-admin");
const fs = require("fs");

let serviceAccount = null;
if (process.env.SERVICE_ACCOUNT_JSON) {
  try {
    serviceAccount = JSON.parse(process.env.SERVICE_ACCOUNT_JSON);
    console.log("Loaded service account from SERVICE_ACCOUNT_JSON env var");
  } catch (e) {
    console.error("Failed to parse SERVICE_ACCOUNT_JSON env var:", e.message);
    console.log("First 100 chars of env var:", process.env.SERVICE_ACCOUNT_JSON.substring(0, 100));
  }
} else {
  console.log("SERVICE_ACCOUNT_JSON env var not set");
}

// Fallback: try reading service_account.json from disk
if (!serviceAccount) {
  try {
    if (fs.existsSync("./service_account.json")) {
      serviceAccount = JSON.parse(fs.readFileSync("./service_account.json", "utf8"));
      console.log("Loaded service account from service_account.json file");
    }
  } catch (e) {
    console.error("Failed to read service_account.json:", e.message);
  }
}

if (serviceAccount) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
  console.log("Firebase Admin initialized");
} else {
  console.error("WARNING: No service account found. /sendNotification will fail.");
  admin.initializeApp();
}

const app = express();
app.use(express.json());

app.post("/sendNotification", async (req, res) => {
  if (!serviceAccount) {
    res.status(500).json({success: false, error: "Server not configured - no service account"});
    return;
  }

  const {recipientEmail, taskTitle, senderEmail, taskId} = req.body;
  if (!recipientEmail || !taskTitle || !senderEmail || !taskId) {
    res.status(400).json({success: false, error: "Missing required fields"});
    return;
  }

  try {
    const usersSnapshot = await admin.firestore()
      .collection("users")
      .where("email", "==", recipientEmail)
      .get();

    if (usersSnapshot.empty) {
      console.log("No user found for email:", recipientEmail);
      res.json({success: false, reason: "user_not_found"});
      return;
    }

    const fcmToken = usersSnapshot.docs[0].data().fcmToken;
    if (!fcmToken) {
      console.log("No FCM token for:", recipientEmail);
      res.json({success: false, reason: "no_token"});
      return;
    }

    await admin.messaging().send({
      token: fcmToken,
      notification: {
        title: "Novi zadatak u Inboxu",
        body: senderEmail + " vam je poslao: " + taskTitle,
      },
      data: {taskId: taskId},
      android: {
        priority: "high",
        notification: {channelId: "inbox_notifications"},
      },
    });

    console.log("Notification sent to", recipientEmail);
    res.json({success: true});
  } catch (error) {
    console.error("Error:", error);
    res.status(500).json({success: false, error: error.message});
  }
});

app.get("/health", (req, res) => {
  res.json({
    status: "ok",
    configured: !!serviceAccount,
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server listening on port", PORT);
});
