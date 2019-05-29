package io.digibyte.tools.list.items;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.BindingAdapter;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import io.digibyte.BR;
import io.digibyte.DigiByte;
import io.digibyte.R;
import io.digibyte.presenter.adapter.LayoutBinding;
import io.digibyte.presenter.entities.TxItem;
import io.digibyte.tools.database.AssetName;
import io.digibyte.tools.database.Database;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.util.BRCurrency;
import io.digibyte.tools.util.BRDateUtil;
import io.digibyte.tools.util.BRExchange;

public class ListItemTransactionData extends BaseObservable implements Parcelable, LayoutBinding {
    private int transactionIndex;
    private int transactionsCount;
    private String transactionDisplayTimeHolder;

    public TxItem transactionItem;

    public ListItemTransactionData(int anIndex, int aTransactionsCount, TxItem aTransactionItem) {
        this.transactionIndex = anIndex;
        this.transactionsCount = aTransactionsCount;
        this.transactionItem = aTransactionItem;
        this.transactionDisplayTimeHolder = BRDateUtil.getCustomSpan(new Date(transactionItem.getTimeStamp() * 1000));
    }

    public TxItem getTransactionItem() {
        return transactionItem;
    }

    public void updateAssetName() {
        notifyPropertyChanged(BR.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListItemTransactionData that = (ListItemTransactionData) o;

        return Objects.equals(transactionItem, that.transactionItem);
    }

    @Override
    public int hashCode() {
        return transactionItem != null ? transactionItem.hashCode() : 0;
    }

    public void update(ListItemTransactionData transactionItemData) {
        if (!transactionItemData.getTransactionItem().equals(this.transactionItem)) {
            this.transactionItem = transactionItemData.getTransactionItem();
            this.transactionDisplayTimeHolder = BRDateUtil.getCustomSpan(new Date(this.transactionItem.getTimeStamp() * 1000));
            notifyPropertyChanged(BR.arrowIcon);
            notifyPropertyChanged(BR.amount);
            notifyPropertyChanged(BR.timeStamp);
            notifyPropertyChanged(BR.textColor);
        }
    }

    protected ListItemTransactionData(Parcel in) {
        transactionIndex = in.readInt();
        transactionsCount = in.readInt();
        transactionItem = in.readParcelable(TxItem.class.getClassLoader());
        transactionDisplayTimeHolder = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(transactionIndex);
        dest.writeInt(transactionsCount);
        dest.writeParcelable(transactionItem, flags);
        dest.writeString(transactionDisplayTimeHolder);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ListItemTransactionData> CREATOR = new Parcelable.Creator<ListItemTransactionData>() {
        @Override
        public ListItemTransactionData createFromParcel(Parcel in) {
            return new ListItemTransactionData(in);
        }

        @Override
        public ListItemTransactionData[] newArray(int size) {
            return new ListItemTransactionData[size];
        }
    };

    @Override
    public int getLayoutId() {
        return R.layout.list_item_transaction;
    }

    @Bindable
    public int getArrowIcon() {
        boolean received = transactionItem.getSent() == 0;
        return received ? R.drawable.receive : R.drawable.send;
    }

    @Bindable
    public String getAmount() {
        if (transactionItem.isAsset) {
            AssetName assetName = Database.instance.findAssetNameFromHash(transactionItem.getTxHashHexReversed());
            if (assetName != null) {
                return assetName.getAssetName();
            } else {
                return DigiByte.getContext().getString(R.string.digi_asset);
            }
        } else {
            boolean isBTCPreferred = BRSharedPrefs.getPreferredBTC(DigiByte.getContext());
            boolean received = transactionItem.getSent() == 0;
            String iso = isBTCPreferred ? "DGB" : BRSharedPrefs.getIso(DigiByte.getContext());
            long satoshisAmount = received ? transactionItem.getReceived() : (transactionItem.getSent() - transactionItem.getReceived());
            String transactionText;
            if (isBTCPreferred) {
                transactionText = BRCurrency.getFormattedCurrencyString(DigiByte.getContext(), iso,
                        BRExchange.getAmountFromSatoshis(DigiByte.getContext(), iso,
                                new BigDecimal(satoshisAmount)));
            } else {
                transactionText = BRCurrency.getFormattedCurrencyString(DigiByte.getContext(), iso,
                        BRExchange.getAmountFromSatoshis(DigiByte.getContext(), iso,
                                new BigDecimal(satoshisAmount)));
            }
            return (received ? "+" : "-") + transactionText;
        }
    }

    @Bindable
    public int getTextColor() {
        boolean received = transactionItem.getSent() == 0;
        return received ? Color.parseColor("#3fe77b") : Color.parseColor("#ff7416");
    }

    @Bindable
    public String getTimeStamp() {
        Date timeStamp =
                new Date(transactionItem.getTimeStamp() == 0 ? System.currentTimeMillis()
                        : transactionItem.getTimeStamp() * 1000);
        Locale current = DigiByte.getContext().getResources().getConfiguration().locale;
        return DateFormat.getDateInstance(DateFormat.SHORT, current).format(timeStamp);
    }

    @BindingAdapter("drawable")
    public static void setDrawable(ImageView imageView, int drawableId) {
        imageView.setImageDrawable(imageView.getContext().getResources().getDrawable(drawableId));
    }
}