package my.home.lehome.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.greenrobot.lehome.ChatItem;
import my.home.lehome.R;
import my.home.lehome.R.drawable;
import my.home.lehome.R.id;
import my.home.lehome.R.layout;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ChatItemArrayAdapter extends ArrayAdapter<ChatItem> {

	private TextView chatTextView;
	private RelativeLayout wrapper;

	@Override
	public void add(ChatItem object) {
		super.add(object);
	}
	
	public void setData(List<ChatItem> items) {
	    clear();
	    setNotifyOnChange(false);
	    if (items != null) {
	        for (ChatItem item : items)
	            add(item);
	    }
	    notifyDataSetChanged();
	}

	public ChatItemArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.chat_item, parent, false);
		}

		wrapper = (RelativeLayout) row.findViewById(R.id.wrapper);
		ChatItem chatItem = getItem(position);
		chatTextView = (TextView) row.findViewById(R.id.chat_item);
		chatTextView.setText(chatItem.getContent());
		chatTextView.setBackgroundResource(!chatItem.getIsMe() ? R.drawable.chatfrom_bg : R.drawable.chatto_bg);
		wrapper.setGravity(!chatItem.getIsMe() ? Gravity.LEFT : Gravity.RIGHT);

		return row;
	}
}