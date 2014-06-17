package my.home.lehome.application;

import my.home.lehome.activity.MainActivity;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.service.ConnectionService;
import android.app.ActivityManager;
import android.app.Application;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class LEHomeApplication extends Application {
	@Override
    public void onCreate()
    {
        super.onCreate();
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK); 
        BroadcastReceiver receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				boolean isServiceRunning = false;
				if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
					ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
					for (RunningServiceInfo service : manager
							.getRunningServices(Integer.MAX_VALUE)) {
						if ("my.home.lehome.service.ConnectionService".equals(service.service.getClassName())){
							isServiceRunning = true;
							break;
						}
					}
					if (!isServiceRunning && !MainActivity.STOPPED) {
						Intent i = new Intent(context, ConnectionService.class);
						context.startService(i);
					}
				}
			}
        	
        }; 
        registerReceiver(receiver, filter); 
    }
}