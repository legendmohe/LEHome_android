package my.home.lehome.activity;

import my.home.lehome.fragment.SettingsFragment;
import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    @Override
    protected void onResume() {
      super.onResume();
    }

    @Override
    protected void onPause() {
      super.onPause();
    }
}
