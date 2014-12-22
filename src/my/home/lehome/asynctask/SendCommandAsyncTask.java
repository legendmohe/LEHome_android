package my.home.lehome.asynctask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import my.home.lehome.R;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.fragment.ChatFragment;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.helper.MessageHelper;

//import org.zeromq.ZMQ;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import de.greenrobot.lehome.ChatItem;


public class SendCommandAsyncTask extends AsyncTask<Void, String, String> {
	
	private static final String TAG = "SendCommandAsyncTask";
	private String cmdString = "";
	private ChatFragment fragment;
	private Context context;
	
	public SendCommandAsyncTask(Context context, String cmdString) {
		if (context instanceof MainActivity) {
			this.fragment = ((MainActivity)context).getChatFragment();
		}else {
			this.fragment = null;
		}
		this.context = context;
		this.cmdString = cmdString;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.fragment.getSendProgressBar().setVisibility(View.VISIBLE);
		ChatItem newItem = new ChatItem();
    	newItem.setContent(cmdString);
    	newItem.setIsMe(true);
    	newItem.setSucceed(true); // always set true
    	newItem.setDate(new Date());
    	DBHelper.addChatItem(this.context, newItem);
    	
    	if (this.fragment != null) {
    		fragment.getAdapter().add(newItem);
    		((MainActivity)fragment.getAdapter().getContext()).getChatFragment().scrollMyListViewToBottom();
		}
	}
	
	@Override
    protected String doInBackground(Void... cmd) {
		Log.d(TAG, "sending: " + cmdString);
		
		if (TextUtils.isEmpty(MessageHelper.DEVICE_ID)) {
			return this.context.getResources().getString(R.string.msg_no_deviceid);
		}
		
		String message = MessageHelper.getFormatMessage(cmdString);
		String targetURL = MessageHelper.getServerURL(message);
		
		HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(targetURL));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                responseString = statusLine.getReasonPhrase();
            }
        } catch (ClientProtocolException e) {
        	responseString = e.toString();
        } catch (IOException e) {
        	responseString = e.toString();
        }
        return responseString;
    }
	
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        ChatItem newItem = new ChatItem();
    	newItem.setContent(result);
    	newItem.setIsMe(false);
    	newItem.setSucceed(true); // always set true
    	newItem.setDate(new Date());
    	DBHelper.addChatItem(this.context, newItem);
    	
    	if (this.fragment != null) {
    		fragment.getAdapter().add(newItem);
    		((MainActivity)fragment.getAdapter().getContext()).getChatFragment().scrollMyListViewToBottom();
    		this.fragment.getSendProgressBar().setVisibility(View.INVISIBLE);
		}
    }

}
