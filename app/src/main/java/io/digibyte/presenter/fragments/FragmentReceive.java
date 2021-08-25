package io.digibyte.presenter.fragments;

import android.animation.ObjectAnimator;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import io.digibyte.DigiByte;
import io.digibyte.R;
import io.digibyte.databinding.FragmentReceiveBinding;
import io.digibyte.presenter.activities.QRCodeActivity;
import io.digibyte.presenter.fragments.interfaces.FragmentReceiveCallbacks;
import io.digibyte.presenter.fragments.interfaces.OnBackPressListener;
import io.digibyte.presenter.fragments.models.ReceiveFragmentModel;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.manager.BRClipboardManager;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.qrcode.QRUtils;
import io.digibyte.tools.util.Utils;
import io.digibyte.wallet.BRWalletManager;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
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

public class FragmentReceive extends Fragment implements OnBackPressListener {

    protected static final String IS_RECEIVE = "FragmentReceive:IsReceive";
    protected FragmentReceiveBinding fragmentReceiveBinding;
    protected String address;
    protected String qrUrl;

    protected ReceiveFragmentModel receiveFragmentModel = new ReceiveFragmentModel();

    private FragmentReceiveCallbacks callbacks = new FragmentReceiveCallbacks() {

        @Override
        public void shareEmailClick() {
            if (onShareEmail()) {
                //This allows for extending fragments to override thd function of this callback
                return;
            }
            String bitcoinUri = Utils.createBitcoinUrl(address, 0, null, null, null);
            Uri qrImageUri = QRUtils.getQRImageUri(getContext(), bitcoinUri);
            QRUtils.share("mailto:", getActivity(), qrImageUri, null, null);
        }

        @Override
        public void shareTextClick() {
            if (onShareSMS()) {
                //This allows for extending fragments to override thd function of this callback
                return;
            }
            try {
                QRUtils.share("sms:", getActivity(), null, address, "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void shareButtonClick() {
            receiveFragmentModel.setShareVisibility(true);
        }

        @Override
        public void shareCopyClick() {
            if (onShareCopy()) {
                return;
            }
            BRClipboardManager.putClipboard(getContext(), address);
            Toast.makeText(getContext(), R.string.Receive_copied, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void addressClick() {
            copyText();
        }

        @Override
        public void requestButtonClick() {
            fadeOutRemove(true);
        }

        @Override
        public void backgroundClick() {
            fadeOutRemove(false);
        }

        @Override
        public void qrImageClick() {
            QRCodeActivity.show((AppCompatActivity) getActivity(), fragmentReceiveBinding.qrImage, qrUrl);
        }

        @Override
        public void closeClick() {
            fadeOutRemove(false);
        }

        @Override
        public void onAmountEditClick() {
            FragmentReceive.this.onAmountEditClick();
            //Not used in this implementation
        }

        @Override
        public void onClick(String key) {
            onKeyboardClick(key);
            //Not used in this implementation
        }

        @Override
        public void onIsoButtonClick() {
            FragmentReceive.this.onIsoButtonClick();
            //Not used in this implementation
        }
    };

    protected void onAmountEditClick() {
    }

    protected void onKeyboardClick(String key) {
    }

    protected void onIsoButtonClick() {
    }

    protected boolean onShareEmail() {
        return false;
    }

    protected boolean onShareSMS() {
        return false;
    }

    protected boolean onShareCopy() {
        return false;
    }

    public static void show(AppCompatActivity activity, boolean isReceive) {
        FragmentReceive fragmentReceive = new FragmentReceive();
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_RECEIVE, isReceive);
        fragmentReceive.setArguments(bundle);
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom,
                R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fragmentReceive,
                FragmentReceive.class.getName());
        transaction.addToBackStack(FragmentReceive.class.getName());
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BRWalletManager.refreshAddress(getContext());
        address = BRSharedPrefs.getReceiveAddress(DigiByte.getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentReceiveBinding = FragmentReceiveBinding.inflate(inflater);
        fragmentReceiveBinding.setCallback(callbacks);
        fragmentReceiveBinding.setData(receiveFragmentModel);
        fragmentReceiveBinding.addressText.setSelected(true);
        receiveFragmentModel.setAddress(address);
        setTitle();
        return fragmentReceiveBinding.getRoot();
    }

    protected void setTitle() {
        receiveFragmentModel.setTitle(getString(R.string.Receive_title));

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateQRImage();
    }

    private boolean getIsReceive() {
        if (getArguments() == null) {
            return false;
        }
        return getArguments().getBoolean(IS_RECEIVE, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (allowRequestAmountButtonShow()) {
            receiveFragmentModel.setRequestButtonVisibility(getIsReceive());
        }
        if (!getIsReceive()) {
            receiveFragmentModel.setTitle(getString(R.string.UnlockScreen_myAddress));
        }
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(fragmentReceiveBinding.backgroundLayout, false, null);
        colorFade.setStartDelay(350);
        colorFade.setDuration(500);
        colorFade.start();
    }

    protected void updateQRImage() {
        qrUrl = "digibyte:" + address;
        QRUtils.generateQR(getContext(), qrUrl, fragmentReceiveBinding.qrImage);
    }

    protected boolean allowRequestAmountButtonShow() {
        return true;
    }

    private void copyText() {
        BRClipboardManager.putClipboard(getContext(), address);
        Toast.makeText(getContext(), R.string.Receive_copied, Toast.LENGTH_SHORT).show();
    }

    protected void fadeOutRemove(boolean showRequestAmountPopup) {
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(fragmentReceiveBinding.backgroundLayout, true,
                () -> {
                    if (showRequestAmountPopup) {
                        BRAnimator.showRequestFragment((AppCompatActivity) getActivity());
                    } else {
                        remove();
                    }
                });
        colorFade.start();
    }

    private void remove() {
        if (getFragmentManager() == null) {
            return;
        }
        try {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        fadeOutRemove(false);
    }
}