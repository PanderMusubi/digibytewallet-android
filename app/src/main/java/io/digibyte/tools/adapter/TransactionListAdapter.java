package io.digibyte.tools.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.digibyte.DigiByte;
import io.digibyte.databinding.ListItemTransactionBinding;
import io.digibyte.presenter.entities.TxItem;
import io.digibyte.presenter.fragments.models.TransactionDetailsViewModel;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.database.Database;
import io.digibyte.tools.list.ListItemData;
import io.digibyte.tools.list.items.ListItemTransactionData;
import io.digibyte.tools.list.items.ListItemTransactionViewHolder;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.wallet.BRWalletManager;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/27/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class TransactionListAdapter extends RecyclerView.Adapter<ListItemTransactionViewHolder> {
    public static final String TAG = TransactionListAdapter.class.getName();

    private ArrayList<ListItemTransactionData> listItemData = new ArrayList<>();
    private Set<ListItemTransactionViewHolder> boundViewHolders = new HashSet<>();

    private RecyclerView recyclerView;

    public TransactionListAdapter(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        setHasStableIds(true);
    }

    public void updateTransactions(ArrayList<ListItemTransactionData> transactions) {
        for (ListItemTransactionData listItemTransactionData : listItemData) {
            if (listItemTransactionData == null) {
                continue;
            }
            //Check to see if the comment/memo changed
            String currentComment = listItemTransactionData.transactionItem.metaData != null
                    ? listItemTransactionData.transactionItem.metaData.comment : "";
            //Check to see if the transaction time changed
            String currentTime = listItemTransactionData.getTransactionDisplayTimeHolder();

            ListItemTransactionData updatedTransaction = findTransaction(listItemTransactionData,
                    transactions);

            if (updatedTransaction == null) {
                continue;
            }

            listItemTransactionData.update(updatedTransaction);
            String newComment = listItemTransactionData.transactionItem.metaData != null
                    ? listItemTransactionData.transactionItem.metaData.comment : "";
            String newTime = listItemTransactionData.getTransactionDisplayTimeHolder();

            boolean commentUpdated = !currentComment.equals(newComment);
            boolean timeChange = !currentTime.equals(newTime);

            int confirms = BRSharedPrefs.getLastBlockHeight(DigiByte.getContext())
                    - listItemTransactionData.getTransactionItem().getBlockHeight() + 1;
            if ((confirms <= 8 || commentUpdated || timeChange) && isPositionOnscreen(
                    listItemData.indexOf(listItemTransactionData))) {
                BRWalletManager.getInstance().refreshBalance(DigiByte.getContext());
                ListItemTransactionViewHolder listItemTransactionViewHolder =
                        (ListItemTransactionViewHolder) recyclerView
                                .findViewHolderForAdapterPosition(
                                        listItemData.indexOf(listItemTransactionData));
                if (listItemTransactionViewHolder != null) {
                    listItemTransactionViewHolder.process(listItemTransactionData);
                }
            }
        }
    }

    public void notifyDataChanged() {
        for (ListItemTransactionViewHolder listItemTransactionViewHolder : boundViewHolders) {
            if (listItemTransactionViewHolder != null) {
                listItemTransactionViewHolder.
                        process(listItemData.get(listItemTransactionViewHolder.getAdapterPosition()));
            }
        }
    }

    /**
     * @return while the method implementation can return null, in reality it will not because
     * the adapter will always have a view holder for view that are currently on screen,
     * and this method is only called after a check to ensure the view is onscreen
     */

    @Nullable
    private ListItemTransactionData findTransaction(ListItemTransactionData listItemTransactionData,
                                                    ArrayList<ListItemTransactionData> transactions) {
        for (ListItemTransactionData checkTransaction : transactions) {
            if (checkTransaction.equals(listItemTransactionData)) {
                return checkTransaction;
            }
        }
        return null;
    }

    private boolean isPositionOnscreen(int position) {
        LinearLayoutManager linearLayoutManager =
                (LinearLayoutManager) recyclerView.getLayoutManager();
        int firstVisiblePosition = linearLayoutManager.findFirstVisibleItemPosition();
        int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
        return position >= firstVisiblePosition && position <= lastVisiblePosition;
    }

    public void addTransactions(ArrayList<ListItemTransactionData> transactions) {
        for (ListItemTransactionData transaction : transactions) {
            saveFiatValue(transaction.transactionItem);
            listItemData.add(0, transaction);
            notifyItemInserted(0);
        }
    }

    public ArrayList<ListItemTransactionData> getTransactions() {
        return listItemData;
    }

    @Override
    public ListItemTransactionViewHolder onCreateViewHolder(ViewGroup aParent, int aResourceId) {
        LayoutInflater layoutInflater = LayoutInflater.from(aParent.getContext());
        ListItemTransactionBinding binding = ListItemTransactionBinding.inflate(layoutInflater);
        return new ListItemTransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ListItemTransactionViewHolder holder, int aPosition) {
        holder.process(this.getListItemDataForPosition(aPosition));
        holder.getView().setOnClickListener(mOnClickListener);
        boundViewHolders.add(holder);
    }

    @Override
    public void onViewRecycled(@NonNull ListItemTransactionViewHolder holder) {
        boundViewHolders.remove(holder);
        super.onViewRecycled(holder);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ListItemTransactionViewHolder listItemTransactionViewHolder =
                    (ListItemTransactionViewHolder) recyclerView.findContainingViewHolder(view);
            int adapterPosition = listItemTransactionViewHolder.getAdapterPosition();
            BRAnimator.showTransactionPager((AppCompatActivity) view.getContext(),
                    listItemData, adapterPosition);
        }
    };

    @Override
    public int getItemViewType(int aPosition) {
        return this.getListItemDataForPosition(aPosition).resourceId;
    }

    @Override
    public int getItemCount() {
        return listItemData.size();
    }

    private ListItemData getListItemDataForPosition(int aPosition) {
        return listItemData.get(aPosition);
    }

    @Override
    public long getItemId(int position) {
        return Integer.valueOf(listItemData.get(
                position).getTransactionItem().hashCode()).longValue();
    }

    private void saveFiatValue(TxItem txItem) {
        //Save the transaction text in fiat mode, the first time the transaction is displayed in the app
        if (!Database.instance.containsTransaction(txItem.getTxHash())) {
            Database.instance.saveTransaction(txItem.getTxHash(), TransactionDetailsViewModel.getRawFiatAmount(txItem));
        }

    }
}