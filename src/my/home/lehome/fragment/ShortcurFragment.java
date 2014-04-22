package my.home.lehome.fragment;

import my.home.lehome.R;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.asynctask.SendCommandAsyncTask;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ShortcurFragment extends ListFragment {
	
	private ArrayAdapter<String> adapter;
	
    @Override
    public View onCreateView(LayoutInflater inflater,      
    		ViewGroup container, Bundle savedInstanceState) {             
        View rootView =  inflater.inflate(R.layout.shortcut_fragment, container, false); 
        return rootView;
    }
          
    @Override
    public void onCreate(Bundle savedInstanceState) {     
        super.onCreate(savedInstanceState);
        if (adapter == null) {
        	adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
		}
        setListAdapter(adapter);
    }     
    
    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onListItemClick(ListView parent, View v,      
    int position, long id)      
    {
    	String value = adapter.getItem(position);
        Toast.makeText(getActivity(),      
            "You have selected " + value,      
            Toast.LENGTH_SHORT).show();
        MainActivity mainActivity = (MainActivity) getActivity();
		new SendCommandAsyncTask(mainActivity).execute(value);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	int id = item.getItemId();
        switch (id) {
		case R.id.add_shortcut_item:
			addListItemWithUserInput();
        	return true;
		default:
			break;
		}
    	return super.onOptionsItemSelected(item);
    }
    
    private void addListItemWithUserInput() {
    	AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

//    	alert.setTitle(getResources().getString(R.string.add_shortcut_item));
    	alert.setMessage(getResources().getString(R.string.add_shortcut_item_summ));

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(getActivity());
    	input.setSingleLine(true);
    	input.setHint(R.string.qs_default_input);
    	alert.setView(input);

    	alert.setPositiveButton(getResources().getString(R.string.com_comfirm)
    							, new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int whichButton) {
	    		String value = input.getText().toString();
	    	  	if (value != null && !value.trim().equals("")) {
					adapter.add(value);
				}
	    	}
    	});

    	alert.setNegativeButton(getResources().getString(R.string.com_cancel), 
    							new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			// Canceled.
    		}
    	});

    	alert.show();
    	// see http://androidsnippets.com/prompt-user-input-with-an-alertdialog
	}
}
