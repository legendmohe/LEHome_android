package my.home.lehome.fragment;

import my.home.lehome.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference("pref_auto_add_begin_and_end");
        EditTextPreference beginEditTextPreference = (EditTextPreference) findPreference("pref_message_begin");
        EditTextPreference endEditTextPreference = (EditTextPreference) findPreference("pref_message_end");
        boolean is_auto = sharedPreferences.getBoolean("pref_auto_add_begin_and_end", false);
        if(is_auto) {
        	checkBoxPreference.setChecked(true);
        	beginEditTextPreference.setEnabled(true);
        	endEditTextPreference.setEnabled(true);
    	} else {
    		checkBoxPreference.setChecked(false);
    		beginEditTextPreference.setEnabled(false);
    		endEditTextPreference.setEnabled(false);
    	}
        beginEditTextPreference.setSummary(sharedPreferences.getString("pref_message_begin", ""));
        endEditTextPreference.setSummary(sharedPreferences.getString("pref_message_end", ""));
        
        EditTextPreference subEditTextPreference = (EditTextPreference) findPreference("pref_sub_address");
        EditTextPreference pubEditTextPreference = (EditTextPreference) findPreference("pref_pub_address");
        subEditTextPreference.setSummary(sharedPreferences.getString("pref_sub_address", ""));
        pubEditTextPreference.setSummary(sharedPreferences.getString("pref_pub_address", ""));
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

	}

	@Override
	public void onPause() {
	    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	    super.onPause();
	}
	
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals("pref_message_begin") || key.equals("pref_message_end"))
        {
            Preference exercisesPref = findPreference(key);
            exercisesPref.setSummary(sharedPreferences.getString(key, ""));
        }else if (key.equals("pref_pub_address") || key.equals("pref_sub_address"))
        {
            Preference exercisesPref = findPreference(key);
            exercisesPref.setSummary(sharedPreferences.getString(key, ""));
        }else if(key.equals("pref_auto_add_begin_and_end")) {
        	if(sharedPreferences.getBoolean("pref_auto_add_begin_and_end", false)) {
        		findPreference("pref_message_begin").setEnabled(true);
        		findPreference("pref_message_end").setEnabled(true);
        	} else {
        		findPreference("pref_message_begin").setEnabled(false);
        		findPreference("pref_message_end").setEnabled(false);
        	}
        }
    }
}
