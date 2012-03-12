package ru.securelayer.rad.ando;

import java.util.ArrayList;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.EditText;
import android.os.Parcelable;
import android.content.Context;

import android.support.v4.view.ViewPager;
import android.support.v4.view.PagerAdapter;

import org.fedorahosted.tennera.jgettext.Message;

import ru.securelayer.rad.ando.R;

public class MessageAdapter extends PagerAdapter{

    private LayoutInflater inflater = null;
    private TextView original;
    private EditText translated;
    private Context ctx;
    private ArrayList<Message> messages;

    public MessageAdapter(Context ctx, ArrayList<Message> messages) {
        this.ctx = ctx;
        this.messages = messages;
    }

    @Override
    public int getCount() {
        return this.messages.size();
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
    public Object instantiateItem(ViewGroup collection, int position) {
        inflater = LayoutInflater.from(this.ctx);
        View page = inflater.inflate(R.layout.slider, null);
        ((ViewPager) collection).addView(page, 0);
        Message msg = this.messages.get(position);
        this.original = (TextView) page.findViewById(R.id.textOriginal);
        this.translated = (EditText) page.findViewById(R.id.editTranslated);
        this.original.setText(msg.getMsgid());
        this.translated.setText(msg.getMsgstr());
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
    public void destroyItem(ViewGroup collection, int position, Object page) {
        this.applyIfChanged((View) page, position);
        ((ViewPager) collection).removeView((View) page);
    }

    /**
     * Compare the content of translation widget and original message.
     * If they have a difference, then it updates the messages catalog.
     *
     * @param position The position at adapter (i.e. message list)
     *                 to be changed.
     */
    public void applyIfChanged(View page, int position) {
        Message msg = this.messages.get(position);
        TextView wOrig = (TextView) page.findViewById(R.id.textOriginal);
        EditText wEdit = (EditText) page.findViewById(R.id.editTranslated);
        String tOrig = msg.getMsgstr();
        String tEdit = wEdit.getText().toString();
        if (! tOrig.equals(tEdit)) {
            msg.setMsgstr(tEdit);
        }
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
    public void restoreState(Parcelable state, ClassLoader loader) {}

    @Override
    public Parcelable saveState() {
        return null;
    }
}
