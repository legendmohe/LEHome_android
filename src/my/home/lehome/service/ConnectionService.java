package my.home.lehome.service;

import java.util.ArrayList;

import my.home.lehome.helper.MessageHelper;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

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
	private static boolean inNormalState = true;

	public static final String TAG = "ConnectionService";
	
	private final LocalBinder subscribeBinder = new LocalBinder();
	private Socket recvMsgSocket;
	private Thread connectionThread;
	private boolean stopRunning;
	
	@Override
	public void onCreate() {
		super.onCreate();
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
                    	String[] msgStrings = recvString.split("\\|");
                    	String type = msgStrings[0];
                    	String msg = msgStrings[1];
                    	if (type.equals("normal")) {  //ugly hack
                    		inNormalState = true;
						}else {
							inNormalState = false;
						}
                    	MessageHelper.sendServerMsgToList(msg);
    				}
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
            }
            recvMsgSocket.close();
            Log.d(TAG, "thread stop......");
        }
    }
}
