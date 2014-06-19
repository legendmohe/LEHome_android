package my.home.lehome.asynctask;

import java.util.Date;

import my.home.lehome.activity.MainActivity;
import my.home.lehome.fragment.ChatFragment;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.service.ConnectionService;

import org.zeromq.ZMQ;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import de.greenrobot.lehome.ChatItem;


public class SendCommandAsyncTask extends AsyncTask<String, String, Boolean> {
	
	private static final String TAG = "SendCommandAsyncTask";
	private String cmdString = "";
	private ChatFragment fragment;
	
	public SendCommandAsyncTask(MainActivity activity) {
		this.fragment = activity.getChatFragment();
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

		String message = ConnectionService.getFormatMessage(cmdString);
		
		ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REQ);
        socket.setLinger(0);
        socket.connect(ConnectionService.PUBLISH_ADDRESS);
        socket.send(message, 0);
        
        ZMQ.Poller poller = new ZMQ.Poller(1);
        poller.register(socket, ZMQ.Poller.POLLIN);
        poller.poll(7*1000);
        boolean succeed = false;
        if (poller.pollin(0)) {
        	String result = new String(socket.recvStr());
        	if (result.equals("ok")) {
        		succeed = true;
        	}
		}
 
        socket.close();
        context.term();
		
        return succeed;
    }
	
    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
    	ChatItem newItem = new ChatItem();
    	newItem.setContent(cmdString);
    	newItem.setIsMe(true);
    	newItem.setSucceed(result);
    	newItem.setDate(new Date());
    	fragment.getAdapter().add(newItem);
        ((MainActivity)fragment.getAdapter().getContext()).getChatFragment().scrollMyListViewToBottom();
    	DBHelper.addChatItem(newItem);
        this.fragment.getSendProgressBar().setVisibility(View.INVISIBLE);
    }

}
