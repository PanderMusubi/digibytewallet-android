package io.digibyte.tools.security;

import android.app.Activity;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import java.security.InvalidKeyException;
import java.util.Arrays;

import io.digibyte.presenter.activities.PaperKeyActivity;
import io.digibyte.presenter.activities.PaperKeyProveActivity;
import io.digibyte.presenter.activities.UpdatePinActivity;
import io.digibyte.presenter.activities.intro.WriteDownActivity;
import io.digibyte.presenter.entities.PaymentItem;
import io.digibyte.presenter.entities.PaymentRequestWrapper;
import io.digibyte.tools.manager.BRReportsManager;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.util.BRConstants;
import io.digibyte.tools.util.TypesConverter;
import io.digibyte.wallet.BRWalletManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 4/14/16.
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

public class PostAuth {
    public static final String TAG = PostAuth.class.getName();

    private String phraseForKeyStore;
    private PaymentRequestWrapper paymentRequest;
    public static boolean isStuckWithAuthLoop;

    public static PostAuth instance = new PostAuth();

    private PostAuth() {
    }

    public void onCreateWalletAuth(AppCompatActivity app, boolean authAsked) {
        boolean success = BRWalletManager.getInstance().generateRandomSeed(app);
        if (success) {
            WriteDownActivity.open(app);
        } else {
            if (authAsked) {
                Log.e(TAG, new Object() {
                }.getClass().getEnclosingMethod().getName() + ": WARNING!!!! LOOP");
                isStuckWithAuthLoop = true;
            }
            return;
        }
    }

    public void onPhraseCheckAuth(AppCompatActivity app, boolean authAsked) {
        try {
            byte[] raw = BRKeyStore.getPhrase(app, BRConstants.SHOW_PHRASE_REQUEST_CODE);
            PaperKeyActivity.show(app, new String(raw));
        } catch (InvalidKeyException e) {
            if (authAsked) {
                Log.e(TAG, new Object() {
                }.getClass().getEnclosingMethod().getName() + ": WARNING!!!! LOOP");
                isStuckWithAuthLoop = true;
            }
            return;
        }
    }

    public void onPhraseProveAuth(AppCompatActivity app, boolean authAsked) {
        String cleanPhrase;
        try {
            cleanPhrase = new String(BRKeyStore.getPhrase(app, BRConstants.PROVE_PHRASE_REQUEST));
        } catch (InvalidKeyException e) {
            if (authAsked) {
                Log.e(TAG, new Object() {
                }.getClass().getEnclosingMethod().getName() + ": WARNING!!!! LOOP");
                isStuckWithAuthLoop = true;
            }
            return;
        }
        PaperKeyProveActivity.show(app, cleanPhrase);
    }

    public void onRecoverWalletAuth(AppCompatActivity app, boolean authAsked) {
        if (phraseForKeyStore == null) {
            Log.e(TAG, "onRecoverWalletAuth: phraseForKeyStore is null!");
            BRReportsManager.reportBug(new NullPointerException("onRecoverWalletAuth: phraseForKeyStore is null"));
            return;
        }
        byte[] bytePhrase = new byte[0];

        try {
            boolean success;
            try {
                success = BRKeyStore.putPhrase(phraseForKeyStore.getBytes(),
                        app, BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE);
            } catch (InvalidKeyException e) {
                if (authAsked) {
                    Log.e(TAG, new Object() {
                    }.getClass().getEnclosingMethod().getName() + ": WARNING!!!! LOOP");
                    isStuckWithAuthLoop = true;

                }
                return;
            }

            if (!success) {
                if (authAsked) {
                    Log.e(TAG, "onRecoverWalletAuth,!success && authAsked");
                }
            } else {
                if (phraseForKeyStore.length() != 0) {
                    BRSharedPrefs.putPhraseWroteDown(app, true);
                    bytePhrase = TypesConverter.getNullTerminatedPhrase(phraseForKeyStore.getBytes());
                    byte[] seed = BRWalletManager.getSeedFromPhrase(bytePhrase);
                    byte[] authKey = BRWalletManager.getAuthPrivKeyForAPI(seed);
                    BRKeyStore.putAuthKey(authKey, app);
                    byte[] pubKey = BRWalletManager.getInstance().getMasterPubKey(bytePhrase);
                    BRKeyStore.putMasterPublicKey(pubKey, app);
                    UpdatePinActivity.open(app, UpdatePinActivity.Mode.ENTER_NEW_PIN);
                    phraseForKeyStore = null;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            BRReportsManager.reportBug(e);
        } finally {
            Arrays.fill(bytePhrase, (byte) 0);
        }
    }

    public void onPublishTxAuth(final Context app, PaymentItem paymentItem) {
        try {
            final BRWalletManager walletManager = BRWalletManager.getInstance();
            byte[] rawSeed = BRKeyStore.getPhrase(app, BRConstants.PAY_REQUEST_CODE);
            byte[] seed = TypesConverter.getNullTerminatedPhrase(rawSeed);
            byte[] txHash = walletManager.publishSerializedTransaction(paymentItem.serializedTx, seed);
            Log.e(TAG, "onPublishTxAuth: txhash:" + Arrays.toString(txHash));
            TxMetaData txMetaData = new TxMetaData();
            txMetaData.comment = paymentItem.comment;
            KVStoreManager.getInstance().putTxMetaData(app, txMetaData, txHash);
            Arrays.fill(seed, (byte) 0);
            seed = null;
        } catch (Exception e) {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.recordException(e);
        }
    }

    public void setPhraseForKeyStore(String phraseForKeyStore) {
        this.phraseForKeyStore = phraseForKeyStore;
    }

    /**
     * This is legacy Bread code that looks to be designed to check for data storage tampering
     *
     * @param app
     * @param authAsked
     */
    public void onCanaryCheck(final Activity app, boolean authAsked) {
        String canary = null;
        try {
            canary = BRKeyStore.getCanary(app, BRConstants.CANARY_REQUEST_CODE);
        } catch (InvalidKeyException e) {
            if (authAsked) {
                Log.e(TAG, new Object() {
                }.getClass().getEnclosingMethod().getName() + ": WARNING!!!! LOOP");
                isStuckWithAuthLoop = true;
            }
            return;
        }
        if (canary == null || !canary.equalsIgnoreCase(BRConstants.CANARY_STRING)) {
            byte[] phrase;
            try {
                phrase = BRKeyStore.getPhrase(app, BRConstants.CANARY_REQUEST_CODE);
            } catch (InvalidKeyException e) {
                if (authAsked) {
                    Log.e(TAG, new Object() {
                    }.getClass().getEnclosingMethod().getName() + ": WARNING!!!! LOOP");
                    isStuckWithAuthLoop = true;
                }
                return;
            }

            String strPhrase = new String((phrase == null) ? new byte[0] : phrase);
            if (strPhrase.isEmpty()) {
                BRWalletManager m = BRWalletManager.getInstance();
                m.wipeKeyStore(app);
                m.wipeWalletButKeystore(app);
            } else {
                Log.e(TAG, "onCanaryCheck: Canary wasn't there, but the phrase persists, adding canary to keystore.");
                try {
                    BRKeyStore.putCanary(BRConstants.CANARY_STRING, app, 0);
                } catch (InvalidKeyException e) {
                    if (authAsked) {
                        Log.e(TAG, new Object() {
                        }.getClass().getEnclosingMethod().getName() + ": WARNING!!!! LOOP");
                        isStuckWithAuthLoop = true;
                    }
                    return;
                }
            }
        }
    }
}
