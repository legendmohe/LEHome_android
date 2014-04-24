package my.home.lehome.helper;

import my.home.lehome.fragment.ChatFragment;
import android.os.Message;

public class MessageHelper {
	public static void sendServerMsgToList(String content) {
		Message msg = new Message();
    	msg.what = ChatFragment.FLAG; 
    	msg.obj = content;
        ChatFragment.handler.sendMessage(msg);
	}
	
	public static void sendClientMsgToServer(String content) {
		
	}
}
