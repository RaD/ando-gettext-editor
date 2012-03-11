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
import android.os.Parcelable;
import android.content.Intent;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import android.support.v4.view.PagerAdapter;
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

public class AndoMain extends Activity
{
    private static final int REQUEST_PICK_FILE = 1;

    private String resourceFileName;
    private TextView textFileName;
    private TextView textOriginal;
    private EditText editTranslated;
    private Button openButton;
    private Button saveButton;
    private Button prevButton;
    private Button nextButton;
    private Button copyButton;
    private Catalog catalog;
    private Message message;
    private ArrayList<Message> messages;
    private ListIterator<Message> iterator;
    private Boolean directionForward = true;
    private int entryCount = 0;
    private int obsoleteCount = 0;
    private Context ctx;
    private MessageAdapter pagerAdapter;
    private ViewPager messagePager;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        ctx = this;

        pagerAdapter = new MessageAdapter();
        messagePager = (ViewPager) findViewById(R.id.view_pager);
        messagePager.setAdapter(pagerAdapter);

        textFileName = (TextView) findViewById(R.id.textFileName);
        textOriginal = (TextView) findViewById(R.id.textOriginal);
        editTranslated = (EditText) findViewById(R.id.editTranslated);

        messages = new ArrayList<Message>();
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
                iterator = messages.listIterator();
                // Set the file path text view
                textFileName.setText(fileName);
                // Show first token
                // nextMessage();
                // Show notification
                CharSequence message = getString(R.string.resource_loaded);
                showNotification(message);
            } catch(FileNotFoundException ex) {}
        } catch(IOException ex) {}
    }

    protected void saveCatalog(String fileName) {
        applyIfChanged();
        PoWriter writer = new PoWriter();
        try {
            File poFile = new File(fileName);
            writer.write(catalog, poFile);
            CharSequence message = getString(R.string.resource_saved);
            showNotification(message);
        } catch(IOException ex) {
            CharSequence message = getString(R.string.resource_saved_not);
            showNotification(message);
        }
    }

    protected void applyIfChanged() {
        if (message != null) {
            String textOrig = message.getMsgstr();
            String textEdit = editTranslated.getText().toString();
            if (! textOrig.equals(textEdit)) {
                message.setMsgstr(textEdit);
            }
        }
    }

    protected void prevMessage() {
        applyIfChanged();
        if (directionForward == true && iterator.hasPrevious()) {
            directionForward = false;
            iterator.previous();
        }
        if (iterator.hasPrevious()) {
            message = iterator.previous();
            fillMsgWidgets(message);
        }
    }

    protected void nextMessage() {
        applyIfChanged();
        if (directionForward == false && iterator.hasNext()) {
            directionForward = true;
            iterator.next();
        }
        if (iterator.hasNext()) {
            message = iterator.next();
            fillMsgWidgets(message);
        }
    }

    protected void fillMsgWidgets(Message message) {
        textOriginal.setText(message.getMsgid());
        editTranslated.setText(message.getMsgstr());
    }

    protected void msgstrCopy() {
        editTranslated.setText(textOriginal.getText());
    }

    protected void showNotification(CharSequence message) {
        Context ctx = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(ctx, message, duration);
        toast.show();
    }





    private class MessageAdapter extends PagerAdapter{

        LayoutInflater inflater = null;

        @Override
        public int getCount() {
            return messages.size();
        }

        /**
         * Create the page for the given position.  The adapter is responsible
         * for adding the view to the container given here, although it only
         * must ensure this is done by the time it returns from
         * {@link #finishUpdate()}.
         *
         * @param container The containing View in which the page will be shown.
         * @param position The page position to be instantiated.
         * @return Returns an Object representing the new page.  This does not
         * need to be a View, but can be some other container of the page.
         */
        @Override
        public Object instantiateItem(View collection, int position) {
            inflater = LayoutInflater.from(ctx);
            View page = inflater.inflate(R.layout.slider, null);
            ((ViewPager) collection).addView(page, 0);
            TextView original = (TextView) page.findViewById(R.id.textOriginal);
            EditText translated = (EditText) page.findViewById(R.id.editTranslated);
            Message message = messages.get(position);
            original.setText(message.getMsgid());
            translated.setText(message.getMsgstr());
            return page;
        }

        /**
         * Remove a page for the given position.  The adapter is responsible
         * for removing the view from its container, although it only must ensure
         * this is done by the time it returns from {@link #finishUpdate()}.
         *
         * @param container The containing View from which the page will be removed.
         * @param position The page position to be removed.
         * @param object The same object that was returned by
         * {@link #instantiateItem(View, int)}.
         */
        @Override
        public void destroyItem(View collection, int position, Object view) {
            ((ViewPager) collection).removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((View)object);
        }

        /**
         * Called when the a change in the shown pages has been completed.  At this
         * point you must ensure that all of the pages have actually been added or
         * removed from the container as appropriate.
         * @param container The containing View which is displaying this adapter's
         * page views.
         */
        @Override
        public void finishUpdate(View arg0) {}

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {}

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View arg0) {}
    }
}
