const express = require("express");
const admin = require("firebase-admin");

let serviceAccount;
if (process.env.SERVICE_ACCOUNT_JSON) {
  serviceAccount = JSON.parse(process.env.SERVICE_ACCOUNT_JSON);
} else {
  serviceAccount = require("./service_account.json");
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const app = express();
app.use(express.json());

app.post("/sendNotification", async (req, res) => {
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
  res.json({status: "ok"});
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server listening on port", PORT);
});
