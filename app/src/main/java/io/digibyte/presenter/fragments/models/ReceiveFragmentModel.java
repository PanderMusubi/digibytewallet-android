package io.digibyte.presenter.fragments.models;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import io.digibyte.BR;
import io.digibyte.R;
import io.digibyte.presenter.activities.util.ActivityUTILS;

public class ReceiveFragmentModel extends BaseObservable {

    private boolean shareVisibility;
    private boolean requestButtonVisibility;
    private String title;
    private String address;
    private boolean showKeyboard;
    private String amount;
    private String isoText;
    private String isoButtonText;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Bindable
    public boolean isShareVisibility() {
        return shareVisibility;
    }

    public void setShareVisibility(boolean shareVisibility) {
        this.shareVisibility = shareVisibility;
        notifyPropertyChanged(BR.shareVisibility);
        if (shareVisibility) {
            handler.postDelayed(() -> {
                showKeyboard = false;
                notifyPropertyChanged(BR.showKeyboard);
            }, 250);
        }
    }

    @Bindable
    public boolean isRequestButtonVisibility() {
        return requestButtonVisibility;
    }

    public void setRequestButtonVisibility(boolean requestButtonVisibility) {
        this.requestButtonVisibility = requestButtonVisibility;
        notifyPropertyChanged(BR.requestButtonVisibility);
    }

    @Bindable
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        notifyPropertyChanged(BR.title);
    }

    @Bindable
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        notifyPropertyChanged(BR.address);
    }

    @Bindable
    public boolean isShowKeyboard() {
        return showKeyboard;
    }

    public void setShowKeyboard(boolean showKeyboard) {
        this.showKeyboard = showKeyboard;
        notifyPropertyChanged(BR.showKeyboard);
        if (showKeyboard) {
            handler.postDelayed(() -> {
                shareVisibility = false;
                notifyPropertyChanged(BR.shareVisibility);
            }, 250);
        }
    }

    @Bindable
    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
        notifyPropertyChanged(BR.displayAmount);
    }

    @Bindable
    public String getDisplayAmount() {
        if (TextUtils.isEmpty(amount)) {
            return "";
        }
        return amount.replace(".",
                Character.toString(ActivityUTILS.getDecimalSeparator())).replace("'", "");
    }

    @Bindable
    public String getIsoText() {
        return isoText;
    }

    public void setIsoText(String isoText) {
        this.isoText = isoText;
        notifyPropertyChanged(BR.isoText);
    }

    @Bindable
    public String getIsoButtonText() {
        return isoButtonText;
    }

    public void setIsoButtonText(String isoButtonText) {
        this.isoButtonText = isoButtonText;
        notifyPropertyChanged(BR.isoButtonText);
    }

    @Bindable
    public int getBRButtonBackgroundRedId() {
        return R.drawable.keyboard_white_button;
    }

    @Bindable
    public int getBRKeyboardColor() {
        return R.color.white;
    }
}