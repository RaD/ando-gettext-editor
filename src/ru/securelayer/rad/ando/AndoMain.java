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

    private static final int MSG_CURRENT = 0;
    private static final int MSG_NEXT = 1;
    private static final int MSG_NEXT_FUZZY = 2;
    private static final int MSG_NEXT_TRANS = 3;

    private TextView widgetMsgId = null;
    private EditText widgetMsgStr = null;

    private String resourceFileName = null;
    private Catalog catalog = null;
    private ArrayList<Message> messages = null;
    private Message token = null;
    private boolean isCatalogReady = false;

    private Boolean procTextChanged = false;

    private GestureLibrary mLibrary;
    private GestureOverlayView gestures;

    private int index = 0;

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
            case R.id.menu_fuzzy:
                msgstrFuzzy();
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
                Message msg = null;
                String action = prediction.name;
                if ("next".equals(action)) {
                    msg = this.getNext(+1, MSG_NEXT);
                } else if ("prev".equals(action)) {
                    msg = this.getNext(-1, MSG_NEXT);
                } else if ("next_fuzzy".equals(action)) {
                    msg = this.getNext(+1, MSG_NEXT_FUZZY);
                } else if ("prev_fuzzy".equals(action)) {
                    msg = this.getNext(-1, MSG_NEXT_FUZZY);
                } else if ("next_untranslated".equals(action)) {
                    msg = this.getNext(+1, MSG_NEXT_TRANS);
                } else if ("prev_untranslated".equals(action)) {
                    msg = this.getNext(-1, MSG_NEXT_TRANS);
                }
                if (msg != null) {
                    this.fillMsgWidgets(msg);
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
                for (Message m : this.catalog) {
                    if (! m.isHeader()) {
                        this.messages.add(m);
                    }
                }
                this.isCatalogReady = true;
                // Show notification
                showNotification(getString(R.string.resource_loaded));
                // Show first page
                this.index = 0;
                this.fillMsgWidgets(this.getNext(0, MSG_CURRENT));
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
        if (! this.isCatalogReady) {
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

    protected int cycleIndex(int current, int increment, int size) {
        int pointer = current;
        pointer += increment;
        if (0 > pointer)
            pointer = size - 1;
        if (size == pointer)
            pointer = 0;
        return pointer;
    }

    protected Message getNext(int increment, int action) {
        int notify = 0;
        int size = this.messages.size();
        boolean found = false;

        if (! this.isCatalogReady) {
            this.showNotification("No Messages!");
            return null;
        }

        if (MSG_CURRENT == action) {}
        if (MSG_NEXT == action) {
            this.index = this.cycleIndex(this.index, increment, size);
            notify = (-1 == increment) ? R.string.prev_token : R.string.next_token;
        }
        if (MSG_NEXT_FUZZY == action) {
            int pointer = this.cycleIndex(this.index, increment, size);
            do {
                Message msg = this.messages.get(pointer);
                if (msg.isFuzzy()) {
                    break;
                }
                pointer = this.cycleIndex(pointer, increment, size);
            } while (pointer != this.index);
            this.index = pointer;
            notify = (-1 == increment) ? R.string.prev_token_fuzzy : R.string.next_token_fuzzy;
        }
        if (MSG_NEXT_TRANS == action) {
            int pointer = this.cycleIndex(this.index, increment, size);
            do {
                Message msg = this.messages.get(pointer);
                if ("".equals(msg.getMsgstr())) {
                    break;
                }
                pointer = this.cycleIndex(pointer, increment, size);
            } while (pointer != this.index);
            this.index = pointer;
            notify = (-1 == increment) ? R.string.prev_token_trans : R.string.next_token_trans;
        }
        if (0 < notify)
            this.showNotification(getString(notify));
        return this.messages.get(this.index);
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

    protected void msgstrFuzzy() {
        if (this.token != null) {
            boolean state = this.token.isFuzzy();
            this.token.setFuzzy(! state);
            this.updateTitle();
            if (state) {
                showNotification(getString(R.string.fuzzy_unset));
            } else {
                showNotification(getString(R.string.fuzzy_set));
            }
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
            this.index + ":" +
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
