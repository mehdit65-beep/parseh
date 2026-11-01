package ir.parseh.poetry;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    static PendingIntent alarm(Context c){return PendingIntent.getBroadcast(c,1,new Intent(c,ReminderReceiver.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
    static void schedule(Context c){AlarmManager manager=c.getSystemService(AlarmManager.class);manager.cancel(alarm(c));android.content.SharedPreferences p=c.getSharedPreferences("personal",0);if(!p.getBoolean("reminder",false))return;
        Calendar time=Calendar.getInstance();time.set(Calendar.HOUR_OF_DAY,p.getInt("hour",20));time.set(Calendar.MINUTE,p.getInt("minute",0));time.set(Calendar.SECOND,0);time.set(Calendar.MILLISECOND,0);if(time.getTimeInMillis()<=System.currentTimeMillis())time.add(Calendar.DATE,1);
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time.getTimeInMillis(),alarm(c));}
    public void onReceive(Context c,Intent intent){if(!c.getSharedPreferences("personal",0).getBoolean("reminder",false))return;schedule(c);
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager manager=c.getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("daily","یادآوری شعر",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        manager.notify(1,new Notification.Builder(c,"daily").setSmallIcon(ir.parseh.poetry.R.drawable.ic_parseh).setContentTitle("لحظه‌ای با پارسه").setContentText("وقت چند بیت شعر است؛ کتابخانهٔ آفلاین شما آماده است.").setContentIntent(open).setAutoCancel(true).build());}
}
