package ir.parseh.poetry;

import com.google.firebase.messaging.FirebaseMessaging;

public final class PushBridge {
    public static void toggle(MainActivity activity) {
        boolean enabled=!activity.prefs.getBoolean("push",false);
        FirebaseMessaging messaging=FirebaseMessaging.getInstance();
        if(enabled){
            messaging.setAutoInitEnabled(true);
            messaging.subscribeToTopic("parseh-news").addOnCompleteListener(task->{
                if(task.isSuccessful()){activity.prefs.edit().putBoolean("push",true).apply();activity.settings();}
                else{messaging.setAutoInitEnabled(false);activity.toast("اتصال برقرار نشد؛ دوباره تلاش کنید.");}
            });
        }else{
            // Stop display immediately even if the device is offline during unsubscribe.
            activity.prefs.edit().putBoolean("push",false).apply();
            messaging.unsubscribeFromTopic("parseh-news");
            messaging.setAutoInitEnabled(false);
            messaging.deleteToken();
            activity.settings();
        }
    }
}
