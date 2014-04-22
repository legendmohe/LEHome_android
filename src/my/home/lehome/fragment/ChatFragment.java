package my.home.lehome.fragment;

import java.util.Random;

import my.home.lehome.R;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.adapter.ChatItemArrayAdapter;
import my.home.lehome.asynctask.SendCommandAsyncTask;
import my.home.lehome.model.ChatItem;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ChatFragment extends Fragment {
	public static final String TAG = ChatFragment.class.getName();
	
	private ChatItemArrayAdapter adapter;
	public static Handler handler;
	public static int FLAG = 1;
	private ListView cmdListview;
	private EditText sendCmdEdittext;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    	handler = new Handler(){ 
            @Override 
            public void handleMessage(Message msg) { 
                super.handleMessage(msg); 
                Log.d(TAG, "onSubscribalbeReceiveMsg : " + msg.obj);
                if(msg.what==FLAG){ 
                	adapter.add(new ChatItem(false, (String)msg.obj));
                	ChatFragment.this.scrollMyListViewToBottom();
                }
            } 
             
        };
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.chat_fragment, container, false);
        
        cmdListview = (ListView) rootView.findViewById(R.id.chat_list);
        if (adapter == null) {
        	adapter = new ChatItemArrayAdapter(this.getActivity(), R.layout.chat_item);
		}
        cmdListview.setAdapter(adapter);

		sendCmdEdittext = (EditText) rootView.findViewById(R.id.send_cmd_edittext);
		sendCmdEdittext.setOnEditorActionListener(new OnEditorActionListener() {
			  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			    if (actionId == EditorInfo.IME_ACTION_DONE) {
			    	// Perform action on key press
					String messageString = sendCmdEdittext.getText().toString();
					if (!messageString.trim().equals("")) {
						MainActivity mainActivity = (MainActivity) getActivity();
						new SendCommandAsyncTask(mainActivity).execute(messageString);
						sendCmdEdittext.setText("");
						scrollMyListViewToBottom();
					}
			      return true;
			    } else {
			      return false;
			    }
			  }

			});
		
		registerForContextMenu(cmdListview);
        return rootView;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == cmdListview.getId()) {
	        MenuInflater inflater = getActivity().getMenuInflater();
	        inflater.inflate(R.menu.add_chat_item_to_shortcut, menu);
		}
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
          AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
          switch(item.getItemId()) {
              case R.id.add_chat_item_to_shortcut:
            	  String selectedString = adapter.getItem(info.position).content;
            	  MainActivity activity = (MainActivity) getActivity();
            	  activity.getShortcurFragment().addShortcut(selectedString);
                  return true;
              default:
                    return super.onContextItemSelected(item);
          }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    
    @Override
    public void onDetach() {
    	InputMethodManager inputManager = 
    	        (InputMethodManager) getActivity().
    	            getSystemService(Context.INPUT_METHOD_SERVICE); 
    	inputManager.hideSoftInputFromWindow(
    	        getActivity().getCurrentFocus().getWindowToken(),
    	        InputMethodManager.HIDE_NOT_ALWAYS); 
    	super.onDetach();
    };
    
    private void scrollMyListViewToBottom() {
    	cmdListview.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
            	cmdListview.setSelection(adapter.getCount() - 1);
            }
        });
    }

	public ChatItemArrayAdapter getAdapter() {
		return adapter;
	}
}
