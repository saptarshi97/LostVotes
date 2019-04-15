package in.mahe.lostvotes.OtherUtilities;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;


/**
 * Created by Saptarshi on 4/15/2019.
 */
public class GilroyTextView extends AppCompatTextView {

    public GilroyTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public GilroyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GilroyTextView(Context context) {
        super(context);
        init();
    }

    public void init() {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/Gilroy-Light.otf");
        setTypeface(tf ,Typeface.NORMAL);
    }
}