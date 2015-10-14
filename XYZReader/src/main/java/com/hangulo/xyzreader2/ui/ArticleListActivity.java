package com.hangulo.xyzreader2.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hangulo.xyzreader2.R;
import com.hangulo.xyzreader2.data.ArticleLoader;
import com.hangulo.xyzreader2.data.ItemsContract;
import com.hangulo.xyzreader2.data.UpdaterService;
import com.squareup.picasso.Picasso;

/*
 *   ================================================
 *        Android Devlelopment Nanodegree
 *        Project 5 - Make Your App Material
 *   ================================================
 *
 *        from : 5th OCT 2015
 *        to : 14th OCT 2015
 *
 *     Kwanghyun JUNG
 *     ihangulo@gmail.com
 *
 *    Android Devlelopment Nanodegree(Udacity)
 *
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private final static String LOG_TAG="ArticleListActivity";
    private Toolbar mToolbar;
    private CoordinatorLayout mCoordinator_layout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private int mPosition=AdapterView.INVALID_POSITION ; // current position of list
    ArticleListAdapter mAdapter;
    ProgressBar mLoadingCircle;
    boolean needForClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mCoordinator_layout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        if (mSwipeRefreshLayout!=null) {
            // set listener
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refresh();
                }
            });

        }
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mLoadingCircle = (ProgressBar) findViewById(R.id.loadingCircle);


        mAdapter = new ArticleListAdapter(); // make without cursor
        mAdapter.setHasStableIds(true);

        mEmptyView= findViewById(R.id.recyclerview_empty);
        mAdapter.setEmptyView(mEmptyView); // set Empty view
        mEmptyView.setVisibility(View.INVISIBLE);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
        else {
            mPosition = savedInstanceState.getInt("POSITION", AdapterView.INVALID_POSITION );
            mAdapter.onRestoreInstanceState(savedInstanceState); // state restore
        }
    }

    private void refresh() {
        mPosition=AdapterView.INVALID_POSITION ;
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onResume() {
        if(mPosition != AdapterView.INVALID_POSITION )
            mRecyclerView.smoothScrollToPosition(mPosition); // scroll to position


        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(mPosition);
        if (null != vh) {
            mAdapter.selectView(vh);

        } else needForClick=true;

        super.onResume();
        Log.v(LOG_TAG,"onresume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
        mLoadingCircle.setVisibility(View.INVISIBLE); // showing off loading circle
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("POSITION", mPosition);
        if(mAdapter!=null)
            mAdapter.onSaveInstanceState(outState); // save current selection
        super.onSaveInstanceState(outState);
    }



    private boolean mIsRefreshing = false;

    // Broadcast Receiver
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {

                if (intent.getBooleanExtra(UpdaterService.BROADCAST_NOT_ONLINE, false)){ // if not online
                    Snackbar.make(mCoordinator_layout, getString(R.string.not_online), Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.msg_retry), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    refresh(); // touch retry button

                                }
                            })
                            .setActionTextColor(ContextCompat.getColor(context, R.color.colorAccent))
                            .show();

// Changing message text color
                    mIsRefreshing = false;
                    updateRefreshingUI();
                }else {
                    mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                    Log.v(LOG_TAG, "receive:" + UpdaterService.BROADCAST_ACTION_STATE_CHANGE + " mIsRefreshing:" + mIsRefreshing);
                    updateRefreshingUI();

                }
            }
        }
    };


    private void updateRefreshingUI() {

        if(mSwipeRefreshLayout == null) return;

        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
            }
        });

        Log.v(LOG_TAG, "mSwipeRefreshLayout.setRefreshing:" +  mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        mAdapter.swapCursor(cursor); // set cursor
        mRecyclerView.setAdapter(mAdapter);

        // it's not good for layout -->
//        int columnCount = getResources().getInteger(R.integer.list_column_count);
//        StaggeredGridLayoutManager sglm =
//                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
//        mRecyclerView.setLayoutManager(sglm);

        // recyclerview divider
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);


        // i'll use linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);


        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        if(mPosition != AdapterView.INVALID_POSITION ) {
            mRecyclerView.smoothScrollToPosition(mPosition); // scroll to position

            RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(mPosition);
            if (null != vh) {
                mAdapter.selectView(vh);
            }
        }

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (needForClick) {
                    RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(mPosition);
                    if (null != vh) { //&& mAutoSelectView) {
                        mAdapter.selectView(vh);
                    }
                    needForClick = false;
                }

            }
        });


        mLoadingCircle.setVisibility(View.INVISIBLE);//show off loading circle

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case 100:
                mPosition = data.getIntExtra("POSITION",0);

                if(mPosition != AdapterView.INVALID_POSITION )
                    mRecyclerView.smoothScrollToPosition(mPosition); // scroll to position

                break;

            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ViewHolder> {

        private Cursor mCursor;
        private ItemChoiceManager mICM;
        private View EmptyView;


        public ArticleListAdapter(Cursor cursor) {
            mCursor = cursor;
            mICM = new ItemChoiceManager(this); // for selecting status
            mICM.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);  // choice single mode
        }

        public ArticleListAdapter() {
            mICM = new ItemChoiceManager(this); // for selecting status
            mICM.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);  // choice single mode
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    mPosition = vh.getAdapterPosition(); // set current position

                    startActivityForResult(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(mPosition))), 0); // start detail activity

                    mLoadingCircle.setVisibility(View.VISIBLE); // showing loading circle

                    mICM.onClick(vh); // process click (for touch_selector)
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));

            Picasso.with(getApplicationContext())
                    .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .fit().centerCrop()
                    .into(holder.thumbnailView);

            mICM.onBindViewHolder(holder, position);

        }

        public void onSaveInstanceState(Bundle outState) {
            mICM.onSaveInstanceState(outState);
        }

        public void onRestoreInstanceState(Bundle savedInstanceState) {
            mICM.onRestoreInstanceState(savedInstanceState);
        }

        public int getSelectedItemPosition() {
            return mICM.getSelectedItemPosition();
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }

        public void selectView(RecyclerView.ViewHolder viewHolder) {
            if ( viewHolder instanceof ViewHolder ) {
                ViewHolder vfh = (ViewHolder)viewHolder;
                vfh.onClick(vfh.itemView);
            }
        }


        public void setEmptyView(View empty_view) {
            EmptyView = empty_view;
        }
        public void swapCursor(Cursor newCursor) {
            mCursor = newCursor;
            notifyDataSetChanged();
            if (EmptyView != null)
                EmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }

        public Cursor getCursor() {
            return mCursor;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements  View.OnClickListener {
            public ImageView thumbnailView;
            public TextView titleView;
            public TextView subtitleView;

            public ViewHolder(View view) {
                super(view);
                thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);


                titleView = (TextView) view.findViewById(R.id.article_title);
                subtitleView = (TextView) view.findViewById(R.id.article_subtitle);

            }

            @Override
            public void onClick(View v) {
                mICM.onClick(this);
            }
        }

    }


}
