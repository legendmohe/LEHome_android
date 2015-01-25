package my.home.lehome.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.voicerecognition.android.VoiceRecognitionClient;
import com.baidu.voicerecognition.android.VoiceRecognitionConfig;
import com.baidu.voicerecognition.android.VoiceRecognitionClient.VoiceClientStatusChangeListener;
import com.baidu.voicerecognition.android.ui.DialogRecognitionListener;

import my.home.lehome.R;
import my.home.lehome.util.Constants;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SpeechDialog extends DialogFragment {
	
	public static final String TAG = "SpeechDialog";
	
	public class State {
		public static final int IDLE = 0;
		public static final int LISTENING = 1;
		public static final int PROCESSING = 2;
		public static final int FINISHED = 3;
		public static final int CANCELED = 4;
		public static final int ERROR = 5;
	}
	
	public class Msg {
		public static final int START = 0;
		public static final int END = 1;
		public static final int FINISH = 2;
		public static final int HIDE = 3;
		public static final int CANCEL = 4;
		public static final int ERROR = 5;
	}
	
	private static int CUR_STATE = State.IDLE;
	private static boolean AUTO_START = true;
	private static boolean AUTO_HIDE = true;
	
	private final Handler mHandler = new MyHandler(this);
	private Handler mMainThreadHandler;
	
	private VoiceRecognitionClient mASREngine;
	private SpeechDialogResultListener mResultListener;
	private MyVoiceRecogListener mVoiceRecogListener = new MyVoiceRecogListener();
	
	private TextView mStatusTextView;
	private TextView mReleaseTextView;
	private ProgressBar mVolumnProgressBar;
	
//	private List<String> mResult = null;
	
    private static final int POWER_UPDATE_INTERVAL = 50;
    private static final int SHORTEST_SHOWN_TIME = 500;
    private static final int DIALOG_HINT_DELAY = 1000;
    
    /**
     * 音量更新任务
     */
    private Runnable mUpdateVolume = new Runnable() {
        public void run() {
            if (CUR_STATE == State.LISTENING) {
                long vol = mASREngine.getCurrentDBLevelMeter();
//                Log.i(TAG, String.valueOf(vol));
                mVolumnProgressBar.setProgress((int) vol);
                // set vol
                mMainThreadHandler.removeCallbacks(mUpdateVolume);
                mMainThreadHandler.postDelayed(mUpdateVolume, POWER_UPDATE_INTERVAL);
            }
        }
    };
    
	private static class MyHandler extends Handler {
		private final WeakReference<SpeechDialog> mSpeechDialogReference;
		
		public MyHandler(SpeechDialog dialog) {
			mSpeechDialogReference = new WeakReference<SpeechDialog>(dialog);
		}
		
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Log.d(TAG, msg.toString());
			
			SpeechDialog dialog = mSpeechDialogReference.get();
			switch (msg.what) {
			case Msg.START:
				dialog.processStartMsg(msg);
				break;
			case Msg.END:
				dialog.processEndMsg(msg);
				break;
			case Msg.CANCEL:
				dialog.processCancelMsg(msg);
				break;
			case Msg.FINISH:
				dialog.processFinishMsg(msg);
				break;
			case Msg.HIDE:
				dialog.processHideMsg(msg);
				break;
			case Msg.ERROR:
				dialog.processErrorMsg(msg);
				break;
			default:
				break;
			}
		}
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    };
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onStart() {
		CUR_STATE = State.IDLE;
		if (AUTO_START) {
			sendMsg(Msg.START, SHORTEST_SHOWN_TIME);
		}
		super.onStart();
	}
	
	@SuppressLint("InflateParams") @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    View contentView = inflater.inflate(R.layout.speech_dialog, null);
	    
	    mStatusTextView = (TextView) contentView.findViewById(R.id.status_textview);
	    mVolumnProgressBar = (ProgressBar) contentView.findViewById(R.id.volumn_progressbar);
	    mReleaseTextView = (TextView) contentView.findViewById(R.id.speech_release_cancel_textview);
	    
	    builder.setView(contentView);      
	    return builder.create();
	}
	
	@Override
	public void onDestroyView() {
		VoiceRecognitionClient.releaseInstance(); // 释放识别库
		super.onDestroyView();
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		sendMsg(Msg.CANCEL);
		super.onCancel(dialog);
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		if (mResultListener != null) {
			mResultListener.onDissmiss(CUR_STATE);
		}
		super.onDismiss(dialog);
	}
	
	/*
	 * User must call setup() before showing the Speech Dialog.
	 */
	public void setup(Context contect, SpeechDialogResultListener listener) {
		mASREngine = VoiceRecognitionClient.getInstance(contect);
        mASREngine.setTokenApis(Constants.BAIDUVOICE_API_KEY, Constants.BAIDUVOICE_SECRET_KEY);
        mMainThreadHandler = new Handler();
        mResultListener = listener;
	}
	
	public void setHintText(String content) {
		mStatusTextView.setText(content);
	}
	
	private void sendMsg(int what) {
		mHandler.obtainMessage(what).sendToTarget();
	}
	
	private void sendMsg(int what, Object obj) {
		mHandler.obtainMessage(what, obj).sendToTarget();
	}
	
	private void sendMsg(int what, long delay) {
		Message m = mHandler.obtainMessage(what);
		mHandler.sendMessageDelayed(m, delay);
	}
	
	private void sendMsg(int what, int arg1, int arg2) {
		Message m = mHandler.obtainMessage(what);
		m.arg1 = arg1;
		m.arg2 = arg2;
		m.sendToTarget();
	}
	
	/*
	 * Start Speech Message
	 */
	protected void processStartMsg(Message msg) {
		switch (CUR_STATE) {
		case State.LISTENING:
			break;
		case State.PROCESSING:
			break;
		case State.CANCELED:
			break;
		case State.FINISHED:
			break;
		case State.ERROR:
			break;
		case State.IDLE:
//            mResult = null;
			mStatusTextView.setText(R.string.speech_speaking);
			CUR_STATE = State.LISTENING;
			if(!startListening()) {
				sendMsg(Msg.ERROR);
			}
			break;
		default:
			break;
		}
	}
	
	/*
	 * Stop Speech, Start Recognize.
	 */
	protected void processEndMsg(Message msg) {
		switch (CUR_STATE) {
		case State.LISTENING:
			CUR_STATE = State.PROCESSING;
			mVolumnProgressBar.setIndeterminate(true);
			mStatusTextView.setText(R.string.speech_processing);
			break;
		case State.PROCESSING:
			break;
		case State.FINISHED:
			break;
		case State.ERROR:
			break;
		case State.IDLE:
			break;
		case State.CANCELED:
			break;
		default:
			break;
		}
	}
	
	protected void processCancelMsg(Message msg) {
		switch (CUR_STATE) {
		case State.CANCELED:
			break;
		case State.LISTENING:
		case State.PROCESSING:
			CUR_STATE = State.CANCELED;
			mVolumnProgressBar.setIndeterminate(false);
			mASREngine.stopVoiceRecognition();
			if (AUTO_HIDE) {
				sendMsg(Msg.HIDE);
			}
			break;
		case State.FINISHED:
			break;
		case State.ERROR:
			break;
		case State.IDLE:
			mStatusTextView.setText(R.string.speech_too_short);
			mVolumnProgressBar.setIndeterminate(false);
			CUR_STATE = State.CANCELED;
			if (AUTO_HIDE) {
				sendMsg(Msg.HIDE, DIALOG_HINT_DELAY);
			}
			break;

		default:
			break;
		}
	}
	
	protected void processFinishMsg(Message msg) {
		switch (CUR_STATE) {
		case State.CANCELED:
			break;
		case State.LISTENING:
			break;
		case State.PROCESSING:
			CUR_STATE = State.FINISHED;
			mVolumnProgressBar.setIndeterminate(false);
			List<String> result = (List<String>) msg.obj;
			parseResult(result);
			if (AUTO_HIDE) {
				sendMsg(Msg.HIDE);
			}
			break;
		case State.FINISHED:
			break;
		case State.ERROR:
			break;
		case State.IDLE:
			break;

		default:
			break;
		}
	}
	
	protected void processErrorMsg(Message msg) {
		switch (CUR_STATE) {
		case State.CANCELED:
			break;
		case State.LISTENING:
		case State.PROCESSING:
			mVolumnProgressBar.setIndeterminate(false);
			int errType = msg.arg1;
			int errCode = msg.arg2;
			mStatusTextView.setText(errorCodeToString(errType, errCode));
			CUR_STATE = State.ERROR;
			if (AUTO_HIDE) {
				sendMsg(Msg.HIDE, DIALOG_HINT_DELAY);
			}
			break;
		case State.FINISHED:
			break;
		case State.ERROR:
			break;
		case State.IDLE:
			break;

		default:
			break;
		}
	}
	
	protected void processHideMsg(Message msg) {
		switch (CUR_STATE) {
		case State.CANCELED:
		case State.FINISHED:
		case State.ERROR:
//			CUR_STATE = State.IDLE;
			this.dismiss();
			break;
		case State.LISTENING:
			break;
		case State.PROCESSING:
			break;
		case State.IDLE:
			break;

		default:
			break;
		}
	}
	
	public int currentState() {
		return CUR_STATE;
	}
	
	public void setReleaseCancelVisible(boolean visible) {
		if (CUR_STATE == State.LISTENING) {
			if (visible) {
				mReleaseTextView.setVisibility(View.VISIBLE);
				mStatusTextView.setVisibility(View.INVISIBLE);
				mVolumnProgressBar.setVisibility(View.INVISIBLE);
			}else {
				mReleaseTextView.setVisibility(View.INVISIBLE);
				mStatusTextView.setVisibility(View.VISIBLE);
				mVolumnProgressBar.setVisibility(View.VISIBLE);
			}
		}
	}

	
    /**
     * 重写用于处理语音识别回调的监听器
     */
    class MyVoiceRecogListener implements VoiceClientStatusChangeListener {

        @Override
        public void onClientStatusChange(int status, Object obj) {
            switch (status) {
            // 语音识别实际开始，这是真正开始识别的时间点，需在界面提示用户说话。
                case VoiceRecognitionClient.CLIENT_STATUS_START_RECORDING:
                	mStatusTextView.setText(R.string.speech_speaking);
                	mMainThreadHandler.removeCallbacks(mUpdateVolume);
                	mMainThreadHandler.postDelayed(mUpdateVolume, POWER_UPDATE_INTERVAL);
                    break;
                case VoiceRecognitionClient.CLIENT_STATUS_SPEECH_START: // 检测到语音起点
                    break;
                // 已经检测到语音终点，等待网络返回
                case VoiceRecognitionClient.CLIENT_STATUS_SPEECH_END:
                	sendMsg(Msg.END);
                    break;
                // 语音识别完成，显示obj中的结果
                case VoiceRecognitionClient.CLIENT_STATUS_FINISH:
                    if (obj != null && obj instanceof List) {
                        List results = (List) obj;
                        sendMsg(Msg.FINISH, results);
                    }
                    break;
                // 用户取消
                case VoiceRecognitionClient.CLIENT_STATUS_USER_CANCELED:
                	sendMsg(Msg.CANCEL);
                    break;
                default:
                    break;
            }

        }

        @Override
        public void onError(int errorType, int errorCode) {
        	sendMsg(Msg.ERROR, errorType, errorCode);
        }

        @Override
        public void onNetworkStatusChange(int status, Object obj) {
        }
    }
    
    public boolean finishListening() {
    	if (CUR_STATE == State.IDLE) {
			sendMsg(Msg.CANCEL);
		}else if (CUR_STATE == State.LISTENING) {
    		mASREngine.speakFinish();
		}
        return true;
    }

    public boolean startListening() {
        VoiceRecognitionConfig config = new VoiceRecognitionConfig();
        config.setProp(VoiceRecognitionConfig.PROP_SEARCH);
        config.setLanguage(VoiceRecognitionConfig.LANGUAGE_CHINESE);
//        config.enableNLU();
        config.setUseBlueTooth(true);
        config.enableVoicePower(true); // 音量反馈。
        config.enableBeginSoundEffect(R.raw.bdspeech_recognition_start); // 设置识别开始提示音
        config.enableEndSoundEffect(R.raw.bdspeech_speech_end); // 设置识别结束提示音
//        config.setSampleRate(VoiceRecognitionConfig.SAMPLE_RATE_8K); // 设置采样率
        // 下面发起识别
        int code = mASREngine.startVoiceRecognition(mVoiceRecogListener, config);
        if (code != VoiceRecognitionClient.START_WORK_RESULT_WORKING) {
            Toast.makeText(this.getActivity(), getString(R.string.speech_start_faild, code),
                    Toast.LENGTH_LONG).show();
        }

        return code == VoiceRecognitionClient.START_WORK_RESULT_WORKING;
    }

    public boolean cancelListening() {
        sendMsg(Msg.CANCEL);
        return true;
    }
    
    private void parseResult(List<String> results) {
        if (mResultListener != null) {
			mResultListener.onResult(results);
		}
    }
    
	
	public interface SpeechDialogResultListener {
		void onResult(List<String> results);
		void onDissmiss(int state);
	}
	
	/*
	 * Error code to String
	 */
	public String errorCodeToString(int type, int code) {
		switch (type) {
		case VoiceRecognitionClient.ERROR_CLIENT:
			switch (code) {
			case VoiceRecognitionClient.ERROR_CLIENT_JNI_EXCEPTION:
				return "ERROR_CLIENT_JNI_EXCEPTION";
			case VoiceRecognitionClient.ERROR_CLIENT_NO_SPEECH:
				return "ERROR_CLIENT_NO_SPEECH";
			case VoiceRecognitionClient.ERROR_CLIENT_TOO_SHORT:
				return "ERROR_CLIENT_TOO_SHORT";
			case VoiceRecognitionClient.ERROR_CLIENT_UNKNOWN:
				return "ERROR_CLIENT_UNKNOWN";
			case VoiceRecognitionClient.ERROR_CLIENT_WHOLE_PROCESS_TIMEOUT:
				return "ERROR_CLIENT_WHOLE_PROCESS_TIMEOUT";
			default:
				break;
			}
			break;
		case VoiceRecognitionClient.ERROR_NETWORK:
			switch (code) {
			case VoiceRecognitionClient.ERROR_NETWORK_UNUSABLE:
				return "ERROR_NETWORK_UNUSABLE";
			case VoiceRecognitionClient.ERROR_NETWORK_CONNECT_ERROR:
				return "ERROR_NETWORK_CONNECT_ERROR";
			case VoiceRecognitionClient.ERROR_NETWORK_PARSE_ERROR:
				return "ERROR_NETWORK_PARSE_ERROR";
			default:
				break;
			}
			break;
		case VoiceRecognitionClient.ERROR_RECORDER:
			switch (code) {
			case VoiceRecognitionClient.ERROR_RECORDER_UNAVAILABLE:
				return "ERROR_RECORDER_UNAVAILABLE";
			case VoiceRecognitionClient.ERROR_RECORDER_INTERCEPTED:
				return "ERROR_RECORDER_INTERCEPTED";
			default:
				break;
			}
			break;
		case VoiceRecognitionClient.ERROR_SERVER:
			switch (code) {
			case VoiceRecognitionClient.ERROR_SERVER_BACKEND_ERROR:
				return "ERROR_SERVER_BACKEND_ERROR";
			case VoiceRecognitionClient.ERROR_SERVER_PARAMETER_ERROR:
				return "ERROR_CLIENT_JNI_EXCEPTION";
			case VoiceRecognitionClient.ERROR_SERVER_RECOGNITION_ERROR:
				return "ERROR_SERVER_RECOGNITION_ERROR";
			case VoiceRecognitionClient.ERROR_SERVER_INVALID_APP_NAME:
				return "ERROR_SERVER_INVALID_APP_NAME";
			case VoiceRecognitionClient.ERROR_SERVER_SPEECH_QUALITY_ERROR:
				return "ERROR_SERVER_SPEECH_QUALITY_ERROR";
			default:
				break;
			}
			break;
		default:
			break;
		}
		return "UNKNOWN ERROR TYPE OR CODE";
	}
}
