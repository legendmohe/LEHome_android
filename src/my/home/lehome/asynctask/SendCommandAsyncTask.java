package my.home.lehome.asynctask;

import my.home.lehome.activity.MainActivity;
import my.home.lehome.adapter.ChatItemArrayAdapter;
import my.home.lehome.model.ChatItem;
import my.home.lehome.service.ConnectionService;

import org.zeromq.ZMQ.Socket;

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
        adapter.add(new ChatItem(true, cmdString));
    }

}
