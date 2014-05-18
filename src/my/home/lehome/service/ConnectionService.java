package my.home.lehome.service;

import java.util.ArrayList;

import my.home.lehome.R;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.helper.MessageHelper;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ConnectionService extends Service {
	
	public static String SUBSCRIBE_ADDRESS = "";
	public static String PUBLISH_ADDRESS = "";
	public static String MESSAGE_BEGIN = "";
	public static String MESSAGE_END = "";
	private static boolean inNormalState = true;

	public static final String TAG = "ConnectionService";
	public static final int NOTIFICATION_ID = MessageHelper.NOTIFICATION_ID + 1;
	
	private final LocalBinder subscribeBinder = new LocalBinder();
	private Socket recvMsgSocket;
	private Thread connectionThread;
	private boolean stopRunning;

	private static boolean activityVisible;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		DBHelper.initHelper(this);
		
        Runnable connect = new ConnectionRunnable();
        connectionThread = new Thread(connect);
        connectionThread.start();
        Log.d(TAG, "onCreate() executed");
	}
	
	public static String getFormatMessage(String content) {
		if (!inNormalState) {
			return content;
		}
		return ConnectionService.MESSAGE_BEGIN + content + ConnectionService.MESSAGE_END;
	}
	
	public static String getFormatMessageURL(String content) {
		return ConnectionService.PUBLISH_ADDRESS + "/cmd/" + content;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand() executed");
		
//		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
//				.setSmallIcon(R.drawable.ic_launcher)
//				.setContentTitle(getString(R.string.app_name))
//				.setContentText(getString(R.string.app_service_running))
//				.setTicker(getString(R.string.app_service_running));
//		Intent notificationIntent = new Intent(this, MainActivity.class);
//		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
//		builder.setContentIntent(contentIntent);
//		startForeground(NOTIFICATION_ID, builder.build());
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		stopRunning = true;
		DBHelper.destory();
//		stopForeground(true);
		super.onDestroy();
		Log.d(TAG, "onDestroy() executed");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return subscribeBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}
	
	public class LocalBinder extends Binder {  
        public ConnectionService getService() {  
            return ConnectionService.this;  
        }
    }
	
	class ConnectionRunnable implements Runnable {
		
        @Override
        public void run() {
        	Context msgContext = ZMQ.context(1);
        	recvMsgSocket = msgContext.socket(ZMQ.SUB);
        	recvMsgSocket.connect(SUBSCRIBE_ADDRESS);
        	recvMsgSocket.subscribe("".getBytes());
        	
        	Poller poller = new Poller(1);
        	poller.register(recvMsgSocket, Poller.POLLIN);
        	
        	stopRunning = false;
            while(!stopRunning) {
            	try {
            		poller.poll(1000);
                	if (poller.pollin(0)) {
                		String recvString = recvMsgSocket.recvStr(ZMQ.NOBLOCK);
                    	Log.d(TAG, "recv: " + recvString);
                    	if (recvString == null) {
        					continue;
        				}
                     	JSONTokener jsonParser = new JSONTokener(recvString);
            			JSONObject cmdObject = (JSONObject) jsonParser.nextValue();
                    	String type = cmdObject.getString("type");
                    	String msg = cmdObject.getString("msg");
                    	if (type.equals("normal")) {
                    		inNormalState = true;
						}else {
							inNormalState = false;
						}
                    	MessageHelper.sendServerMsgToList(msg, ConnectionService.this);
    				}
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
            }
            recvMsgSocket.close();
            Log.d(TAG, "thread stop......");
        }
    }
	
	  public static boolean isActivityVisible() {
	    return activityVisible;
	  }  

	  public static void activityResumed() {
	    activityVisible = true;
	  }

	  public static void activityPaused() {
	    activityVisible = false;
	  }

}
