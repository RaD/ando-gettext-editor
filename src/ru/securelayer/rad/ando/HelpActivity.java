package ru.securelayer.rad.ando;

import android.os.Bundle;
import android.app.Activity;
import android.widget.TextView;
import android.view.Window;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import ru.securelayer.rad.ando.R;

public class HelpActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.help);

        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);

        TextView helpText = (TextView) findViewById(R.id.help_text);

        helpText.setText(Html.fromHtml(getString(R.string.help_text), this.imgGetter, null));
    }

    private ImageGetter imgGetter = new ImageGetter() {
        public Drawable getDrawable(String source) {
            Context ctx = getApplicationContext();
            Resources res = ctx.getResources();
            Drawable img = null;
            if ("icon".equals(source)) {
                img = res.getDrawable(R.drawable.icon);
            } else
            if ("help_status".equals(source)) {
                img = res.getDrawable(R.drawable.help_status);
            } else
            if ("help_gestures_1".equals(source)) {
                img = res.getDrawable(R.drawable.help_gestures_1);
            } else
            if ("help_gestures_2".equals(source)) {
                img = res.getDrawable(R.drawable.help_gestures_2);
            } else
            if ("help_gestures_3".equals(source)) {
                img = res.getDrawable(R.drawable.help_gestures_3);
            } else
            if ("help_gestures_4".equals(source)) {
                img = res.getDrawable(R.drawable.help_gestures_4);
            } else
            if ("help_gestures_5".equals(source)) {
                img = res.getDrawable(R.drawable.help_gestures_5);
            } else
            if ("help_gestures_6".equals(source)) {
                img = res.getDrawable(R.drawable.help_gestures_6);
            } else {
                img = res.getDrawable(R.drawable.help_status);
            }
            img.setBounds(0, 0, img.getIntrinsicWidth(), img.getIntrinsicHeight());
            return img;
        }
    };
}
