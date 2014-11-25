package my.home.lehome.helper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import com.igexin.sdk.PushConsts;
import com.igexin.sdk.PushManager;
import com.igexin.sdk.Tag;

import de.greenrobot.lehome.ChatItem;
import my.home.lehome.R;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.application.LEHomeApplication;
import my.home.lehome.fragment.ChatFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class MessageHelper {
	
	private static final int maxNotiLen = 50;
	private static int unreadMsgCount = 0;
	public static String SERVER_ADDRESS = "";
	public static String MESSAGE_BEGIN = "";
	public static String MESSAGE_END = "";
	public static String DEVICE_ID = "";
	public static boolean inNormalState = true;
	public static boolean needCorrect = true;
	
	public final static int NOTIFICATION_ID = 1;
	
	public static void setPushTag(final Context context, String tagText) {
		String[] tags = tagText.split(",");
		final Tag[] tagParam = new Tag[tags.length];
		for (int i = 0; i < tags.length; i++) {
			Tag t = new Tag();
			t.setName(tags[i]);
			tagParam[i] = t;
		}
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				int i = PushManager.getInstance().setTag(context, tagParam);
				String text = "ERROR";

				switch (i) {
				case PushConsts.SETTAG_SUCCESS:
					text = context.getString(R.string.msg_device_binded);
					break;
				case PushConsts.SETTAG_ERROR_COUNT:
					text = context.getString(R.string.msg_device_bind_faild);
					break;
				default:
					text = context.getString(R.string.msg_device_bind_faild);
					break;
				}
				
				MessageHelper.sendToast(text);
			}
		}).start();
	}
	
	public static void delPushTag(final Context context, String tagText) {
//		String[] tags = tagText.split(",");
//		final Tag[] tagParam = new Tag[tags.length];
//		for (int i = 0; i < tags.length; i++) {
//			Tag t = new Tag();
//			t.setName(tags[i]);
//			tagParam[i] = t;
//		}
//		
//		new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				int i = PushManager.getInstance().setTag(context, tagParam);
//				String text = "ERROR";
//
//				switch (i) {
//				case PushConsts.SETTAG_SUCCESS:
//					text = context.getString(R.string.msg_device_binded);
//					break;
//				case PushConsts.SETTAG_ERROR_COUNT:
//					text = "设置标签失败，tag数量过大";
//					break;
//				default:
//					text = "设置标签失败，setTag异常";
//					break;
//				}
//				
//				MessageHelper.sendToast(text);
//			}
//		}).start();
	}
	
	public static void loadPref(Context context){
    	SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    	DEVICE_ID = mySharedPreferences.getString("pref_bind_device", "");
    	SERVER_ADDRESS = mySharedPreferences.getString("pref_server_address", "http://lehome.sinaapp.com");
    	boolean auto_complete_cmd = mySharedPreferences.getBoolean("pref_auto_add_begin_and_end", false);
    	if (auto_complete_cmd) {
    		MESSAGE_BEGIN = mySharedPreferences.getString("pref_message_begin", "");
    		MESSAGE_END = mySharedPreferences.getString("pref_message_end", "");
    		if (MESSAGE_BEGIN.endsWith("/")) {
    			MESSAGE_BEGIN = CommonHelper.removeLastChar(MESSAGE_BEGIN);
			}
    		if (MESSAGE_END.endsWith("/")) {
    			MESSAGE_END = CommonHelper.removeLastChar(MESSAGE_END);
    		}
		}else {
			MESSAGE_BEGIN = "";
			MESSAGE_END = "";
		}
    	needCorrect = mySharedPreferences.getBoolean("pref_cmd_need_correct", true);
    }
	
	public static String getFormatMessage(String content) {
		if (!inNormalState) {
			return "*" + content;
		}
		content = MESSAGE_BEGIN + content + MESSAGE_END;
		if (!needCorrect) {
			content = "*" + content;
		}
		return content;
	}
	
	public static String getServerURL(String content) {
		try {
			content = URLEncoder.encode(content, "utf-8");
		} catch (UnsupportedEncodingException e) {
			content = "";
			e.printStackTrace();
		}
		return SERVER_ADDRESS + "/cmd/put/" + content + "?id=" + DEVICE_ID;
	}
	
	public static void resetUnreadCount() {
		unreadMsgCount = 0;
	}
	
	public static boolean hasUnread() {
		return unreadMsgCount > 0 ? true : false;
	}
	
	public static void sendToast(String content) {
		Message msg = new Message();
    	msg.what = ChatFragment.TOAST; 
    	msg.obj = content;
        ChatFragment.handler.sendMessage(msg);
	}
	
	public static void sendServerMsgToList(int seq, String content, Context context) {
		ChatItem newItem = new ChatItem();
    	newItem.setContent(content);
    	newItem.setIsMe(false);
    	newItem.setDate(new Date());
    	newItem.setSeq(seq);
    	DBHelper.addChatItem(context, newItem);
    	
    	if (!((LEHomeApplication)context.getApplicationContext()).activityVisible) {
    		unreadMsgCount++;
    		content = content.replaceAll("\\s?", "");
    		int len = content.length();
    		if (len >= MessageHelper.maxNotiLen) {
				content = context.getString(R.string.noti_bref_msg, content.substring(0, maxNotiLen));
			}
    		if (unreadMsgCount <= 1) {
    			addNotification(
    					context.getString(R.string.noti_new_msg)
    					, content
    					, content
    					, context);
			}else {
				addNotification(
						context.getString(R.string.noti_new_msg)
						, context.getString(R.string.noti_num_new_msg, unreadMsgCount)
						, content
						, context
						);
			}
		}else{
			unreadMsgCount = 0;
			Message msg = new Message();
	    	msg.what = ChatFragment.FLAG; 
	    	msg.obj = newItem;
	        ChatFragment.handler.sendMessage(msg);
		}
	}
	
	private static void addNotification(String title, String content, String ticker, Context context) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
											.setSmallIcon(R.drawable.ic_launcher)
											.setAutoCancel(true)
											.setContentTitle(title)
											.setContentText(content)
											.setTicker(ticker)
											.setDefaults(Notification.DEFAULT_ALL);
		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
		builder.setContentIntent(contentIntent);
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_ID, builder.build());
	}

	// Remove notification
	private void removeNotification(Context context) {
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(NOTIFICATION_ID);
	}
}
