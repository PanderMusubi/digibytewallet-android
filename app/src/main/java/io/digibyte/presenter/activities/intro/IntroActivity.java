
package io.digibyte.presenter.activities.intro;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;

import io.digibyte.R;
import io.digibyte.databinding.ActivityIntroBinding;
import io.digibyte.presenter.activities.UpdatePinActivity;
import io.digibyte.presenter.activities.callbacks.IntroActivityCallback;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.security.BRKeyStore;
import io.digibyte.tools.security.SmartValidator;
import io.digibyte.wallet.BRWalletManager;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
 * Copyright (c) 2016 breadwallet LLC
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

public class IntroActivity extends BRActivity implements Serializable {

    private IntroActivityCallback callback = new IntroActivityCallback() {
        @Override
        public void onNewWalletClick() {
            UpdatePinActivity.open(IntroActivity.this, UpdatePinActivity.Mode.SET_PIN);
        }

        @Override
        public void onRestoreClick() {
            Intent intent = new Intent(IntroActivity.this, RecoverActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        }

        @Override
        public void onNewDigiIDClick() {
            BRSharedPrefs.setDigiIDFocus(IntroActivity.this);
            UpdatePinActivity.open(IntroActivity.this, UpdatePinActivity.Mode.SET_PIN);
        }
    };

    public static void open(AppCompatActivity activity) {
        Intent intent = new Intent(activity, IntroActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityIntroBinding binding = DataBindingUtil.setContentView(this,
                R.layout.activity_intro);
        binding.setCallback(callback);
        cleanup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BRWalletManager.getInstance().showLockscreenRequiredDialog(this);
    }

    private void cleanup() {
        byte[] masterPubKey = BRKeyStore.getMasterPublicKey(this);
        boolean isFirstAddressCorrect = false;
        if (masterPubKey != null && masterPubKey.length != 0) {
            isFirstAddressCorrect = SmartValidator.checkFirstAddress(this, masterPubKey);
        }
        if (!isFirstAddressCorrect) {
            BRWalletManager.getInstance().wipeWalletButKeystore(this);
        }
    }
}