package my.home.lehome.helper;

import java.util.Date;

import de.greenrobot.lehome.ChatItem;
import my.home.lehome.R;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.fragment.ChatFragment;
import my.home.lehome.service.ConnectionService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

public class MessageHelper {
	
	private static int unreadMsgCount = 0;
	
	public final static int NOTIFICATION_ID = 1;
	
	public static void resetUnreadCount() {
		unreadMsgCount = 0;
	}
	
	public static boolean hasUnread() {
		return unreadMsgCount > 0 ? true : false;
	}
	
	public static void sendServerMsgToList(String content, Context context) {
		ChatItem newItem = new ChatItem();
    	newItem.setContent(content);
    	newItem.setIsMe(false);
    	newItem.setDate(new Date());
    	DBHelper.addChatItem(newItem);
    	
    	if (!ConnectionService.isActivityVisible()) {
    		unreadMsgCount++;
    		if (unreadMsgCount <= 1) {
    			addNotification(
    					context.getString(R.string.noti_new_msg)
    					, content.replaceAll("\\s?", "")
    					, context);
			}else {
				addNotification(
						context.getString(R.string.noti_new_msg)
						, context.getString(R.string.noti_num_new_msg, unreadMsgCount)
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
	
	private static void addNotification(String title, String content, Context context) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
											.setSmallIcon(R.drawable.ic_launcher)
											.setAutoCancel(true)
											.setContentTitle(title)
											.setContentText(content)
											.setTicker(content)
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
