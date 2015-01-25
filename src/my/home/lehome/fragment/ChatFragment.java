package my.home.lehome.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.baidu.voicerecognition.android.VoiceRecognitionConfig;
import com.baidu.voicerecognition.android.ui.BaiduASRDigitalDialog;
import com.baidu.voicerecognition.android.ui.DialogRecognitionListener;

import my.home.lehome.R;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.activity.WakeupActivity;
import my.home.lehome.adapter.ChatItemArrayAdapter;
import my.home.lehome.asynctask.LoadMoreChatItemAsyncTask;
import my.home.lehome.asynctask.SendCommandAsyncTask;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.helper.MessageHelper;
import my.home.lehome.receiver.MediaButtonIntentReceiver;
import my.home.lehome.util.Constants;
import my.home.lehome.util.JsonParser;
import my.home.lehome.view.SpeechDialog;
import my.home.lehome.view.SpeechDialog.SpeechDialogResultListener;
import my.home.lehome.view.SpeechDialog.State;
import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

//import com.iflytek.cloud.speech.RecognizerResult;
//import com.iflytek.cloud.speech.SpeechConstant;
//import com.iflytek.cloud.speech.SpeechError;
//import com.iflytek.cloud.speech.SpeechListener;
//import com.iflytek.cloud.speech.SpeechUser;
//import com.iflytek.cloud.ui.RecognizerDialog;
//import com.iflytek.cloud.ui.RecognizerDialogListener;

import de.greenrobot.lehome.ChatItem;
import de.greenrobot.lehome.Shortcut;

public class ChatFragment extends Fragment implements SpeechDialogResultListener {
	public static final String TAG = ChatFragment.class.getName();
	
	private ChatItemArrayAdapter adapter;
	private ProgressBar sendProgressBar;
	private Button switchButton;
	
	private Toast mToast;
	public static Handler handler;
	public static int FLAG = 1;
	public static int TOAST = 2;
	public static int VOICE_CMD = 3;
	private int topVisibleIndex;
	private boolean keyboard_open = false;
	private boolean inSpeechMode = false;
	private OnGlobalLayoutListener keyboardListener;
	private ListView cmdListview;
	private AutoCompleteTextView sendCmdEdittext;
	private ArrayAdapter<String> autoCompleteAdapter;
	
	private HashSet<String> autoCompleteHashSet = new HashSet<String>();
	
	private BaiduASRDigitalDialog mDialog;
	SpeechDialog mSpeechDialog;
	private boolean scriptInputMode;
	public boolean inRecogintion = false;
	private int mScreenWidth = 0;
	private int mScreenHeight = 0;
	
	public static final int CHATITEM_LOAD_LIMIT = 20;
	public static final int CHATITEM_LOWEST_INDEX = 1;
	public static final float DIALOG_CANCEL_Y_PERSENT = 0.57f;
	
    @SuppressLint("HandlerLeak")
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    	if (adapter == null) {
        	adapter = new ChatItemArrayAdapter(this.getActivity(), R.layout.chat_item);
		}
    	handler = new Handler(){ 
            @Override 
            public void handleMessage(Message msg) { 
                super.handleMessage(msg); 
                if(msg.what==FLAG){ 
                	ChatItem newItem = (ChatItem) msg.obj;
                	if (newItem != null) {
                		Log.d(TAG, "onSubscribalbeReceiveMsg : " + newItem.getContent());
                		adapter.add(newItem);
                		ChatFragment.this.scrollMyListViewToBottom();
					}
                }else if(msg.what == TOAST) {
                	if(getActivity() != null) {
                		Context context = getActivity().getApplicationContext();
                		if (context != null) {
                			Toast.makeText(
                					context
                					, (String) msg.obj
                					, Toast.LENGTH_SHORT)
                					.show();
						}
                	}
                }else if(msg.what == VOICE_CMD) {
                	startRecognize(getActivity());
                }
            } 
             
        };
        
    };
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    public static boolean sendMessage(Message msg) {
		if (ChatFragment.handler != null) {
	        ChatFragment.handler.sendMessage(msg);
	        return true;
		}
		return false;
	}

    @SuppressLint("ShowToast")
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.chat_fragment, container, false);
        
        cmdListview = (ListView) rootView.findViewById(R.id.chat_list);
        cmdListview.setAdapter(adapter);

		
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
    	
    	
    	final Button clearButton = (Button) rootView.findViewById(R.id.cmd_clear_button);
    	clearButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				sendCmdEdittext.setText("");
			}
		});
        
    	switchButton = (Button) rootView.findViewById(R.id.switch_input_button);
        switchButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!inSpeechMode) {
					Button switch_btn = (Button) getView().findViewById(R.id.switch_input_button);
					switch_btn.setBackgroundResource(R.drawable.chatting_setmode_voice_btn);
					getView().findViewById(R.id.speech_button).setVisibility(View.VISIBLE);
					getView().findViewById(R.id.send_cmd_edittext).setVisibility(View.INVISIBLE);
					inSpeechMode = true;
					clearButton.setVisibility(View.GONE);
					
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
					clearButton.setVisibility(View.VISIBLE);
				}
			}
		});
        mSpeechDialog = new SpeechDialog();
        Button speechButton = (Button) rootView.findViewById(R.id.speech_button);
//        speechButton.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				startRecognize(getActivity());
//			}
//		});
        
        speechButton.setOnTouchListener(new OnTouchListener() {
        	
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                    if (!mSpeechDialog.isVisible()) {
                    	Log.d(TAG, "show mSpeechDialog");
                    	mSpeechDialog.setup(getActivity(), ChatFragment.this);
                    	mSpeechDialog.show(getFragmentManager(), TAG);
					}
                    return true;
                }else if (event.getAction() == MotionEvent.ACTION_UP ) {
                    if (event.getRawY()/mScreenHeight <= DIALOG_CANCEL_Y_PERSENT) {
                    	Log.d(TAG, "cancelListening.");
                    	mSpeechDialog.cancelListening();
					}else {
						Log.d(TAG, "finishListening.");
						mSpeechDialog.finishListening();
					}
                    return true;
                }else if (event.getAction() == MotionEvent.ACTION_MOVE ) {
//                	Log.d(TAG, String.valueOf(event.getRawY()/mScreenHeight));
                	if (mSpeechDialog != null) {
                		if (event.getRawY()/mScreenHeight <= DIALOG_CANCEL_Y_PERSENT) {
                			mSpeechDialog.setReleaseCancelVisible(true);
                		}else {
                			mSpeechDialog.setReleaseCancelVisible(false);
                		}
					}
                    return true;
                }
				return false;
			}
		});
        
        sendCmdEdittext = (AutoCompleteTextView) rootView.findViewById(R.id.send_cmd_edittext);
		sendCmdEdittext.setOnEditorActionListener(new OnEditorActionListener() {
			  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			    if (actionId == EditorInfo.IME_ACTION_DONE) {
			    	// Perform action on key press
					String messageString = sendCmdEdittext.getText().toString();
					if (!messageString.trim().equals("")) {
						MainActivity mainActivity = (MainActivity) getActivity();
						new SendCommandAsyncTask(mainActivity, messageString).execute();
						ChatFragment.this.addCmdHistory(messageString);
						sendCmdEdittext.setText("");
					}
			      return true;
			    } else {
			      return false;
			    }
			  }

			});
		sendCmdEdittext.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() > 0) {
					clearButton.setVisibility(View.VISIBLE);
				}else {
					clearButton.setVisibility(View.GONE);
				}
			}
		});
		
		keyboardListener = (new OnGlobalLayoutListener() {
    		@Override
    		public void onGlobalLayout() {
    			int heightDiff = getView().getRootView().getHeight() - getView().getHeight();
    			Log.v(TAG, "height" + String.valueOf(heightDiff));
    			if (heightDiff > 200) { // if more than 100 pixels, its probably a keyboard...
    				Log.v(TAG, "keyboard show.");
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
        
        sendProgressBar = (ProgressBar) rootView.findViewById(R.id.send_msg_progressbar);
        sendProgressBar.setVisibility(View.INVISIBLE);
        
        mToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
        
        scrollMyListViewToBottom();
        
        setupBaiduVoice(getActivity());
        
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
        	scriptInputMode = true;
        	startRecognize(getActivity());
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
            		  DBHelper.addShortcut(this.getActivity(), shortcut);
            	  }else{
            		  activity.getShortcurFragment().addShortcut(selectedString);
            	  }
                  return true;
              case R.id.resend_item:
            	  MainActivity mainActivity = (MainActivity) getActivity();
            	  new SendCommandAsyncTask(mainActivity, selectedString).execute();
            	  this.addCmdHistory(selectedString);
                  return true;
              case R.id.copy_item:
            	  ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE); 
            	  ClipData clip = ClipData.newPlainText(getString(R.string.app_name), selectedString);
            	  clipboard.setPrimaryClip(clip);
                  return true;
              case R.id.copy_to_input:
            	  if (!TextUtils.isEmpty(selectedString)) {
            		  sendCmdEdittext.append(selectedString);
            		  	if (inSpeechMode) {
			    			switchButton.performClick();
						}
            	  }
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
    	
    	Display display = getActivity().getWindowManager().getDefaultDisplay();
    	Point size = new Point();
    	display.getSize(size);
    	mScreenWidth = size.x;
    	mScreenHeight = size.y;
//		scrollMyListViewToBottom();
    }
    
    @Override
    public void onDestroyView() {
    	super.onDestroyView();
    	if (keyboard_open) {
    		InputMethodManager inputManager = 
    				(InputMethodManager) getActivity().
    				getSystemService(Context.INPUT_METHOD_SERVICE); 
    		inputManager.hideSoftInputFromWindow(
    				getActivity().getCurrentFocus().getWindowToken(),
    				InputMethodManager.HIDE_NOT_ALWAYS); 
		}
    	
    	mToast.cancel();
    	if (null != mDialog) {
    		mDialog.dismiss();
		}
    	if (null != mSpeechDialog && mSpeechDialog.isVisible()) {
			mSpeechDialog.dismiss();
		}
    	
    	View rootView = getView();
    	rootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
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
    	Log.d(TAG, "onResume");
    	
    	MessageHelper.resetUnreadCount();
		this.resetDatas();
		
		this.registerBTSCO(getActivity().getApplicationContext());
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(TAG, "onPause");
    	this.unregisterBTSCO(getActivity().getApplicationContext());
    	this.closeSCO(getActivity().getApplicationContext());
    }
    
    @Override
    public void onStart() {
		sendCmdEdittext.setAdapter(this.setupAutoComplete(getActivity()));
    	super.onStart();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	this.saveCmdHistory(getActivity());
    }
    
    // =========================================================================================

    
    private ArrayAdapter<String> setupAutoComplete(Context context) {
    	SharedPreferences pref = context.getSharedPreferences(Constants.PREF_NAME, 0);
    	Set<String> cmdSet = pref.getStringSet(Constants.CMD_HISTORY_PREF_NAME, new HashSet<String>());
    	
    	autoCompleteHashSet.clear();
    	autoCompleteHashSet.addAll(cmdSet);
    	autoCompleteAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
    	autoCompleteAdapter.addAll(cmdSet);
    	
    	Log.d(TAG, "setupAutoCompleteArrayAdapter: " + cmdSet.size());
    	return autoCompleteAdapter;
    }
    
    private void addCmdHistory(String cmd) {
    	if (!autoCompleteHashSet.contains(cmd)) {
			autoCompleteHashSet.add(cmd);
			autoCompleteAdapter.add(cmd);
			
			Log.d(TAG, "addCmdHistory: " + cmd);
		}
    }
    
    private void saveCmdHistory(Context context) {
    	SharedPreferences pref = context.getSharedPreferences(Constants.PREF_NAME, 0);
    	SharedPreferences.Editor editor = pref.edit();
    	editor.putStringSet(Constants.CMD_HISTORY_PREF_NAME, autoCompleteHashSet);
    	editor.commit();
    	
    	Log.d(TAG, "saveCmdHistory: " + autoCompleteHashSet.size());
    }
    
    public void resetDatas() {
    	List<ChatItem> chatItems = DBHelper.loadLatest(this.getActivity(), CHATITEM_LOAD_LIMIT);
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
	
	/***
	 * ========================s2t===========================
	 */
	
    
    private void setupBaiduVoice(final Context context){
    	Bundle params=new Bundle();
    	//设置开放 API Key
    	params.putString(BaiduASRDigitalDialog.PARAM_API_KEY, Constants.BAIDUVOICE_API_KEY);
    	//设置开放平台 Secret Key
    	params.putString(BaiduASRDigitalDialog.PARAM_SECRET_KEY, Constants.BAIDUVOICE_SECRET_KEY);
    	//设置识别领域：搜索、输入、地图、音乐……，可选。默认为输入。
    	params.putInt(BaiduASRDigitalDialog.PARAM_PROP, VoiceRecognitionConfig.PROP_INPUT);
    	//设置语种类型：中文普通话，中文粤语，英文，可选。默认为中文普通话
    	params.putString( BaiduASRDigitalDialog.PARAM_LANGUAGE,VoiceRecognitionConfig.LANGUAGE_CHINESE);
    	// 设置对话框主题，可选。BaiduASRDigitalDialog ᨀ供了蓝、暗、红、绿、橙四中颜色，每种颜
    	//色又分亮、暗两种色调。共 8 种主题，开发者可以按需选择，取值参考 BaiduASRDigitalDialog 中
    	//前缀为 THEME_的常量。默认为亮蓝色
    	params.putInt(BaiduASRDigitalDialog.PARAM_DIALOG_THEME, BaiduASRDigitalDialog.THEME_GREEN_LIGHTBG);
    	
    	params.putBoolean(BaiduASRDigitalDialog.PARAM_PARTIAL_RESULTS, false);
    	
    	mDialog = new BaiduASRDigitalDialog(context, params);
    	mDialog.setDialogRecognitionListener(mRecognitionListener);
    	mDialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				Log.v(TAG, "RecognizerDialog dismiss.");
				if (sco_on) {
		        	closeSCO(context);
				}
				inRecogintion = false;
			}
		});
    }
	
	private DialogRecognitionListener mRecognitionListener=new DialogRecognitionListener(){
		 @Override
		public void onResults(Bundle results){
			//在 Results 中获取 Key 为 DialogRecognitionListener .RESULTS_RECOGNITION 的
			//StringArrayList，可能为空。获取到识别结果后执行相应的业务逻辑即可，此回调会在主线程调用。
			ArrayList<String> rs = results != null ? results.getStringArrayList(RESULTS_RECOGNITION) : null;
			if(rs == null){
				inRecogintion = false;
				return;
			}
			//此处处理识别结果，识别结果可能有多个，按置信度从高到低排列，第一个元素是置信度最高的结果。
			Log.d(TAG, rs.toString());
			
			String resultString = rs.get(0);
			if (scriptInputMode == true) {
				resultString = "运行脚本#" + resultString + "#";
				scriptInputMode = false;
			}
			final String msgString = resultString;
			final Context context = getActivity();
			
			Log.d(TAG, "result: " + msgString);
			
			if (!msgString.trim().equals("")) {
				SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		        boolean need_confirm = mySharedPreferences.getBoolean("pref_speech_cmd_need_confirm", true);
		        if (!need_confirm) {
		        	new SendCommandAsyncTask(context, msgString).execute();
		        	ChatFragment.this.addCmdHistory(msgString);
		        	inRecogintion = false;
				}else {
					AlertDialog.Builder alert = new AlertDialog.Builder(context);

			    	alert.setMessage(msgString);
			    	alert.setTitle(getResources().getString(R.string.speech_cmd_need_confirm));

			    	alert.setNeutralButton(getResources().getString(R.string.com_send_to_edittext)
			    							, new DialogInterface.OnClickListener() {
				    	public void onClick(DialogInterface dialog, int whichButton) {
				    		sendCmdEdittext.append(msgString);
				    		if (inSpeechMode) {
				    			switchButton.performClick();
							}
				    		inRecogintion = false;
				    	}
			    	});
			    	
			    	alert.setPositiveButton(getResources().getString(R.string.com_comfirm)
			    							, new DialogInterface.OnClickListener() {
				    	public void onClick(DialogInterface dialog, int whichButton) {
				        	new SendCommandAsyncTask(context, msgString).execute();
				        	ChatFragment.this.addCmdHistory(msgString);
				        	inRecogintion = false;
				    	}
			    	});

			    	alert.setNegativeButton(getResources().getString(R.string.com_cancel), 
			    							new DialogInterface.OnClickListener() {
			    		public void onClick(DialogInterface dialog, int whichButton) {
			    			inRecogintion = false;
			    		}
			    	});

			    	alert.show();
				}
			}else {
				inRecogintion = false;
			}
		}
	};
		
	
	public void startRecognize(Context context) {
		SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean auto_sco = mySharedPreferences.getBoolean("pref_auto_connect_sco", true);
        Log.d(TAG, "auto_sco: " + auto_sco);
		
        inRecogintion = true;
        if (auto_sco && isBTSCOConnected(context)) {
			Log.d(TAG, "bt headset is connected. recognize with sco.");
			IatWithBTSCO(context);
		}else {
			Log.d(TAG, "open iat dialog.");
			showVoiceDialog(context);
		}
	}
	
	public void showVoiceDialog(final Context context)
	{
		mDialog.show();
	}

	public ProgressBar getSendProgressBar() {
		return sendProgressBar;
	}
	
	/***
	 * ========================bt sco===========================
	 */
	
//	private boolean bt_on = false;
	private boolean sco_on = false;
	
	private BroadcastReceiver btBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
            if (BluetoothHeadset.STATE_AUDIO_CONNECTED == state) { 
            	sco_on = true;
            	Log.d(TAG, "SCO_AUDIO_STATE_CONNECTED " + state);
            	showVoiceDialog(getActivity());
            }else if (BluetoothHeadset.STATE_AUDIO_DISCONNECTED == state) {
            	Log.d(TAG, "SCO_AUDIO_STATE_DISCONNECTED " + state);
            	sco_on = false;
            }
        }
	};;
	
	private boolean isBTSCOConnected(Context connect) {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
		    return false;
		}
		int pState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
		if (pState == BluetoothProfile.STATE_CONNECTED) {
			return true;
		}
		return false;
	}
	
	private void registerBTSCO(Context context) {
		Log.d(TAG, "registerBTSCO");
		context.registerReceiver(btBroadcastReceiver, 
				new IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED));
	}
	
	private void unregisterBTSCO(Context context) {
		Log.d(TAG, "unregisterBTSCO");
		context.unregisterReceiver(btBroadcastReceiver);
	}
	
	public boolean IatWithBTSCO(Context context) {
		openSCO(context);
		return true;
	}
	
	private void openSCO(Context context) {
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (!am.isBluetoothScoOn()) {
			Log.d(TAG, "connecting to bluetooth sco");
			am.startBluetoothSco();
		}
	}
	
	private void closeSCO(Context context) {
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (am.isBluetoothScoOn()) {
			Log.d(TAG, "closing bluetooth sco");
			am.stopBluetoothSco();
		}
	}

	@Override
	public void onResult(List<String> results) {
		Log.d(TAG, "onResult: " + results.toString());
		if (results.size() > 0) {
			String msgString = results.get(0);
			new SendCommandAsyncTask(getActivity(), msgString).execute();
			ChatFragment.this.addCmdHistory(msgString);
		}else {
			showTip(getString(R.string.speech_no_result));
		}
	}

	@Override
	public void onDissmiss(int state) {
		
	}
}