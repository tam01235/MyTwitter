package com.example.nhoxb.mysimpletwitter.ui.profile;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.nhoxb.mysimpletwitter.R;
import com.example.nhoxb.mysimpletwitter.TwitterApplication;
import com.example.nhoxb.mysimpletwitter.data.DataManager;
import com.example.nhoxb.mysimpletwitter.data.remote.model.Tweet;
import com.example.nhoxb.mysimpletwitter.ui.custom.DividerItemDecoration;
import com.example.nhoxb.mysimpletwitter.ui.custom.EndlessRecyclerViewScrollListener;
import com.example.nhoxb.mysimpletwitter.ui.detail.DetailActivity;
import com.example.nhoxb.mysimpletwitter.ui.timeline.TimelineActivity;
import com.example.nhoxb.mysimpletwitter.ui.timeline.TimelineAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;

import java.util.List;

import cz.msebera.android.httpclient.Header;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {
    public static final String KEY_TWEET_DETAIL = "tweet_detail";
    public static final String KEY_FRAGMENT_TYPE = "fragment_type";
    public static final int TWEET_LIST = 0;
    public static final int MEDIA_LIST = 1;
    public static final int LIKE_LIST = 2;
    private static final String TAG = ProfileFragment.class.getSimpleName();
    RecyclerView mRecyclerView;

    private TimelineAdapter mTimelineAdapter;
    private LinearLayoutManager layoutManager;
    private DataManager dataManager;
    private Context mContext;
    private Gson mGson;
    private int mType;
    private JsonHttpResponseHandler setTweetHandler = new JsonHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
            super.onSuccess(statusCode, headers, response);
            Log.v(TAG, "Load tweet success.");
            List<Tweet> mTweetList = mGson.fromJson(response.toString(), new TypeToken<List<Tweet>>() {
            }.getType());
            mTimelineAdapter.setTweet(mTweetList);
            EventBus.getDefault().post(new FragmentMessageEvent(true));
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            super.onFailure(statusCode, headers, responseString, throwable);
            Log.v(TAG, "Load tweet failed.");
            EventBus.getDefault().post(new FragmentMessageEvent(true));
        }
    };
    private JsonHttpResponseHandler addTweetHandler = new JsonHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
            super.onSuccess(statusCode, headers, response);
            Log.v(TAG, "Add tweets success.");
            List<Tweet> mTweetList = mGson.fromJson(response.toString(), new TypeToken<List<Tweet>>() {
            }.getType());
            mTimelineAdapter.addTweet(mTweetList);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            super.onFailure(statusCode, headers, responseString, throwable);
            Log.v(TAG, "Add tweets failed.");
        }
    };

    public ProfileFragment() {
        // Required empty public constructor

        dataManager = TwitterApplication.getDataManager();
    }

    public static ProfileFragment newInstance(int type) {
        Bundle args = new Bundle();
        args.putInt(KEY_FRAGMENT_TYPE, type);
        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static ProfileFragment newInstance() {

        Bundle args = new Bundle();

        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.rv_list_tweet);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGson = new Gson();

        mType = getArguments().getInt(KEY_FRAGMENT_TYPE, TWEET_LIST);

        //Recycler View, Adapter
        mTimelineAdapter = new TimelineAdapter(getContext());
        layoutManager = new LinearLayoutManager(getContext());
        RecyclerView.ItemDecoration decoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.setLayoutManager(layoutManager);
        mTimelineAdapter.setOnItemClickListener(new TimelineAdapter.OnItemTweetClickListener() {
            @Override
            public void onClick(Tweet tweet) {
                Bundle extras = new Bundle();
                extras.putParcelable(KEY_TWEET_DETAIL, tweet);
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtras(extras);
                startActivity(intent);
            }
        });
        mRecyclerView.setAdapter(mTimelineAdapter);
        mRecyclerView.addItemDecoration(decoration);
        mRecyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                loadMoreTweet(mType, page);
            }
        });


        loadTweets(mType);
    }

    private void loadTweets(int mType) {
        switch (mType) {
            case TWEET_LIST:
                populateUserTimeline();
                break;

            case MEDIA_LIST:
                populateMediaList();
                break;

            case LIKE_LIST:
                populateLikeList();
                break;

            default:
                Log.e(TAG, "Error loading tweets");
                break;
        }
    }

    private void populateLikeList() {
        dataManager.getUserFavourite(25, setTweetHandler);
    }


    private void populateUserTimeline() {
        dataManager.getUserTimeline(25, setTweetHandler);
    }

    private void populateMediaList() {
    }

    private void loadMoreTweet(int type, final int page) {
        switch (type) {
            case TWEET_LIST:
                dataManager.getHomeTimeline(page, addTweetHandler);
                break;
            case MEDIA_LIST:
                break;
            case LIKE_LIST:
                dataManager.getMentionTimeline(page, addTweetHandler);
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActivityMessageEvent(TimelineActivity.ActivityMessageEvent event) {
        if (event.isPullToRefresh) {
            loadTweets(mType);
        }

        if (event.isAddTweetToTop) {
            mTimelineAdapter.addTweetOnTop(event.getTweet());
            mRecyclerView.smoothScrollToPosition(0);
        }
    }

    public static class FragmentMessageEvent {
        boolean isFinishRefresh = false;

        public FragmentMessageEvent(boolean isFinishRefresh) {
            this.isFinishRefresh = isFinishRefresh;
        }

        public boolean isFinishRefresh() {
            return isFinishRefresh;
        }
    }


}
