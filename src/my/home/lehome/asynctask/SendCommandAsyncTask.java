package my.home.lehome.asynctask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;

import my.home.lehome.activity.MainActivity;
import my.home.lehome.adapter.ChatItemArrayAdapter;
import my.home.lehome.fragment.ChatFragment;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.service.ConnectionService;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.zeromq.ZMQ.Socket;

import de.greenrobot.lehome.ChatItem;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;


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
		String urlString = ConnectionService.getFormatMessageURL(Uri.encode(message));
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response;
        try {
            response = httpclient.execute(new HttpGet(urlString));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                if (out.toString().equals("ok")) {
					return true;
				} else {
                	return false;
				}
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            
        } catch (IOException e) {
            
        }
        return false;
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
