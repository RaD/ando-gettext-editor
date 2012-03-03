package ru.securelayer.rad.ando;

import android.app.Activity;
import android.os.Bundle;

import android.widget.TextView;
import android.widget.EditText;

public class AndoMain extends Activity
{
    private TextView textOriginal;
    private EditText editTranslated;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        textOriginal = (TextView) findViewById(R.id.textOriginal);
        editTranslated = (EditText) findViewById(R.id.editTranslated);

        textOriginal.setText("Hello, dude!");
        // editTranslated.setText("Привет, чувак!");
    }
}
