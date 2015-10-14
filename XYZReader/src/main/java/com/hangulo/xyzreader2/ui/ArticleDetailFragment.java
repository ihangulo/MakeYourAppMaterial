package com.hangulo.xyzreader2.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.graphics.Typeface;

import android.graphics.drawable.Drawable;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;

import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.ProgressBar;
import android.widget.TextView;

import com.hangulo.xyzreader2.R;
import com.hangulo.xyzreader2.data.ArticleLoader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;


/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFFFFFFFF;

    private ImageView mPhotoView;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;
    private ProgressBar mLoadingCircle;
    private AppBarLayout mAppBarLayout;
    private Button mLoadingFailed;

    String articleTitle;
    String mImageUrl;
    CollapsingToolbarLayout collapsingToolbar;
    private LinearLayout mMetaBar;
    int colorPrimary ;
    int colorPrimaryDark ;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        setHasOptionsMenu(true);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {



        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        colorPrimary = ContextCompat.getColor(getActivity(), R.color.colorPrimary);
        colorPrimaryDark = ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mLoadingFailed = (Button) mRootView.findViewById (R.id.loading_failed); // laoding failed
        mLoadingFailed.setVisibility(View.INVISIBLE);
        mLoadingFailed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                reloadImage(mImageUrl);
            }
        });


        collapsingToolbar = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar);

        // http://blog.grafixartist.com/toolbar-animation-with-android-design-support-library/
        Toolbar toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ActionBar actionBar =   ((AppCompatActivity) getActivity()).getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true); // setup up arrow

        mAppBarLayout= (AppBarLayout) mRootView.findViewById (R.id.app_bar_layout); // appbar layout

        mMetaBar = (LinearLayout) mRootView.findViewById(R.id.meta_bar);
//        ImageView header = (ImageView) mRootView.findViewById(R.id.photo);

        mLoadingCircle=(ProgressBar) mRootView.findViewById(R.id.loadingImageCircle); // loading...

        bindViews();

//        updateStatusBar();
        return mRootView;
    }




//    @Override
//    public void setUserVisibleHint(boolean isVisibleToUser) {
//        super.setUserVisibleHint(isVisibleToUser);
//        if (isVisibleToUser) {
//            Log.v("Detail", "now Showing:"+mItemId);
//        } else {
//        }
//    }


    // setting status bar color to change when it ready (from outer activity)

    // using Picasso target
        private Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // loading of the bitmap was a success

                mLoadingCircle.setVisibility(View.INVISIBLE); // show off loading circle
                mLoadingFailed.setVisibility(View.INVISIBLE); // show off fail button

                if (bitmap != null) {

                    mPhotoView.setImageBitmap(bitmap); // set image
                    // set title color palete
                    Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            mMutedColor = palette.getMutedColor(colorPrimary);
                            collapsingToolbar.setContentScrimColor(mMutedColor);

                            mMetaBar.setBackgroundColor(mMutedColor);
                            collapsingToolbar.setStatusBarScrimColor(palette.getDarkMutedColor(colorPrimaryDark));
                        }
                    });

                    mAppBarLayout.setExpanded(true); // expand appbar

                }

            }



            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

                // failed --> loading error
                mLoadingCircle.setVisibility(View.INVISIBLE); // show off loading circle
                mLoadingFailed.setVisibility(View.VISIBLE);
                mAppBarLayout.setExpanded(false); // fold Appbar
                collapsingToolbar.setTitle(articleTitle); // if do not this, then title is vanished~! (I think it's system bug)

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };



    // reload image
    void reloadImage(String url) {

        if (url==null || !URLUtil.isValidUrl(url)) // check url
            return;

        mLoadingFailed.setVisibility(View.INVISIBLE);
        mLoadingCircle.setVisibility(View.VISIBLE); // show loading circle

        Picasso.with(getActivity())
                .load(url)
                .into(target);
    }



    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);

//        bylineView.setMovementMethod(new LinkMovementMethod());


        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf")); // set font

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));


            mImageUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);

            reloadImage(mImageUrl);

        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();

        // set title
        articleTitle= mCursor.getString(ArticleLoader.Query.TITLE);

        collapsingToolbar.setTitle(articleTitle);

        // set share button
        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(articleTitle)
                        .getIntent(), getString(R.string.action_share)));
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed(); // home button --> back key
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
