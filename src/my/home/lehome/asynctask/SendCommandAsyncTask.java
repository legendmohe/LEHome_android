package my.home.lehome.asynctask;

import java.util.Date;

import my.home.lehome.activity.MainActivity;
import my.home.lehome.adapter.ChatItemArrayAdapter;
import my.home.lehome.fragment.ChatFragment;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.service.ConnectionService;

import org.zeromq.ZMQ.Socket;

import de.greenrobot.lehome.ChatItem;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;


public class SendCommandAsyncTask extends AsyncTask<String, String, Boolean> {
	
	private static final String TAG = "SendCommandAsyncTask";
	private String cmdString = "";
	private ChatFragment fragment;
	private ConnectionService service;
	
	public SendCommandAsyncTask(MainActivity activity) {
		this.fragment = activity.getChatFragment();
		this.service = activity.getConnectionService();
	}
	
	@Override
	protected void onPreExecute() {
		this.fragment.getSendProgressBar().setVisibility(View.VISIBLE);
		super.onPreExecute();
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
        	fragment.getAdapter().add(newItem);
            ((MainActivity)fragment.getAdapter().getContext()).getChatFragment().scrollMyListViewToBottom();
        	DBHelper.addChatItem(newItem);
		}
        this.fragment.getSendProgressBar().setVisibility(View.INVISIBLE);
    }

}
