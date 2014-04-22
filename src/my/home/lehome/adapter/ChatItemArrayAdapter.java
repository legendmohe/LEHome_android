package my.home.lehome.adapter;

import java.util.ArrayList;
import java.util.List;

import my.home.lehome.R;
import my.home.lehome.R.drawable;
import my.home.lehome.R.id;
import my.home.lehome.R.layout;
import my.home.lehome.model.ChatItem;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChatItemArrayAdapter extends ArrayAdapter<ChatItem> {

	private TextView chatTextView;
	private List<ChatItem> cmds = new ArrayList<ChatItem>();
	private LinearLayout wrapper;

	@Override
	public void add(ChatItem object) {
		cmds.add(object);
		super.add(object);
	}

	public ChatItemArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}

	public int getCount() {
		return this.cmds.size();
	}

	public ChatItem getItem(int index) {
		return this.cmds.get(index);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.chat_item, parent, false);
		}

		wrapper = (LinearLayout) row.findViewById(R.id.wrapper);
		ChatItem chatItem = getItem(position);
		chatTextView = (TextView) row.findViewById(R.id.chat_item);
		chatTextView.setText(chatItem.content);
		chatTextView.setBackgroundResource(!chatItem.isMe ? R.drawable.chatfrom_bg : R.drawable.chatto_bg);
		wrapper.setGravity(!chatItem.isMe ? Gravity.LEFT : Gravity.RIGHT);

		return row;
	}

	public Bitmap decodeToBitmap(byte[] decodedByte) {
		return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
	}

}