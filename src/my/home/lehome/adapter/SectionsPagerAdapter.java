package my.home.lehome.adapter;

import java.util.Locale;

import my.home.lehome.R;
import my.home.lehome.fragment.ChatFragment;
import my.home.lehome.fragment.ShortcutFragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SectionsPagerAdapter extends FragmentPagerAdapter {
	
	private Context context;
	private ChatFragment chatFragment;
    private ShortcutFragment shortcurFragment;
	
	public SectionsPagerAdapter(FragmentManager fragmentManager, Context context) {
		super(fragmentManager);
		this.context = context;
	}

	@Override
	public Fragment getItem(int position) {
		Fragment fragment = null;
		switch (position) {
		case 0:
	    	if(chatFragment == null) {
	    		chatFragment = new ChatFragment();
	    	}
	    	fragment = chatFragment;
			break;
		case 1:
	    	if(shortcurFragment == null) {
	    		shortcurFragment = new ShortcutFragment();
	    	}
	    	fragment = shortcurFragment;
			break;
		default:
			break;
		}
		return fragment;
	}

	@Override
	public int getCount() {
		return 2;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		Locale l = Locale.getDefault();
		switch (position) {
		case 0:
			return context.getString(R.string.title_section1).toUpperCase(l);
		case 1:
			return context.getString(R.string.title_section2).toUpperCase(l);
		}
		return null;
	}
}
