const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendTaskNotification = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }

  const {recipientEmail, taskTitle, senderEmail, taskId} = req.body;
  if (!recipientEmail || !taskTitle || !senderEmail || !taskId) {
    res.status(400).send("Missing required fields");
    return;
  }

  try {
    const usersSnapshot = await admin.firestore()
        .collection("users")
        .where("email", "==", recipientEmail)
        .get();

    if (usersSnapshot.empty) {
      console.log("No user found for email:", recipientEmail);
      res.status(200).send({success: false, reason: "user_not_found"});
      return;
    }

    const fcmToken = usersSnapshot.docs[0].data().fcmToken;
    if (!fcmToken) {
      console.log("No FCM token for:", recipientEmail);
      res.status(200).send({success: false, reason: "no_token"});
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
    res.status(200).send({success: true});
  } catch (error) {
    console.error("Error sending notification:", error);
    res.status(500).send({success: false, error: error.message});
  }
});
