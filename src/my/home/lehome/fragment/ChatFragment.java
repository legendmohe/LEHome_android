package my.home.lehome.fragment;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import my.home.lehome.R;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.adapter.ChatItemArrayAdapter;
import my.home.lehome.asynctask.LoadMoreChatItemAsyncTask;
import my.home.lehome.asynctask.SendCommandAsyncTask;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.helper.MessageHelper;
import my.home.lehome.service.ConnectionService;
import my.home.lehome.util.JsonParser;
import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.iflytek.cloud.speech.RecognizerResult;
import com.iflytek.cloud.speech.SpeechConstant;
import com.iflytek.cloud.speech.SpeechError;
import com.iflytek.cloud.speech.SpeechListener;
import com.iflytek.cloud.speech.SpeechRecognizer;
import com.iflytek.cloud.speech.SpeechUser;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import de.greenrobot.lehome.ChatItem;
import de.greenrobot.lehome.Shortcut;

public class ChatFragment extends Fragment {
	public static final String TAG = ChatFragment.class.getName();
	
	private ChatItemArrayAdapter adapter;
	private ProgressBar sendProgressBar;
	private Toast mToast;
	public static Handler handler;
	public static int FLAG = 1;
	private int topVisibleIndex;
	private boolean keyboard_open = false;
	private boolean inSpeechMode = false;
	private OnGlobalLayoutListener keyboardListener;
	private ListView cmdListview;
	private EditText sendCmdEdittext;
	
	private RecognizerDialog iatDialog;
	private SpeechRecognizer iatRecognizer;
	
	public static int CHATITEM_LOAD_LIMIT = 20;
	public static final int CHATITEM_LOWEST_INDEX = 1;
	
    @SuppressLint("HandlerLeak")
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    	initMSC();
    	if (adapter == null) {
        	adapter = new ChatItemArrayAdapter(this.getActivity(), R.layout.chat_item);
		}
    	handler = new Handler(){ 
            @Override 
            public void handleMessage(Message msg) { 
                super.handleMessage(msg); 
                ChatItem newItem = (ChatItem) msg.obj;
                Log.d(TAG, "onSubscribalbeReceiveMsg : " + newItem.getContent());
                if(msg.what==FLAG){ 
		        	adapter.add(newItem);
                	ChatFragment.this.scrollMyListViewToBottom();
                }
            } 
             
        };
        
        SpeechUser.getUser().login(getActivity()
        		, null
        		, null
        		, "appid=" + getString(R.string.msc_app_id)
				, new SpeechListener(){
	    		@Override
	    		public void onCompleted(SpeechError error) {
	    			if(error != null) {
	    				showTip(getString(R.string.msc_login_faild));			
	    			}
	    		}
	
	    		@Override
	    		public void onData(byte[] arg0) {
	    		}
	
	    		@Override
	    		public void onEvent(int arg0, Bundle arg1) {
	    		}
	    });
    };

    @SuppressLint("ShowToast")
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.chat_fragment, container, false);
        
        cmdListview = (ListView) rootView.findViewById(R.id.chat_list);
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
					}
			      return true;
			    } else {
			      return false;
			    }
			  }

			});
        cmdListview.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (keyboard_open && scrollState == SCROLL_STATE_TOUCH_SCROLL) {
							InputMethodManager inputManager = 
									(InputMethodManager) getActivity().
									getSystemService(Context.INPUT_METHOD_SERVICE); 
							inputManager.hideSoftInputFromWindow(
									getActivity().getCurrentFocus().getWindowToken(),
									InputMethodManager.HIDE_NOT_ALWAYS); 
				}else if(scrollState == OnScrollListener.SCROLL_STATE_IDLE
						&& topVisibleIndex == 0
						&& adapter.getItem(0).getId() > CHATITEM_LOWEST_INDEX) {
					new LoadMoreChatItemAsyncTask(ChatFragment.this).execute(CHATITEM_LOAD_LIMIT);
				}
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				topVisibleIndex = firstVisibleItem;
			}
		});
    	keyboardListener = (new OnGlobalLayoutListener() {
    		@Override
    		public void onGlobalLayout() {
    			int heightDiff = getView().getRootView().getHeight() - getView().getHeight();
    			Log.d(TAG, "height" + String.valueOf(heightDiff));
    			if (heightDiff > 200) { // if more than 100 pixels, its probably a keyboard...
    				Log.d(TAG, "keyboard show.");
    				if (!keyboard_open) {
    					ChatFragment.this.scrollMyListViewToBottom();	
    				}
    				keyboard_open = true;
    			}else if(keyboard_open) {
					keyboard_open = false;
					sendCmdEdittext.clearFocus();
					cmdListview.requestFocus();
					Log.d(TAG, "keyboard hide.");
    			}
    		}
    	});
    	rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
        
        Button switchButton = (Button) rootView.findViewById(R.id.switch_input_button);
        switchButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!inSpeechMode) {
					Button switch_btn = (Button) getView().findViewById(R.id.switch_input_button);
					switch_btn.setBackgroundResource(R.drawable.chatting_setmode_voice_btn);
					getView().findViewById(R.id.speech_button).setVisibility(View.VISIBLE);
					getView().findViewById(R.id.send_cmd_edittext).setVisibility(View.INVISIBLE);
					inSpeechMode = true;
					
					if (keyboard_open) {
			    		InputMethodManager inputManager = 
			    				(InputMethodManager) getActivity().
			    				getSystemService(Context.INPUT_METHOD_SERVICE); 
			    		inputManager.hideSoftInputFromWindow(
			    				getActivity().getCurrentFocus().getWindowToken(),
			    				InputMethodManager.HIDE_NOT_ALWAYS); 
					}
				}else {
					Button switch_btn = (Button) getView().findViewById(R.id.switch_input_button);
					switch_btn.setBackgroundResource(R.drawable.chatting_setmode_msg_btn);
					getView().findViewById(R.id.speech_button).setVisibility(View.INVISIBLE);
					getView().findViewById(R.id.send_cmd_edittext).setVisibility(View.VISIBLE);
					inSpeechMode = false;
				}
			}
		});
        Button speechButton = (Button) rootView.findViewById(R.id.speech_button);
        speechButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showIatDialog();
			}
		});
        
        sendProgressBar = (ProgressBar) rootView.findViewById(R.id.send_msg_progressbar);
        sendProgressBar.setVisibility(View.INVISIBLE);
        
        mToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
        
        scrollMyListViewToBottom();
        
        return rootView;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == cmdListview.getId()) {
        	AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
	        MenuInflater inflater = getActivity().getMenuInflater();
	        ChatItem chatItem = adapter.getItem(info.position);
	        if (chatItem.getIsMe()) {
	        	inflater.inflate(R.menu.chat_item_is_me, menu);
			}else {
				inflater.inflate(R.menu.chat_item_not_me, menu);
			}
		}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
        case R.id.voice_input:
      	  showIatDialog();
      	  return true;
        default:
              return super.onOptionsItemSelected(item);
	    }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
          AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
          String selectedString = adapter.getItem(info.position).getContent();
          switch(item.getItemId()) {
              case R.id.add_chat_item_to_shortcut:
            	  MainActivity activity = (MainActivity) getActivity();
            	  if(activity.getShortcurFragment() == null) {
            		  Shortcut shortcut = new Shortcut();
            		  shortcut.setContent(selectedString);
            		  shortcut.setInvoke_count(0);
            		  shortcut.setWeight(1.0);
            		  DBHelper.addShortcut(shortcut);
            	  }else{
            		  activity.getShortcurFragment().addShortcut(selectedString);
            	  }
                  return true;
              case R.id.resend_item:
            	  MainActivity mainActivity = (MainActivity) getActivity();
            	  new SendCommandAsyncTask(mainActivity).execute(selectedString);
                  return true;
              case R.id.copy_item:
            	  ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE); 
            	  ClipData clip = ClipData.newPlainText(getString(R.string.app_name), selectedString);
            	  clipboard.setPrimaryClip(clip);
                  return true;
              case R.id.voice_input:
            	  showIatDialog();
            	  return true;
              default:
                    return super.onContextItemSelected(item);
          }
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	registerForContextMenu(cmdListview);
    	setHasOptionsMenu(true);
//		scrollMyListViewToBottom();
    }
    
    @Override
    public void onDestroyView() {
    	if (keyboard_open) {
    		InputMethodManager inputManager = 
    				(InputMethodManager) getActivity().
    				getSystemService(Context.INPUT_METHOD_SERVICE); 
    		inputManager.hideSoftInputFromWindow(
    				getActivity().getCurrentFocus().getWindowToken(),
    				InputMethodManager.HIDE_NOT_ALWAYS); 
		}
    	
    	mToast.cancel();
    	cancelRecognize();
    	if (null != iatDialog) {
			iatDialog.cancel();
		}
    	
    	View rootView = getView();
    	rootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
    	super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    
    @Override
    public void onDetach() {
    	super.onDetach();
    };
    
    @Override
    public void onResume() {
    	super.onResume();
    	MessageHelper.resetUnreadCount();
		this.resetDatas();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    }
    
    public void resetDatas() {
    	List<ChatItem> chatItems = DBHelper.loadLatest(CHATITEM_LOAD_LIMIT);
    	if (chatItems != null) {
    		Collections.reverse(chatItems); // reverse descend items
    		adapter.setData(chatItems);
		}
	}
    
    private void showTip(String str)
	{
		if(!TextUtils.isEmpty(str))
		{
			mToast.setText(str);
			mToast.show();
		}
	}
    
    public void scrollMyListViewToBottom() {
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
	
	private void initMSC() {
		iatRecognizer = SpeechRecognizer.createRecognizer(getActivity());
		iatDialog = new RecognizerDialog(getActivity());
	}
	
	protected void cancelRecognize()
	{
		if(null != iatRecognizer) {
			iatRecognizer.cancel();
		}
	}
	
	public void showIatDialog()
	{
		if(null == iatDialog) {
			iatDialog = new RecognizerDialog(getActivity());
		}

		//清空Grammar_ID，防止识别后进行听写时Grammar_ID的干扰
		iatDialog.setParameter(SpeechConstant.CLOUD_GRAMMAR, null);
		iatDialog.setParameter(SpeechConstant.DOMAIN, "iat");
		iatDialog.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
		iatDialog.setParameter(SpeechConstant.ASR_PTT, "0");
		iatDialog.setParameter(SpeechConstant.VAD_EOS, "1000");
		iatDialog.setListener(new RecognizerDialogListener() {
			@Override
			public void onResult(RecognizerResult results, boolean isLast) {
				final String resultString = JsonParser.parseIatResult(results.getResultString());
				Log.d(TAG, "result: " + resultString);
				if (!resultString.trim().equals("")) {
					SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
			        boolean need_confirm = mySharedPreferences.getBoolean("pref_speech_cmd_need_confirm", true);
			        if (!need_confirm) {
			        	MainActivity mainActivity = (MainActivity) getActivity();
			        	new SendCommandAsyncTask(mainActivity).execute(resultString);
					}else {
						AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

				    	alert.setMessage(resultString);
				    	alert.setTitle(getResources().getString(R.string.speech_cmd_need_confirm));

				    	alert.setPositiveButton(getResources().getString(R.string.com_comfirm)
				    							, new DialogInterface.OnClickListener() {
					    	public void onClick(DialogInterface dialog, int whichButton) {
					    		MainActivity mainActivity = (MainActivity) getActivity();
					        	new SendCommandAsyncTask(mainActivity).execute(resultString);
					    	}
				    	});

				    	alert.setNegativeButton(getResources().getString(R.string.com_cancel), 
				    							new DialogInterface.OnClickListener() {
				    		public void onClick(DialogInterface dialog, int whichButton) {
				    			// Canceled.
				    		}
				    	});

				    	alert.show();
					}
				}
			}
			public void onError(SpeechError error) {
				
			}
			
		});
		iatDialog.show();
	}

	public ProgressBar getSendProgressBar() {
		return sendProgressBar;
	}
}
