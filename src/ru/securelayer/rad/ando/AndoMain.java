package ru.securelayer.rad.ando;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import android.content.res.Configuration;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.gesture.GestureLibraries;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;

import com.kaloer.filepicker.FilePickerActivity;
import org.fedorahosted.tennera.jgettext.Catalog;
import org.fedorahosted.tennera.jgettext.Message;
import org.fedorahosted.tennera.jgettext.catalog.parse.ExtendedCatalogParser;
import org.fedorahosted.tennera.jgettext.catalog.parse.ParseException;
import org.fedorahosted.tennera.jgettext.PoWriter;
import antlr.RecognitionException;
import antlr.TokenStreamException;

import ru.securelayer.rad.ando.R;

public class AndoMain extends Activity 
    implements OnGesturePerformedListener, TextWatcher
{
    private static final String PREFERENCE_FILE = "AndoGettextResourceEditor";
    private static final String RESOURCE_FILENAME_KEY = "RESOURCE_FILENAME";
    private static final String RESOURCE_POSITION_KEY = "RESOURCE_POSITION";

    private static final int REQUEST_PICK_FILE = 1;

    private TextView widgetMsgId = null;
    private EditText widgetMsgStr = null;

    private String resourceFileName = null;
    private Catalog catalog = null;
    private ArrayList<Message> messages = null;
    private Message token = null;
    private ListIterator<Message> iterator = null;

    private Boolean directionForward = true;
    private Boolean procTextChanged = false;

    private GestureLibrary mLibrary;
    private GestureOverlayView gestures;

    private int msgTotal = 0;
    private int msgTrans = 0;
    private int msgFuzzy = 0;
    private int msgTrash = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        setContentView(R.layout.main);

        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);

        widgetMsgId = (TextView) findViewById(R.id.wMsgId);
        widgetMsgStr = (EditText) findViewById(R.id.wMsgStr);
        widgetMsgStr.addTextChangedListener(this);

        this.messages = new ArrayList<Message>();

        this.mLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
        if (!this.mLibrary.load()) {
            finish();
        }
        this.gestures = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        this.gestures.addOnGesturePerformedListener(this);

        String title = (String) getTitle() + ": " + getString(R.string.resource_choose);
        setTitle(title);
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
        // if (this.resourceFileName == null && this.messagePager != null) {
        //     if (! writeInstanceState(this)) {
        //         showNotification(getString(R.string.activity_saved_not));
        //     }
        // }
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
                // messagePager.setCurrentItem(position, true);
            }
            return true;
        }
        return false;
    }

    public boolean writeInstanceState(Context c) {
        SharedPreferences pref = c.getSharedPreferences(AndoMain.PREFERENCE_FILE, MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = pref.edit();
        // int position = this.messagePager.getCurrentItem();
        // editor.putString(RESOURCE_FILENAME_KEY, resourceFileName);
        // editor.putInt(RESOURCE_POSITION_KEY, position);
        return (editor.commit());
    }

    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        ArrayList predictions = this.mLibrary.recognize(gesture);

        // We want at least one prediction
        if (predictions.size() > 0) {
            Prediction prediction = (Prediction) predictions.get(0);
            if (prediction.score > 1.0) {
                String action = prediction.name;
                if ("next".equals(action)) {
                    this.showNotification(getString(R.string.next_token));
                    this.nextMessage();
                } else if ("prev".equals(action)) {
                    this.showNotification(getString(R.string.prev_token));
                    this.prevMessage();
                } else if ("next_fuzzy".equals(action)) {
                    this.showNotification(getString(R.string.next_token_fuzzy));
                    this.nextMessage();
                } else if ("prev_fuzzy".equals(action)) {
                    this.showNotification(getString(R.string.prev_token_fuzzy));
                    this.prevMessage();
                } else if ("next_untranslated".equals(action)) {
                    this.showNotification(getString(R.string.next_token_untranslated));
                    this.nextMessage();
                } else if ("prev_untranslated".equals(action)) {
                    this.showNotification(getString(R.string.prev_token_untranslated));
                    this.prevMessage();
                }
            }
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    public void onTextChanged(CharSequence s, int start, int before, int count) {}
    public void afterTextChanged(Editable s) {
        if (! this.procTextChanged) {
            this.procTextChanged = true;
            this.token.setMsgstr(s.toString());
            this.procTextChanged = false;
        }
    }

    /**
     * Loads the messages catalog.
     *
     * @param fileName The full path to resource file on filesystem.
     */
    protected void loadCatalog(String fileName) {
        this.catalog = null;
        this.messages.clear();
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
                this.catalog = parser.getCatalog();
                // Iterate of file's items.
                for (Message m : this.catalog){
                    if (! m.isHeader()) {
                        this.messages.add(m);
                    }
                }
                iterator = this.messages.listIterator();
                // Show notification
                showNotification(getString(R.string.resource_loaded));
                // Show first page
                this.nextMessage();
            } catch(FileNotFoundException ex) {}
        } catch(IOException ex) {}
    }

    /**
     * Saves the messages catalog.
     *
     * @param fileName The full path to resource file on filesystem.
     */
    protected void saveCatalog(String fileName) {
        CharSequence msg;
        if (0 == this.messages.size()) {
            msg = getString(R.string.resource_ready_not);
        } else {
            PoWriter writer = new PoWriter();
            try {
                File poFile = new File(fileName);
                writer.write(catalog, poFile);
                msg = getString(R.string.resource_saved);
            } catch(IOException ex) {
                msg = getString(R.string.resource_saved_not);
            }
        }
        showNotification(msg);
    }

    protected void prevMessage() {
        if (iterator != null && iterator.hasPrevious()) {
            if (directionForward == true) {
                directionForward = false;
                iterator.previous();
            }
            this.fillMsgWidgets(iterator.previous());
        }
    }

    protected void nextMessage() {
        if (iterator != null && iterator.hasNext()) {
            if (directionForward == false) {
                directionForward = true;
                iterator.next();
            }
            this.fillMsgWidgets(iterator.next());
        }
    }

    protected void fillMsgWidgets(Message message) {
        this.token = message;
        this.widgetMsgId.setText(message.getMsgid());
        this.widgetMsgStr.setText(message.getMsgstr());
        this.updateTitle();
    }

    /**
     * Copies the content from original widget into translated one.
     */
    protected void msgstrCopy() {
        if (this.token != null) {
            this.widgetMsgStr.setText(this.widgetMsgId.getText());
        }
    }

    /**
     * Refreshes total/translated/fuzzy/obsolete counters.
     */
    protected void refreshCounters() {
        int total = 0;
        int trans = 0;
        int fuzzy = 0;
        int trash = 0;

        for (Message m : this.messages){
            total += 1;
            if (m.isFuzzy()) {
                fuzzy += 1;
            }
            if (m.isObsolete()) {
                trash += 1;
            }
            if (! m.getMsgstr().equals("")) {
                trans += 1;
            }
        }
        this.msgTotal = total;
        this.msgTrans = trans;
        this.msgFuzzy = fuzzy;
        this.msgTrash = trash;
    }

    /**
     * Updates activity's title.
     */
    protected void updateTitle() {
        this.refreshCounters();
        setProgressBarVisibility(true);
        setProgress(this.msgTrans * 10000 / this.msgTotal);
        String fileName = new File(this.resourceFileName).getName();
        String msg = "[" +
            this.msgTotal + "/" +
            this.msgTrans + "/" +
            this.msgFuzzy + "/" +
            this.msgTrash + "] " +
            fileName;
        setTitle(msg);
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
