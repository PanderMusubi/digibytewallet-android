package io.digibyte.presenter.activities;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.LayoutTransition;
import android.content.Intent;

import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.google.android.material.appbar.AppBarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.appcompat.content.res.AppCompatResources;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.ToxicBakery.viewpager.transforms.CubeOutTransformer;
import com.appolica.flubber.Flubber;

import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.Unbinder;
import io.digibyte.DigiByte;
import io.digibyte.R;
import io.digibyte.databinding.ActivityBreadBinding;
import io.digibyte.presenter.activities.adapters.TxAdapter;
import io.digibyte.presenter.activities.settings.SecurityCenterActivity;
import io.digibyte.presenter.activities.settings.SettingsActivity;
import io.digibyte.presenter.activities.settings.SyncBlockchainActivity;
import io.digibyte.presenter.activities.util.ActivityUTILS;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.presenter.entities.TxItem;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.list.items.ListItemTransactionData;
import io.digibyte.tools.manager.BRApiManager;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.manager.SyncManager;
import io.digibyte.tools.manager.TxManager;
import io.digibyte.tools.manager.TxManager.onStatusListener;
import io.digibyte.tools.sqlite.TransactionDataSource;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.tools.util.ViewUtils;
import io.digibyte.wallet.BRPeerManager;
import io.digibyte.wallet.BRWalletManager;

/**
 * BreadWallet
 * <p/>
 * Created by Noah Seidman <noah@noahseidman.com> on 4/14/18.
 * Copyright (c) 2018 DigiByte Holdings
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class BreadActivity extends BRActivity implements BRWalletManager.OnBalanceChanged,
        BRPeerManager.OnTxStatusUpdate, BRSharedPrefs.OnIsoChangedListener,
        TransactionDataSource.OnTxAddedListener, SyncManager.onStatusListener, onStatusListener {

    ActivityBreadBinding bindings;
    private Unbinder unbinder;
    private Handler handler = new Handler(Looper.getMainLooper());
    private TxAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindings = DataBindingUtil.setContentView(this, R.layout.activity_bread);
        bindings.digiSymbolBackground.
                setBackground(AppCompatResources.getDrawable(DigiByte.getContext(),
                        R.drawable.nav_drawer_header));
        bindings.setPagerAdapter(adapter = new TxAdapter(this));
        bindings.txPager.setOffscreenPageLimit(2);
        bindings.txPager.setPageTransformer(true, new CubeOutTransformer());
        bindings.tabLayout.setupWithViewPager(bindings.txPager);
        bindings.contentContainer.getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);
        ViewUtils.increaceClickableArea(bindings.qrButton);
        ViewUtils.increaceClickableArea(bindings.navDrawer);
        ViewUtils.increaceClickableArea(bindings.digiidButton);
        unbinder = ButterKnife.bind(this);
        Animator animator = AnimatorInflater.loadAnimator(this, R.animator.from_bottom);
        animator.setTarget(bindings.bottomNavigationLayout);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
        animator = AnimatorInflater.loadAnimator(this, R.animator.from_top);
        animator.setTarget(bindings.tabLayout);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private Runnable showSyncButtonRunnable = new Runnable() {
        @Override
        public void run() {
            bindings.syncButton.setVisibility(View.VISIBLE);
            Flubber.with()
                    .animation(Flubber.AnimationPreset.FLIP_Y)
                    .interpolator(Flubber.Curve.BZR_EASE_IN_OUT_QUAD)
                    .duration(1000)
                    .autoStart(true)
                    .createFor(findViewById(R.id.sync_button));
        }
    };

    @Override
    public void onSyncManagerStarted() {
        handler.postDelayed(showSyncButtonRunnable, 10000);
        CoordinatorLayout.LayoutParams coordinatorLayoutParams =
                (CoordinatorLayout.LayoutParams) bindings.contentContainer.getLayoutParams();
        coordinatorLayoutParams.setBehavior(null);
        bindings.syncContainer.setVisibility(View.VISIBLE);
        bindings.toolbarLayout.setVisibility(View.GONE);
        bindings.animationView.playAnimation();
        updateSyncText();
    }

    @Override
    public void onSyncManagerUpdate() {
        handler.removeCallbacks(showSyncButtonRunnable);
        bindings.syncButton.setVisibility(View.GONE);
        updateSyncText();
    }

    @Override
    public void onSyncManagerFinished() {
        CoordinatorLayout.LayoutParams coordinatorLayoutParams =
                (CoordinatorLayout.LayoutParams) bindings.contentContainer.getLayoutParams();
        coordinatorLayoutParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        handler.removeCallbacks(showSyncButtonRunnable);
        bindings.syncButton.setVisibility(View.GONE);
        bindings.syncContainer.setVisibility(View.GONE);
        bindings.toolbarLayout.setVisibility(View.VISIBLE);
        bindings.animationView.cancelAnimation();
    }

    @Override
    public void onSyncFailed() {
        CoordinatorLayout.LayoutParams coordinatorLayoutParams =
                (CoordinatorLayout.LayoutParams) bindings.contentContainer.getLayoutParams();
        coordinatorLayoutParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        handler.removeCallbacks(showSyncButtonRunnable);
        bindings.syncButton.setVisibility(View.GONE);
        bindings.syncContainer.setVisibility(View.GONE);
        bindings.toolbarLayout.setVisibility(View.VISIBLE);
        bindings.animationView.cancelAnimation();
    }

    private void updateSyncText() {
        Locale current = getResources().getConfiguration().locale;
        Date time = new Date(SyncManager.getInstance().getLastBlockTimestamp() * 1000);

        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);

        bindings.syncText.setText(SyncManager.getInstance().getLastBlockTimestamp() == 0
                ? DigiByte.getContext().getString(R.string.NodeSelector_statusLabel) + ": "
                + DigiByte.getContext().getString(R.string.SyncingView_connecting)
                : df.format(Double.valueOf(SyncManager.getInstance().getProgress() * 100d)) + "%"
                        + " - " + DateFormat.getDateInstance(DateFormat.SHORT, current).format(time)
                        + ", " + DateFormat.getTimeInstance(DateFormat.SHORT, current).format(
                        time));
    }

    @Override
    public void onTxManagerUpdate(TxItem[] newTxItems) {
        BRWalletManager.getInstance().refreshBalance(DigiByte.getContext());
        if (newTxItems == null || newTxItems.length == 0) {
            return;
        }
        ArrayList<ListItemTransactionData> newTransactions = getNewTransactionsData(newTxItems);
        ArrayList<ListItemTransactionData> transactionsToAdd = removeAllExistingEntries(
                newTransactions);
        if (transactionsToAdd.size() > 0) {
            adapter.getAllAdapter().addTransactions(transactionsToAdd);
            adapter.getSentAdapter().addTransactions(
                    convertNewTransactionsForAdapter(Adapter.SENT, transactionsToAdd));
            adapter.getReceivedAdapter().addTransactions(
                    convertNewTransactionsForAdapter(Adapter.RECEIVED, transactionsToAdd));
            adapter.getAllRecycler().smoothScrollToPosition(0);
            adapter.getSentRecycler().smoothScrollToPosition(0);
            adapter.getReceivedRecycler().smoothScrollToPosition(0);
            notifyDataSetChangeForAll();
        } else {
            adapter.getAllAdapter().updateTransactions(newTransactions);
            adapter.getSentAdapter().updateTransactions(newTransactions);
            adapter.getReceivedAdapter().updateTransactions(newTransactions);
            notifyDataSetChangeForAll();
        }
    }

    private enum Adapter {
        SENT, RECEIVED
    }

    private ArrayList<ListItemTransactionData> convertNewTransactionsForAdapter(Adapter adapter,
            ArrayList<ListItemTransactionData> transactions) {
        ArrayList<ListItemTransactionData> transactionList = new ArrayList<>();
        for (int index = 0; index < transactions.size(); index++) {
            ListItemTransactionData transactionData = transactions.get(index);
            TxItem item = transactions.get(index).transactionItem;
            if (adapter == Adapter.RECEIVED && item.getSent() == 0) {
                transactionList.add(transactionData);
            } else if (adapter == Adapter.SENT && item.getSent() > 0) {
                transactionList.add(transactionData);
            }
        }
        return transactionList;
    }

    private ArrayList<ListItemTransactionData> getNewTransactionsData(TxItem[] newTxItems) {
        ArrayList<ListItemTransactionData> newTransactionsData = new ArrayList();
        ArrayList<TxItem> newTransactions = new ArrayList(Arrays.asList(newTxItems));
        Collections.sort(newTransactions,
                (t1, t2) -> Long.valueOf(t1.getTimeStamp()).compareTo(t2.getTimeStamp()));
        for (TxItem tx : newTransactions) {
            newTransactionsData.add(new ListItemTransactionData(newTransactions.indexOf(tx),
                    newTransactions.size(), tx));
        }
        return newTransactionsData;
    }

    private ArrayList<ListItemTransactionData> removeAllExistingEntries(
            ArrayList<ListItemTransactionData> newTransactions) {
        return new ArrayList<ListItemTransactionData>(newTransactions) {{
            removeAll(adapter.getAllAdapter().getTransactions());
        }};
    }

    private void updateAmounts() {
        ActivityUTILS.updateDigibyteDollarValues(this, bindings.primaryPrice,
                bindings.secondaryPrice);
    }

    @Override
    public void onStatusUpdate() {
        TxManager.getInstance().updateTxList();
    }

    @Override
    public void onIsoChanged(String iso) {
        updateAmounts();
    }

    @Override
    public void onTxAdded() {
        TxManager.getInstance().updateTxList();
    }

    @Override
    public void onBalanceChanged(final long balance) {
        updateAmounts();
    }

    @Override
    public void showSendConfirmDialog(final String message, final int error, byte[] txHash) {
        BRExecutor.getInstance().forMainThreadTasks().execute(() -> {
            BRAnimator.showBreadSignal(BreadActivity.this,
                    error == 0 ? getString(R.string.Alerts_sendSuccess)
                            : getString(R.string.Alert_error),
                    error == 0 ? getString(R.string.Alerts_sendSuccessSubheader)
                            : message, error == 0 ? R.raw.success_check
                            : R.raw.error_check, () -> {
                        try {
                            getSupportFragmentManager().popBackStack();
                        } catch (IllegalStateException e) {
                        }
                    });
        });
    }

    @OnClick(R.id.nav_drawer)
    void onNavButtonClick(View view) {
        try {
            bindings.drawerLayout.openDrawer(GravityCompat.START);
        } catch (IllegalArgumentException e) {
            //Race condition inflating the hierarchy?
        }
    }

    @OnClick(R.id.main_action)
    void onMenuButtonClick(View view) {
        BRAnimator.showMenuFragment(BreadActivity.this);
    }

    @OnClick(R.id.digiid_button)
    void onDigiIDButtonClick(View view) {
        BRAnimator.openScanner(this);
    }

    @OnClick(R.id.primary_price)
    void onPrimaryPriceClick(View view) {
        BRSharedPrefs.putPreferredBTC(BreadActivity.this, true);
        notifyDataSetChangeForAll();
    }

    @OnClick(R.id.secondary_price)
    void onSecondaryPriceClick(View view) {
        BRSharedPrefs.putPreferredBTC(BreadActivity.this, false);
        notifyDataSetChangeForAll();
    }

    @OnClick(R.id.sync_button)
    void onSyncButtonClick(View view) {
        startActivity(new Intent(BreadActivity.this,
                SyncBlockchainActivity.class));
    }

    private void notifyDataSetChangeForAll() {
        adapter.getAllAdapter().notifyDataChanged();
        adapter.getSentAdapter().notifyDataChanged();
        adapter.getReceivedAdapter().notifyDataChanged();
    }

    @OnClick(R.id.security_center)
    void onSecurityCenterClick(View view) {
        startActivity(new Intent(this, SecurityCenterActivity.class));
    }

    @OnClick(R.id.settings)
    void onSettingsClick(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }


    @OnClick(R.id.lock)
    void onLockClick(View view) {
        BRAnimator.startBreadActivity(this, true);
    }

    @OnClick(R.id.qr_button)
    void onQRClick(View view) {
        BRAnimator.openScanner(this);
    }

    @OnLongClick(R.id.qr_button)
    boolean onQRLongClick() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                BRActivity.QR_IMAGE_PROCESS);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAmounts();
        BRWalletManager.getInstance().addBalanceChangedListener(this);
        BRPeerManager.getInstance().addStatusUpdateListener(this);
        BRSharedPrefs.addIsoChangedListener(this);
        TxManager.getInstance().addListener(this);
        SyncManager.getInstance().addListener(this);
        BRWalletManager.getInstance().refreshBalance(this);
        DigiByte.SyncBlockchainJob.scheduleJob();
        TxManager.getInstance().updateTxList();
        BRApiManager.getInstance().asyncUpdateCurrencyData(this);
        SyncManager.getInstance().startSyncingProgressThread();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BRWalletManager.getInstance().removeListener(this);
        BRPeerManager.getInstance().removeListener(this);
        BRSharedPrefs.removeListener(this);
        TxManager.getInstance().removeListener(this);
        SyncManager.getInstance().removeListener(this);
        SyncManager.getInstance().stopSyncingProgressThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public void onBackPressed() {
        if (bindings.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            handler.post(() -> bindings.drawerLayout.closeDrawer(GravityCompat.START));
        } else {
            super.onBackPressed();
        }
    }
}