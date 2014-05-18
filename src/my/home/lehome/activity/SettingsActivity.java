package my.home.lehome.activity;

import my.home.lehome.fragment.SettingsFragment;
import my.home.lehome.service.ConnectionService;
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
      ConnectionService.activityResumed();
    }

    @Override
    protected void onPause() {
      super.onPause();
      ConnectionService.activityPaused();
    }
}
