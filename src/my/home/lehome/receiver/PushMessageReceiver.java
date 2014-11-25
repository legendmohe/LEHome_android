package my.home.lehome.receiver;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import my.home.lehome.R;
import my.home.lehome.helper.MessageHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.igexin.sdk.PushConsts;
import com.igexin.sdk.PushManager;

public class PushMessageReceiver extends BroadcastReceiver {
	
	public static final String TAG = PushMessageReceiver.class
            .getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		Log.d(TAG, "onReceive() action=" + bundle.getInt("action"));
		switch (bundle.getInt(PushConsts.CMD_ACTION)) {

		case PushConsts.GET_MSG_DATA:
			// 获取透传数据
			// String appid = bundle.getString("appid");
			byte[] payload = bundle.getByteArray("payload");
			
			String taskid = bundle.getString("taskid");
			String messageid = bundle.getString("messageid");

			// smartPush第三方回执调用接口，actionid范围为90000-90999，可根据业务场景执行
			boolean result = PushManager.getInstance().sendFeedbackMessage(context, taskid, messageid, 90001);
			System.out.println("第三方回执接口调用" + (result ? "成功" : "失败"));
			
			if (payload != null) {
				String message = new String(payload);
				Log.d(TAG, "Got Payload:" + message);
				
				JSONTokener jsonParser = new JSONTokener(message);
				String type = "";
				String msg = "";
				String err_msg = "";
				int seq = -1;
				try {
					JSONObject cmdObject = (JSONObject) jsonParser.nextValue();
					type = cmdObject.getString("type");
					msg = cmdObject.getString("msg");
					seq = cmdObject.getInt("seq");
//					int maxseq = cmdObject.getInt("maxseq");
				} catch (JSONException e) {
					e.printStackTrace();
					err_msg = context.getString(R.string.msg_push_msg_format_error);
				} catch (Exception e) {
					e.printStackTrace();
					err_msg = context.getString(R.string.msg_push_msg_format_error);
				}
				
				if(!TextUtils.isEmpty(err_msg)){
					MessageHelper.sendToast(err_msg);
					return;
				}
				
		    	if (type.equals("normal")) {
		    		MessageHelper.inNormalState = true;
				}else if (type.equals("toast")) {
					MessageHelper.sendToast(msg);
					return;
				}else {
					MessageHelper.inNormalState = false;
				}
				MessageHelper.sendServerMsgToList(seq, msg, context);
			}
			break;
		case PushConsts.GET_CLIENTID:
			// 获取ClientID(CID)
			// 第三方应用需要将CID上传到第三方服务器，并且将当前用户帐号和CID进行关联，以便日后通过用户帐号查找CID进行消息推送
			String cid = bundle.getString("clientid");
			Log.d(TAG, "get clientid: " + cid);
			MessageHelper.sendToast(context.getString(R.string.msg_push_binded));
			break;
		case PushConsts.THIRDPART_FEEDBACK:
			/*String appid = bundle.getString("appid");
			String taskid = bundle.getString("taskid");
			String actionid = bundle.getString("actionid");
			String result = bundle.getString("result");
			long timestamp = bundle.getLong("timestamp");

			Log.d("GetuiSdkDemo", "appid = " + appid);
			Log.d("GetuiSdkDemo", "taskid = " + taskid);
			Log.d("GetuiSdkDemo", "actionid = " + actionid);
			Log.d("GetuiSdkDemo", "result = " + result);
			Log.d("GetuiSdkDemo", "timestamp = " + timestamp);*/
			break;
		default:
			break;
		}
	}
}
