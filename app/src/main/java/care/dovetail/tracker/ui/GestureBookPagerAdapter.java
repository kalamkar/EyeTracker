package care.dovetail.tracker.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import care.dovetail.tracker.R;

/**
 * Created by abhi on 4/20/17.
 */

public class GestureBookPagerAdapter extends FragmentStatePagerAdapter {
    private static final String TAG = "GestureBookPagerAdapter";

    private final PageFragment pages[];

    public GestureBookPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        String pageContents[] = context.getResources().getStringArray(R.array.pages);
        pages = new PageFragment[pageContents.length];
        for (int i = 0; i < pageContents.length; i++) {
            pages[i] = new PageFragment();
            pages[i].setContent(pageContents[i]);
        }
    }

    @Override
    public int getCount() {
        return pages.length;
    }

    @Override
    public Fragment getItem(int position) {
        return pages[position];
    }

    public static class PageFragment extends Fragment {
        private String content;

        public void setContent(String content) {
            this.content = content;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_page, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            Log.d(TAG, content);
            ((TextView) view.findViewById(R.id.text)).setText(content);
        }
    }
}
