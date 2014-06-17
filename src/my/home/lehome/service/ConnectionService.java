package my.home.lehome.service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import my.home.lehome.helper.CommonHelper;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.helper.MessageHelper;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
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
	private Context recvContext;
	private boolean stopRunning;
	
	private boolean recvHeartbeat;
	private Lock heartbeatLock = new ReentrantLock();
	private Poller poller;

	private static boolean activityVisible;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		loadPref();
		DBHelper.initHelper(this);
		
		poller = new Poller(1);
        recvContext = ZMQ.context(1);
    	recvMsgSocket = recvContext.socket(ZMQ.SUB);
    	poller.register(recvMsgSocket, Poller.POLLIN);
		
    	Thread connectionRunnable = new Thread(new ConnectionRunnable());
    	connectionRunnable.setDaemon(true);
    	connectionRunnable.start();
        Thread checkHeartbeatRunnable = new Thread(new CheckHeartbeatRunnable());
        checkHeartbeatRunnable.setDaemon(true);
        checkHeartbeatRunnable.start();
        
        Log.i(TAG, "onCreate() executed");
	}
	
	public void loadPref(){
    	SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    	ConnectionService.SUBSCRIBE_ADDRESS = mySharedPreferences.getString("pref_sub_address", "tcp://192.168.1.102:9000");
    	ConnectionService.PUBLISH_ADDRESS = mySharedPreferences.getString("pref_pub_address", "http://192.168.1.102:8002");
    	boolean auto_complete_cmd = mySharedPreferences.getBoolean("pref_auto_add_begin_and_end", false);
    	if (auto_complete_cmd) {
    		ConnectionService.MESSAGE_BEGIN = mySharedPreferences.getString("pref_message_begin", "");
    		ConnectionService.MESSAGE_END = mySharedPreferences.getString("pref_message_end", "");
    		if (ConnectionService.MESSAGE_BEGIN.endsWith("/")) {
				ConnectionService.MESSAGE_BEGIN = CommonHelper.removeLastChar(ConnectionService.MESSAGE_BEGIN);
			}
    		if (ConnectionService.MESSAGE_END.endsWith("/")) {
    			ConnectionService.MESSAGE_END = CommonHelper.removeLastChar(ConnectionService.MESSAGE_END);
    		}
		}else {
			ConnectionService.MESSAGE_BEGIN = "";
    		ConnectionService.MESSAGE_END = "";
		}
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
		Log.i(TAG, "onStartCommand() executed");
		super.onStartCommand(intent, flags, startId);
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
	
	private void initConnection() {
		
//		if (recvMsgSocket. != null) {
//			poller.unregister(recvMsgSocket);
//			recvMsgSocket.close();
//			recvMsgSocket = null;
//			recvContext.close();
//			recvContext = null;
//		}
		try {
			recvMsgSocket.disconnect(SUBSCRIBE_ADDRESS);  // just reconnect, don't close and new
			recvMsgSocket.connect(SUBSCRIBE_ADDRESS);
			recvMsgSocket.subscribe("".getBytes());
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
    	
	}
	
	class CheckHeartbeatRunnable implements Runnable {
		
        @Override
        public void run() {
        	while(!stopRunning) {
            	try {
            		synchronized (heartbeatLock) {
            			heartbeatLock.wait(41*1000);
	            		if (!recvHeartbeat) {
	            			initConnection();
						}else {
							recvHeartbeat = false;
						}
            		}
				} catch (Exception e) {
					Log.e(TAG, Log.getStackTraceString(e));
				}
        	}
        	Log.i(TAG, "CheckHeartbeatRunnable stop......");
        }
    }
	
	class ConnectionRunnable implements Runnable {
		
        @Override
        public void run() {
        	initConnection();
        	recvHeartbeat = true;
            while(!stopRunning) {
            	try {
            		poller.poll(5000);
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
                    	if (type.equals("heartbeat")) {
                    		synchronized (heartbeatLock) {
                    			recvHeartbeat = true;
                    			heartbeatLock.notifyAll();
        					}
							continue;
						}else if (type.equals("normal")) {
                    		inNormalState = true;
						}else {
							inNormalState = false;
						}
                    	MessageHelper.sendServerMsgToList(msg, ConnectionService.this);
    				}
				} catch (Exception e) {
					Log.e(TAG, Log.getStackTraceString(e));
				}
            }
            poller.unregister(recvMsgSocket);
            recvMsgSocket.close();
            recvMsgSocket = null;
            Log.i(TAG, "ConnectionRunnable stop......");
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
