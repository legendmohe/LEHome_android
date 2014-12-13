package my.home.lehome.application;

import com.baidu.frontia.FrontiaApplication;

import my.home.lehome.R;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.helper.DBHelper;
import android.app.ActivityManager;
import android.app.Application;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class LEHomeApplication extends FrontiaApplication {
	
	@Override
    public void onCreate()
    {
        super.onCreate();
    }
}
