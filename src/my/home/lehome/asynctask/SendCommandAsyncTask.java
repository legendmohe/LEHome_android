package my.home.lehome.asynctask;

import java.util.Date;

import my.home.lehome.activity.MainActivity;
import my.home.lehome.adapter.ChatItemArrayAdapter;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.service.ConnectionService;

import org.zeromq.ZMQ.Socket;

import de.greenrobot.lehome.ChatItem;
import android.os.AsyncTask;
import android.util.Log;


public class SendCommandAsyncTask extends AsyncTask<String, String, Boolean> {
	
	private static final String TAG = "SendCommandAsyncTask";
	private String cmdString = "";
	private ChatItemArrayAdapter adapter;
	private ConnectionService service;
	
	public SendCommandAsyncTask(MainActivity activity) {
		this.adapter = activity.getChatFragment().getAdapter();
		this.service = activity.getConnectionService();
	}
	
	@Override
    protected Boolean doInBackground(String... cmd) {
		this.cmdString = cmd[0];
		Log.d(TAG, "sending: " + cmdString);
		Socket socket = service.getSendMsgSocket();
		String message = ConnectionService.getFormatMessage(cmdString);
		Boolean result = socket.send(message);
		return result;
    }
	
    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (result) {
        	ChatItem newItem = new ChatItem();
        	newItem.setContent(cmdString);
        	newItem.setIsMe(true);
        	newItem.setDate(new Date());
        	adapter.add(newItem);
            ((MainActivity)adapter.getContext()).getChatFragment().scrollMyListViewToBottom();
        	DBHelper.addChatItem(newItem);
		}
    }

}
