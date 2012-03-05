package ru.securelayer.rad.ando;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;

import com.kaloer.filepicker.FilePickerActivity;
import org.fedorahosted.tennera.jgettext.Catalog;
import org.fedorahosted.tennera.jgettext.Message;
import org.fedorahosted.tennera.jgettext.catalog.parse.ExtendedCatalogParser;
import org.fedorahosted.tennera.jgettext.catalog.parse.ParseException;
import antlr.RecognitionException;
import antlr.TokenStreamException;

import ru.securelayer.rad.ando.R;

public class AndoMain extends Activity implements OnClickListener
{
    private static final int REQUEST_PICK_FILE = 1;
    private TextView textOriginal;
    private EditText editTranslated;
    private Button openButton;
    private int entryCount = 0;
    private int obsoleteCount = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        textOriginal = (TextView) findViewById(R.id.textOriginal);
        editTranslated = (EditText) findViewById(R.id.editTranslated);
        openButton = (Button) findViewById(R.id.openButton);

        textOriginal.setText("Hello, dude!");
        editTranslated.setText("Привет, чувак!");
        openButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        switch(v.getId()) {
        case R.id.openButton:
            // create an intent for the file picker activity
            Intent intent = new Intent(this, FilePickerActivity.class);
            // set the initial directory to be the sdcard
            // intent.putExtra(FilePickerActivity.EXTRA_FILE_PATH, Environment.getExternalStorageDirectory());
            // only make .po files visible
            ArrayList<String> extensions = new ArrayList<String>();
            //extensions.add(".po");
            intent.putExtra(FilePickerActivity.EXTRA_ACCEPTED_FILE_EXTENSIONS, extensions);
            // start the activity
            startActivityForResult(intent, REQUEST_PICK_FILE);
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch(requestCode) {
            case REQUEST_PICK_FILE:
                if (data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH)) {
                    // Get the file path
                    try {
                        try {
                            File poFile = new File(data.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH));
                                                // Parse a file
                            ExtendedCatalogParser parser = new ExtendedCatalogParser(poFile);
                            try {
                                parser.catalog();
                            } catch (RecognitionException ex) {}
                              catch (TokenStreamException ex) {}
                            Catalog catalog = parser.getCatalog();
                            // Iterate of file's items.
                            for (Message m : catalog){
                                    entryCount++;
                                    if(m.isObsolete()) obsoleteCount++;
                            }
                            // Set the file path text view
                            editTranslated.setText(poFile.getPath());
                        } catch(FileNotFoundException ex) {}
                    } catch(IOException ex) {}
                    //R.id.totalValue.setText(entryCount);
                    //R.id.transValue.setText(obsoleteCount);

                }
            }
        }
    }
}
