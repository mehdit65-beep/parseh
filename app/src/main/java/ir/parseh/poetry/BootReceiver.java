package ir.parseh.poetry;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {public void onReceive(Context context,Intent intent){if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))ReminderReceiver.schedule(context);}}
