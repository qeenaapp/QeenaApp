package com.aneeq.aneeqrider.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.aneeq.aneeqrider.common.Common;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.aneeq.aneeqrider.R;
import com.aneeq.aneeqrider.RateActivity;

import java.util.Map;

public class MyFirebaseMessaging extends FirebaseMessagingService {


    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        if (remoteMessage.getData() != null) {
            Map<String, String> data = remoteMessage.getData();
            String title = data.get("title");
            final String message = data.get("message");

            switch (title) {
                case "Cancel":
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MyFirebaseMessaging.this, message, Toast.LENGTH_SHORT).show();
                            Toast.makeText(MyFirebaseMessaging.this, "Sending new request", Toast.LENGTH_SHORT).show();
                            Common.requestPickupHere(
                                    FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                    Common.mLastLocation,
                                    Common.mUserMarker,
                                    Common.mMap,
                                    MyFirebaseMessaging.this
                            );
                        }
                    });
                    break;
                case "Arrived":
                    showArrivedNotification(message);
                    break;
                case "DropOff":
                    openRateActivity(message);
                    break;
            }
        }
    }

    private void openRateActivity(String body) {
        Intent intent = new Intent(this, RateActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }

    private void showArrivedNotification(String body) {
        // This code works only for API 25 and below >> see notification channel
        PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(), 0 , new Intent(), PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());

        builder.setAutoCancel(true)
                .setDefaults(android.app.Notification.DEFAULT_LIGHTS| android.app.Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Arrived")
                .setContentText(body)
                .setContentIntent(contentIntent);
        NotificationManager manager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());

    }
}
