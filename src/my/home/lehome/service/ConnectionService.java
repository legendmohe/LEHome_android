package my.home.lehome.service;

import my.home.lehome.R;
import my.home.lehome.R.string;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.asynctask.SendCommandAsyncTask;
import my.home.lehome.helper.MessageHelper;

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
import android.util.Log;

public class ConnectionService extends Service {
	
	public static String SUBSCRIBE_ADDRESS = "";
	public static String PUBLISH_ADDRESS = "";
	public static String MESSAGE_BEGIN = "";
	public static String MESSAGE_END = "";

	public static final String TAG = "ConnectionService";
	private static final int NOTIFICATION_ID = 1;
	
	private final LocalBinder subscribeBinder = new LocalBinder();
//	private Socket sendMsgSocket;
	private Socket recvMsgSocket;
	private Thread connectionThread;
	private boolean stopRunning;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
//		Notification.Builder builder = new Notification.Builder(this); 
////		If you click the Notification while on Homescreen the last shown
////		Activity of your App will be get to the foreground without starting
////		it new. If the Activity was killed by the system you will get a 
////		new Activity.
//		Intent intent = new Intent(this, MainActivity.class);
//	    intent.setAction(Intent.ACTION_MAIN);
//	    intent.addCategory(Intent.CATEGORY_LAUNCHER);
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,    
//        		intent, 0);   
//        
//        builder.setContentIntent(contentIntent);  
//        builder.setSmallIcon(R.drawable.ic_launcher);  
//        builder.setTicker("Foreground Service Start");  
//        builder.setContentTitle("Foreground Service");  
//        builder.setContentText("Make this service run in the foreground.");  
//        Notification notification = builder.build();  
//          
//        this.startForeground(NOTIFICATION_ID, notification);
//        
        Runnable connect = new ConnectionRunnable();
        connectionThread = new Thread(connect);
        connectionThread.start();
        Log.d(TAG, "onCreate() executed");
	}
	
//	public Socket getSendMsgSocket() {
//		return sendMsgSocket;
//	}
	
	public static String getFormatMessage(String content) {
		return ConnectionService.MESSAGE_BEGIN + content + ConnectionService.MESSAGE_END;
	}
	
	public static String getFormatMessageURL(String content) {
		return ConnectionService.PUBLISH_ADDRESS + "/cmd/" + content;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand() executed");
//		return super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		stopRunning = true;
		super.onDestroy();
//		this.stopForeground(false);
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
//    		sendMsgSocket = msgContext.socket(ZMQ.PUB);
//    		sendMsgSocket.bind(ConnectionService.PUBLISH_ADDRESS);
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
                		String msgString = recvMsgSocket.recvStr(ZMQ.NOBLOCK);
                    	Log.d(TAG, "recv: " + msgString);
                    	if (msgString == null) {
        					continue;
        				}
                    	MessageHelper.sendServerMsgToList(msgString);
    				}
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
            }
            recvMsgSocket.close();
//            sendMsgSocket.close();
            Log.d(TAG, "thread stop......");
        }
    }
}
