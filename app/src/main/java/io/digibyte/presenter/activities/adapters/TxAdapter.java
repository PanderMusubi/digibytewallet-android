package io.digibyte.presenter.activities.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.digibyte.R;
import io.digibyte.presenter.entities.VerticalSpaceItemDecoration;
import io.digibyte.tools.adapter.TransactionListAdapter;
import jp.wasabeef.recyclerview.animators.SlideInDownAnimator;

public class TxAdapter extends PagerAdapter {

    private Context context;
    private RecyclerView allRecycler;
    private TransactionListAdapter allAdapter;
    private RecyclerView sentRecycler;
    private TransactionListAdapter sentAdapter;
    private RecyclerView receivedRecycler;
    private TransactionListAdapter receivedAdapter;

    public RecyclerView getAllRecycler() {
        return allRecycler;
    }

    public TransactionListAdapter getAllAdapter() {
        return allAdapter;
    }

    public RecyclerView getSentRecycler() {
        return sentRecycler;
    }

    public TransactionListAdapter getSentAdapter() {
        return sentAdapter;
    }

    public RecyclerView getReceivedRecycler() {
        return receivedRecycler;
    }

    public TransactionListAdapter getReceivedAdapter() {
        return receivedAdapter;
    }

    public TxAdapter(Context context) {
        this.context = context;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        RecyclerView layout = (RecyclerView) LayoutInflater.from(context).inflate(
                R.layout.activity_bread_recycler, collection, false);
        SlideInDownAnimator slideInDownAnimator = new SlideInDownAnimator();
        slideInDownAnimator.setAddDuration(500);
        slideInDownAnimator.setChangeDuration(0);
        layout.addItemDecoration(new VerticalSpaceItemDecoration(4));
        layout.setItemAnimator(slideInDownAnimator);
        layout.setLayoutManager(new LinearLayoutManager(context));
        switch (position) {
            case 0:
                allAdapter = new TransactionListAdapter(layout);
                layout.setAdapter(allAdapter);
                allRecycler = layout;
                break;
            case 1:
                sentAdapter = new TransactionListAdapter(layout);
                layout.setAdapter(sentAdapter);
                sentRecycler = layout;
                break;
            case 2:
                receivedAdapter = new TransactionListAdapter(layout);
                layout.setAdapter(receivedAdapter);
                receivedRecycler = layout;
                break;
        }
        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return object == view;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            default:
            case 0:
                return context.getString(R.string.all);
            case 1:
                return context.getString(R.string.sent);
            case 2:
                return context.getString(R.string.received);
        }
    }
}