package com.syncloudsoft.taktak.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.events.MessageEvent;
import com.syncloudsoft.taktak.workers.DeviceTokenWorker;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FirebaseMessageService extends FirebaseMessagingService {

    private static final String CONTENT_SENT_MESSAGE = "sent_message";
    private static final String TAG = "FirebaseMessageService";

    @Override
    public void onMessageReceived(@NotNull RemoteMessage message) {
        Log.d(TAG, "Received message from " + message.getFrom());
        Map<String, String> data = message.getData();
        boolean im = data.containsKey("content")
                && TextUtils.equals(data.get("content"), CONTENT_SENT_MESSAGE);
        Log.v(TAG, "Is message an IM? " + im);
        if (im && data.containsKey("thread_id")) {
            //noinspection ConstantConditions
            int thread = Integer.parseInt(data.get("thread_id"));
            Log.v(TAG, "Thread ID for im is " + thread);
            if (EventBus.getDefault().hasSubscriberForEvent(MessageEvent.class)) {
                EventBus.getDefault().post(new MessageEvent(thread));
                return;
            }
        }

        RemoteMessage.Notification notification = message.getNotification();
        if (notification != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            String id = getString(R.string.notification_channel_id);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, id)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setContentText(notification.getBody())
                    .setContentTitle(notification.getTitle())
                    .setSmallIcon(R.mipmap.ic_launcher);
            if (notification.getImageUrl() != null) {
                builder.setLargeIcon(downloadImage(notification.getImageUrl()));
            }

            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel =
                        new NotificationChannel(
                                id, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(channel);
            }

            nm.notify(0, builder.build());
        }
    }

    @Override
    public void onNewToken(@NonNull String token1) {
        Log.d(TAG, "Refreshed FCM token is " + token1);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString(SharedConstants.PREF_FCM_TOKEN, token1).apply();
        String token2 = prefs.getString(SharedConstants.PREF_SERVER_TOKEN, null);
        Data input = new Data.Builder()
                .putString(DeviceTokenWorker.KEY_FCM_TOKEN, token1)
                .putString(DeviceTokenWorker.KEY_SERVER_TOKEN, token2)
                .build();
        WorkRequest request = new OneTimeWorkRequest.Builder(DeviceTokenWorker.class)
                .setInputData(input)
                .build();
        WorkManager.getInstance(this).enqueue(request);
        String topic = getString(R.string.notification_topic);
        FirebaseMessaging.getInstance()
                .subscribeToTopic(topic)
                .addOnCompleteListener(task ->
                        Log.v(TAG, "Subscription to " + topic + " was " + task.isSuccessful() + "."));
    }

    public Bitmap downloadImage(Uri url) {
        try {
            Request request = new Request.Builder().url(url.toString()).get().build();
            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                //noinspection ConstantConditions
                return BitmapFactory.decodeStream(response.body().byteStream());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to download notification image.", e);
        }

        return null;
    }
}
