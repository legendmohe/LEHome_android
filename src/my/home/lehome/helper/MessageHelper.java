package my.home.lehome.helper;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import my.home.lehome.fragment.ChatFragment;

import org.apache.http.conn.util.InetAddressUtils;

import android.os.Message;

public class MessageHelper {
	public static void sendServerMsgToList(String content) {
		Message msg = new Message();
    	msg.what = ChatFragment.FLAG; 
    	msg.obj = content;
        ChatFragment.handler.sendMessage(msg);
	}
}
