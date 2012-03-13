package ru.securelayer.rad.ando;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import android.content.res.Configuration;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import android.support.v4.view.ViewPager;

import com.kaloer.filepicker.FilePickerActivity;
import org.fedorahosted.tennera.jgettext.Catalog;
import org.fedorahosted.tennera.jgettext.Message;
import org.fedorahosted.tennera.jgettext.catalog.parse.ExtendedCatalogParser;
import org.fedorahosted.tennera.jgettext.catalog.parse.ParseException;
import org.fedorahosted.tennera.jgettext.PoWriter;
import antlr.RecognitionException;
import antlr.TokenStreamException;

import ru.securelayer.rad.ando.R;
import ru.securelayer.rad.ando.MessageAdapter;

public class AndoMain extends Activity
{
    private static final String PREFERENCE_FILE = "AndoGettextResourceEditor";
    private static final String RESOURCE_FILENAME_KEY = "RESOURCE_FILENAME";
    private static final String RESOURCE_POSITION_KEY = "RESOURCE_POSITION";

    private static final int REQUEST_PICK_FILE = 1;

    private String resourceFileName = null;
    private TextView textFileName = null;
    private Catalog catalog = null;
    private ArrayList<Message> messages = null;
    private Context ctx = null;
    private MessageAdapter pagerAdapter = null;
    private ViewPager messagePager = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);

        setContentView(R.layout.main);

        messages = new ArrayList<Message>();

        ctx = this;

        pagerAdapter = new MessageAdapter(ctx, messages);
        messagePager = (ViewPager) findViewById(R.id.view_pager);
        messagePager.setAdapter(pagerAdapter);

        textFileName = (TextView) findViewById(R.id.textFileName);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
    }

    /**
     * Loads last state of UI and variables.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (readInstanceState(this)) {
            showNotification(getString(R.string.activity_resumed));
        }
    }

    /**
     * Saves current state of UI and variables.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (this.resourceFileName == null) {
            if (! writeInstanceState(this)) {
                showNotification(getString(R.string.activity_saved_not));
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape orientation
        } else {
            // portrait orientation
        }
        if (newConfig.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO) {
            // keyboard is active
        } else {
            // keyboard is hidden
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_open:
                chooseAndOpen();
                return true;
            case R.id.menu_save:
                saveCatalog(resourceFileName);
                return true;
            case R.id.menu_copy:
                msgstrCopy();
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch(requestCode) {
            case REQUEST_PICK_FILE:
                if (data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH)) {
                    resourceFileName = data.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH);
                    loadCatalog(resourceFileName);
                }
            }
        }
    }

    protected void chooseAndOpen() {
        // create an intent for the file picker activity
        Intent intent = new Intent(this, FilePickerActivity.class);
        // set the initial directory to be the sdcard
        intent.putExtra(FilePickerActivity.EXTRA_FILE_PATH, Environment.getExternalStorageDirectory().getAbsolutePath());
        // only make .po files visible
        ArrayList<String> extensions = new ArrayList<String>();
        extensions.add(".po");
        intent.putExtra(FilePickerActivity.EXTRA_ACCEPTED_FILE_EXTENSIONS, extensions);
        // start the activity
        startActivityForResult(intent, REQUEST_PICK_FILE);
    }

    public boolean readInstanceState(Context c) {
        SharedPreferences pref = c.getSharedPreferences(AndoMain.PREFERENCE_FILE, MODE_WORLD_READABLE);
        if (pref.contains(RESOURCE_FILENAME_KEY) && pref.contains(RESOURCE_POSITION_KEY)) {
            String filename = pref.getString(RESOURCE_FILENAME_KEY, "");
            if (! filename.equals("")) {
                this.loadCatalog(filename);
                int position = pref.getInt(RESOURCE_POSITION_KEY, 0);
                messagePager.setCurrentItem(position, true);
            }
            return true;
        }
        return false;
    }

    public boolean writeInstanceState(Context c) {
        SharedPreferences pref = c.getSharedPreferences(AndoMain.PREFERENCE_FILE, MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = pref.edit();
        int position = this.messagePager.getCurrentItem();
        editor.putString(RESOURCE_FILENAME_KEY, resourceFileName);
        editor.putInt(RESOURCE_POSITION_KEY, position);
        return (editor.commit());
    }

    /**
     * Loads the messages catalog.
     *
     * @param fileName The full path to resource file on filesystem.
     */
    protected void loadCatalog(String fileName) {
        try {
            try {
                File poFile = new File(fileName);
                // Parse a file
                ExtendedCatalogParser parser = new ExtendedCatalogParser(poFile);
                try {
                    parser.catalog();
                } catch (RecognitionException ex) {
                } catch (TokenStreamException ex) {
                }
                catalog = parser.getCatalog();
                // Iterate of file's items.
                messages.clear();
                for (Message m : catalog){
                    if (! m.isHeader()) {
                        messages.add(m);
                    }
                }
                // Set the file path text view
                textFileName.setText(fileName);
                // Show first page
                messagePager.setCurrentItem(0, true);
                // Show notification
                showNotification(getString(R.string.resource_loaded));
            } catch(FileNotFoundException ex) {}
        } catch(IOException ex) {}
    }

    /**
     * Saves the messages catalog.
     *
     * @param fileName The full path to resource file on filesystem.
     */
    protected void saveCatalog(String fileName) {
        View page = this.messagePager.getFocusedChild();
        int position = this.messagePager.getCurrentItem();
        this.pagerAdapter.applyIfChanged(page, position);

        PoWriter writer = new PoWriter();
        CharSequence msg;
        try {
            File poFile = new File(fileName);
            writer.write(catalog, poFile);
            msg = getString(R.string.resource_saved);
        } catch(IOException ex) {
            msg = getString(R.string.resource_saved_not);
        }
        showNotification(msg);
    }

    /**
     * Copies the content from original widget into translated one.
     */
    protected void msgstrCopy() {
        View page = this.messagePager.getFocusedChild();
        TextView wOrig = (TextView) page.findViewById(R.id.textOriginal);
        EditText wEdit = (EditText) page.findViewById(R.id.editTranslated);
        wEdit.setText(wOrig.getText());
        pagerAdapter.notifyDataSetChanged();
    }

    /**
     * Shows the visible notification on device's screen.
     *
     * @param msg The message to be shown.
     */
    protected void showNotification(CharSequence msg) {
        Context ctx = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(ctx, msg, duration);
        toast.show();
    }
}
