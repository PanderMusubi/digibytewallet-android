package io.digibyte.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.platform.entities.WalletInfo;
import com.platform.tools.KVStoreManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import io.digibyte.BuildConfig;
import io.digibyte.DigiByte;
import io.digibyte.R;
import io.digibyte.presenter.activities.BreadActivity;
import io.digibyte.presenter.customviews.BRToast;
import io.digibyte.presenter.entities.BRMerkleBlockEntity;
import io.digibyte.presenter.entities.BRPeerEntity;
import io.digibyte.presenter.entities.BRTransactionEntity;
import io.digibyte.presenter.entities.ImportPrivKeyEntity;
import io.digibyte.presenter.entities.TxItem;
import io.digibyte.tools.animation.BRDialog;
import io.digibyte.tools.animation.SpringAnimator;
import io.digibyte.tools.manager.BRApiManager;
import io.digibyte.tools.manager.BRNotificationManager;
import io.digibyte.tools.manager.BRReportsManager;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.security.BRKeyStore;
import io.digibyte.tools.sqlite.MerkleBlockDataSource;
import io.digibyte.tools.sqlite.PeerDataSource;
import io.digibyte.tools.sqlite.TransactionDataSource;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.tools.threads.ImportPrivKeyTask;
import io.digibyte.tools.util.BRConstants;
import io.digibyte.tools.util.BRCurrency;
import io.digibyte.tools.util.BRExchange;
import io.digibyte.tools.util.Bip39Reader;
import io.digibyte.tools.util.TypesConverter;
import io.digibyte.tools.util.Utils;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 12/10/15.
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

public class BRWalletManager {
    private static final String TAG = BRWalletManager.class.getName();
    private static String DIGIEXPLORER_URL = "https://digiexplorer.info";
    private static final String DIGIEXPLORER_URL_FALLBACK = "https://explorer-2.us.digibyteservers.io";
    private static final String DIGIEXPLORER_URL_FALLBACK_2 = "https://digiexplorer.info/";

    private static BRWalletManager instance;
    public List<OnBalanceChanged> balanceListeners;
    private boolean initingWallet = false;
    private boolean initingPeerManager = false;

    public void setBalance(final Context context, long balance) {
        BRSharedPrefs.putCatchedBalance(context, balance);
        refreshAddress(context);
        for (OnBalanceChanged listener : balanceListeners) {
            if (listener != null) {
                listener.onBalanceChanged(balance);
            }
        }
    }

    private void showSendConfirmDialog(final String message, final int error, byte[] txHash) {
        for (OnBalanceChanged listener : balanceListeners) {
            if (listener != null) {
                listener.showSendConfirmDialog(message, error, txHash);
            }
        }
    }

    public void refreshBalance(Context app) {
        long nativeBalance = nativeBalance();
        if (nativeBalance != -1) {
            setBalance(app, nativeBalance);
        } else {
            Log.e(TAG, "UpdateUI, nativeBalance is -1 meaning _wallet was null!");
        }
    }

    public long getBalance(Context context) {
        return BRSharedPrefs.getCatchedBalance(context);
    }

    private BRWalletManager() {
        balanceListeners = new ArrayList<>();
    }

    public static BRWalletManager getInstance() {
        if (instance == null) {
            instance = new BRWalletManager();
        }
        return instance;
    }

    public synchronized boolean generateRandomSeed(final Context ctx) {
        SecureRandom sr = new SecureRandom();
        final String[] words;
        List<String> list;
        String languageCode = Locale.getDefault().getLanguage();
        if (languageCode == null) {
            languageCode = "en";
        }
        list = Bip39Reader.bip39List(ctx, languageCode);
        words = list.toArray(new String[list.size()]);
        final byte[] randomSeed = sr.generateSeed(16);
        if (words.length != 2048) {
            BRReportsManager.reportBug(
                    new IllegalArgumentException("the list is wrong, size: " + words.length), true);
            return false;
        }
        if (randomSeed.length != 16) {
            throw new NullPointerException(
                    "failed to create the seed, seed length is not 128: " + randomSeed.length);
        }
        byte[] strPhrase = encodeSeed(randomSeed, words);
        if (strPhrase == null || strPhrase.length == 0) {
            BRReportsManager.reportBug(new NullPointerException("failed to encodeSeed"), true);
            return false;
        }
        String[] splitPhrase = new String(strPhrase).split(" ");
        if (splitPhrase.length != 12) {
            BRReportsManager.reportBug(new NullPointerException(
                    "phrase does not have 12 words:" + splitPhrase.length + ", lang: "
                            + languageCode), true);
            return false;
        }
        boolean success;
        try {
            success = BRKeyStore.putPhrase(strPhrase, ctx,
                    BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
        } catch (InvalidKeyException e) {
            return false;
        }
        if (!success) {
            return false;
        }
        byte[] phrase;
        try {
            phrase = BRKeyStore.getPhrase(ctx, 0);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(
                    "Failed to retrieve the phrase even though at this point the system auth was "
                            + "asked for sure.");
        }
        if (Utils.isNullOrEmpty(phrase)) {
            throw new NullPointerException("phrase is null!!");
        }
        byte[] nulTermPhrase = TypesConverter.getNullTerminatedPhrase(phrase);
        if (nulTermPhrase == null || nulTermPhrase.length == 0) {
            throw new RuntimeException("nulTermPhrase is null");
        }
        byte[] seed = getSeedFromPhrase(nulTermPhrase);
        if (seed == null || seed.length == 0) {
            throw new RuntimeException("seed is null");
        }
        byte[] authKey = getAuthPrivKeyForAPI(seed);
        if (authKey == null || authKey.length == 0) {
            BRReportsManager.reportBug(new IllegalArgumentException("authKey is invalid"), true);
        }
        BRKeyStore.putAuthKey(authKey, ctx);
        int walletCreationTime = (int) (System.currentTimeMillis() / 1000);
        BRKeyStore.putWalletCreationTime(walletCreationTime, ctx);
        final WalletInfo info = new WalletInfo();
        info.creationDate = walletCreationTime;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            KVStoreManager.getInstance().putWalletInfo(ctx,
                    info); //push the creation time to the kv store
        });

        byte[] strBytes = TypesConverter.getNullTerminatedPhrase(strPhrase);
        byte[] pubKey = BRWalletManager.getInstance().getMasterPubKey(strBytes);
        BRKeyStore.putMasterPublicKey(pubKey, ctx);

        return true;

    }

    public boolean wipeKeyStore(Context context) {
        Log.d(TAG, "wipeKeyStore");
        return BRKeyStore.resetWalletKeyStore(context);
    }

    /**
     * true if keystore is available and we know that no wallet exists on it
     */
    public boolean noWallet(Context ctx) {
        byte[] pubkey = BRKeyStore.getMasterPublicKey(ctx);

        if (pubkey == null || pubkey.length == 0) {
            byte[] phrase;
            try {
                phrase = BRKeyStore.getPhrase(ctx, 0);
                //if not authenticated, an error will be thrown and returned false, so no worry
                // about mistakenly removing the wallet
                if (phrase == null || phrase.length == 0) {
                    return true;
                }
            } catch (InvalidKeyException e) {
                return false;
            }

        }
        return false;
    }

    /**
     * true if device passcode is enabled
     */
    public boolean isPasscodeEnabled(Context ctx) {
        KeyguardManager keyguardManager = (KeyguardManager) ctx.getSystemService(
                Activity.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    public boolean isNetworkAvailable(Context ctx) {
        if (ctx == null) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();

    }

    public static boolean refreshAddress(Context ctx) {
        String address = getReceiveAddress();
        if (Utils.isNullOrEmpty(address)) {
            Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
            return false;
        }
        BRSharedPrefs.putReceiveAddress(ctx, address);
        return true;

    }

    public void wipeWalletButKeystore(final Context ctx) {
        Log.d(TAG, "wipeWalletButKeystore");
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            BRPeerManager.getInstance().peerManagerFreeEverything();
            walletFreeEverything();
            TransactionDataSource.getInstance(ctx).deleteAllTransactions();
            MerkleBlockDataSource.getInstance(ctx).deleteAllBlocks();
            PeerDataSource.getInstance(ctx).deleteAllPeers();
            BRSharedPrefs.clearAllPrefs(ctx);
        });

    }

    public void wipeBlockAndTrans(Context ctx, ClearedListener clearedListener) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            BRPeerManager.getInstance().peerManagerFreeEverything();
            walletFreeEverything();
            TransactionDataSource.getInstance(ctx).deleteAllTransactions();
            MerkleBlockDataSource.getInstance(ctx).deleteAllBlocks();
            PeerDataSource.getInstance(ctx).deleteAllPeers();
            BRSharedPrefs.putStartHeight(ctx, 0);
            BRSharedPrefs.putAllowSpend(ctx, false);
            new Handler(Looper.getMainLooper()).post(() -> clearedListener.onCleared());
        });
    }

    public interface ClearedListener {
        void onCleared();
    }

    public boolean confirmSweep(final Context ctx, final String privKey) {
        if (ctx == null) {
            return false;
        }
        if (isValidBitcoinBIP38Key(privKey)) {
            Log.d(TAG, "isValidBitcoinBIP38Key true");
            ((Activity) ctx).runOnUiThread(() -> {

                final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

                final View input = ((Activity) ctx).getLayoutInflater().inflate(
                        R.layout.view_bip38password_dialog, null);
                // Specify the type of input expected; this, for example, sets the input as a
                // password, and will mask the text
                builder.setView(input);

                final EditText editText = input.findViewById(
                        R.id.bip38password_edittext);

                (new Handler()).postDelayed(() -> {
                    editText.dispatchTouchEvent(
                            MotionEvent.obtain(SystemClock.uptimeMillis(),
                                    SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0,
                                    0, 0));
                    editText.dispatchTouchEvent(
                            MotionEvent.obtain(SystemClock.uptimeMillis(),
                                    SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0,
                                    0));

                }, 100);

                // Set up the buttons
                builder.setPositiveButton(ctx.getString(R.string.Button_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (ctx != null) {
                                    ((Activity) ctx).runOnUiThread(
                                            () -> BRToast.showCustomToast(ctx,
                                                    ctx.getString(R.string.Import_checking),
                                                    500, Toast.LENGTH_LONG,
                                                    R.drawable.toast_layout_blue));
                                }
                                if (editText == null) {
                                    Log.e(TAG, "onClick: edit text is null!");
                                    return;
                                }

                                final String pass = editText.getText().toString();
                                Log.e(TAG, "onClick: before");
                                BRExecutor.getInstance().forLightWeightBackgroundTasks()
                                        .execute(
                                                () -> {
                                                    String decryptedKey = decryptBip38Key(
                                                            privKey,
                                                            pass);
                                                    Log.e(TAG, "onClick: after");

                                                    if (decryptedKey.equals("")) {
                                                        SpringAnimator.springView(input);
                                                        confirmSweep(ctx, privKey);
                                                    } else {
                                                        confirmSweep(ctx, decryptedKey);
                                                    }
                                                });

                            }
                        });
                builder.setNegativeButton(ctx.getString(R.string.Button_cancel),
                        (dialog, which) -> dialog.cancel());

                builder.show();
            });
            return true;
        } else if (isValidBitcoinPrivateKey(privKey)) {
            Log.d(TAG, "isValidBitcoinPrivateKey true");
            new ImportPrivKeyTask(((Activity) ctx)).execute(privKey);
            return true;
        } else {
            Log.e(TAG, "confirmSweep: !isValidBitcoinPrivateKey && !isValidBitcoinBIP38Key");
            return false;
        }
    }


    /**
     * Wallet callbacks
     */
    public static void publishCallback(final String message, final int error, byte[] txHash) {
        Log.e(TAG,
                "publishCallback: " + message + ", err:" + error + ", txHash: " + Arrays.toString(
                        txHash));
        BRWalletManager.getInstance().showSendConfirmDialog(message, error, txHash);
    }

    public static void onBalanceChanged(final long balance) {
        Log.d(TAG, "onBalanceChanged:  " + balance);
        Context app = DigiByte.getContext();
        BRWalletManager.getInstance().setBalance(app, balance);

    }

    public static void onTxAdded(byte[] tx, int blockHeight, long timestamp, final long amount,
            String hash) {
        Log.d(TAG, "onTxAdded: " + String.format(
                "tx.length: %d, blockHeight: %d, timestamp: %d, amount: %d, hash: %s", tx.length,
                blockHeight, timestamp, amount, hash));

        final Context ctx = DigiByte.getContext();
        if (amount > 0) {
            BRExecutor.getInstance().forMainThreadTasks().execute(() -> {
                String am = BRCurrency.getFormattedCurrencyString(ctx, "DGB",
                        BRExchange.getBitcoinForSatoshis(ctx, new BigDecimal(amount)));
                String amCur = BRCurrency.getFormattedCurrencyString(ctx,
                        BRSharedPrefs.getIso(ctx),
                        BRExchange.getAmountFromSatoshis(ctx, BRSharedPrefs.getIso(ctx),
                                new BigDecimal(amount)));
                String formatted = String.format("%s (%s)", am, amCur);
                String strToShow = String.format(
                        ctx.getString(R.string.TransactionDetails_received), formatted);
                showToastWithMessage(ctx, strToShow);
            });
        }
        if (ctx != null) {
            TransactionDataSource.getInstance(ctx).putTransaction(
                    new BRTransactionEntity(tx, blockHeight, timestamp, hash));
        } else {
            Log.e(TAG, "onTxAdded: ctx is null!");
        }
    }

    private static void showToastWithMessage(Context ctx, final String message) {
        if (ctx == null) {
            ctx = DigiByte.getContext();
        }
        if (ctx != null) {
            final Context finalCtx = ctx;
            new Handler().postDelayed(() -> {
                if (!BRToast.isToastShown()) {
                    Point screenSize = Utils.getScreenSize(finalCtx);
                    BRToast.showCustomToast(finalCtx, message, screenSize.y / 2,
                            Toast.LENGTH_LONG, R.drawable.toast_layout_black);
                    AudioManager audioManager = (AudioManager) finalCtx.getSystemService(
                            Context.AUDIO_SERVICE);
                    if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                        final MediaPlayer mp = MediaPlayer.create(finalCtx, R.raw.coinflip);
                        if (mp != null) {
                            try {
                                mp.start();
                            } catch (IllegalArgumentException ex) {
                                Log.e(TAG, "run: ", ex);
                            }
                        }
                    }

                    // TODO: Double check if this should work via DigiByte.getContext()
                    // .isSuspended()
                    final Activity activity = DigiByte.getContext().getActivity();
                    if (null == activity || !(activity instanceof BreadActivity)
                            && BRSharedPrefs.getShowNotification(finalCtx)) {
                        BRNotificationManager.sendNotification(finalCtx,
                                R.drawable.notification_icon,
                                finalCtx.getString(R.string.app_name), message, 1);
                    }
                }
            }, 1000);


        } else {
            Log.e(TAG, "showToastWithMessage: failed, ctx is null");
        }
    }

    public static void onTxUpdated(String hash, int blockHeight, int timeStamp) {
        Log.d(TAG, "onTxUpdated: " + String.format("hash: %s, blockHeight: %d, timestamp: %d", hash,
                blockHeight, timeStamp));
        Context ctx = DigiByte.getContext();
        if (ctx != null) {
            TransactionDataSource.getInstance(ctx).updateTxBlockHeight(hash, blockHeight,
                    timeStamp);

        } else {
            Log.e(TAG, "onTxUpdated: Failed, ctx is null");
        }
    }

    public static void onTxDeleted(String hash, int notifyUser, final int recommendRescan) {
        Log.e(TAG, "onTxDeleted: " + String.format("hash: %s, notifyUser: %d, recommendRescan: %d",
                hash, notifyUser, recommendRescan));
        final Context ctx = DigiByte.getContext();
        if (ctx != null) {
            BRSharedPrefs.putScanRecommended(ctx, true);
        } else {
            Log.e(TAG, "onTxDeleted: Failed! ctx is null");
        }
    }


    public void showLockscreenRequiredDialog(final Activity app) {
        final BRWalletManager m = BRWalletManager.getInstance();
        if (!m.isPasscodeEnabled(app)) {
            //Device passcode/password should be enabled for the app to work
            BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                    app.getString(R.string.Prompts_NoScreenLock_body_android),
                    app.getString(R.string.AccessibilityLabels_close), null,
                    brDialogView -> {
                        try {
                            brDialogView.dismiss();
                        } catch (IllegalStateException e) {
                        }
                        Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                        app.startActivity(intent);
                    }, null, dialog -> app.finish(), 0);
        }
    }

    public void init() {
        BRExecutor.getInstance().forBackgroundTasks().execute(() -> {
            initWalletAndConnectPeers();
        });
    }

    private void initWalletAndConnectPeers() {
        try {
            Log.d(TAG, "initWallet:" + Thread.currentThread().getName());
            createBRWalletManager();
            createBRPeerManager();
            BRPeerManager.getInstance().connect();
        } catch (Throwable e) {
            e.printStackTrace();
            initingPeerManager = false;
            initingWallet = false;
            //TODO if the wallet fails to init, wtf to do?
        }
    }

    private void createBRWalletManager() {
        if (isCreated() || initingWallet) {
            return;
        }
        initingWallet = true;
        List<BRTransactionEntity> transactions = TransactionDataSource.getInstance(
                DigiByte.getContext()).getAllTransactions();
        int transactionsCount = transactions.size();
        if (transactionsCount > 0) {
            createTxArrayWithCount(transactionsCount);
            for (BRTransactionEntity entity : transactions) {
                putTransaction(entity.getBuff(), entity.getBlockheight(), entity.getTimestamp());
            }
        }

        byte[] pubkeyEncoded = BRKeyStore.getMasterPublicKey(DigiByte.getContext());
        if (Utils.isNullOrEmpty(pubkeyEncoded)) {
            Log.e(TAG, "initWallet: pubkey is missing");
            return;
        }
        //Save the first address for future check
        createWallet(transactionsCount, pubkeyEncoded);
        String firstAddress = getFirstAddress(pubkeyEncoded);
        BRSharedPrefs.putFirstAddress(DigiByte.getContext(), firstAddress);
        long fee = BRSharedPrefs.getFeePerKb(DigiByte.getContext());
        if (fee == 0) {
            fee = defaultFee();
        }
        setFeePerKb(fee, false);
        initingWallet = false;
    }

    private void createBRPeerManager() throws JSONException {
        if (BRPeerManager.getInstance().isCreated() || initingPeerManager) {
            return;
        }
        initingPeerManager = true;
        List<BRMerkleBlockEntity> blocks = MerkleBlockDataSource.getInstance(
                DigiByte.getContext()).getAllMerkleBlocks();
        List<BRPeerEntity> peers = PeerDataSource.getInstance(DigiByte.getContext()).getAllPeers();
        final int blocksCount = blocks.size();
        final int peersCount = peers.size();
        if (blocksCount > 0) {
            BRPeerManager.getInstance().createBlockArrayWithCount(blocksCount);
            for (BRMerkleBlockEntity entity : blocks) {
                BRPeerManager.getInstance().putBlock(entity.getBuff(), entity.getBlockHeight());
            }
        }
        if (peersCount > 0) {
            BRPeerManager.getInstance().createPeerArrayWithCount(peersCount);
            for (BRPeerEntity entity : peers) {
                BRPeerManager.getInstance().putPeer(entity.getAddress(), entity.getPort(),
                        entity.getTimeStamp());
            }
        }
        Log.d(TAG, "blocksCount before connecting: " + blocksCount);
        Log.d(TAG, "peersCount before connecting: " + peersCount);

        int walletTime = BRKeyStore.getWalletCreationTime(DigiByte.getContext());

        Log.e(TAG, "initWallet: walletTime: " + walletTime);

        //Are we beyond initial block sync? If we are proceed to the else clause
        // and sync as normal from the currently stored head block.
        // If there's no stored blocks query public keys for transactions
        // and if there's no transactions sync from the head of the DigiByte blockchain
        // otherwise sync from the oldest block associated with the transactions
        if (MerkleBlockDataSource.getInstance(DigiByte.getContext()).getAllMerkleBlocks().size() == 0) {
            pingToUpdateExplorerDomain();
            LinkedList<String> transactionsData = getAllTransactions();
            if (transactionsData == null) {
                //If null returns an exception occurred connecting to the block explorer
                //Init the peer manager using the standard checkpoint mechanic
                BRPeerManager.getInstance().create(walletTime, blocksCount, peersCount);
            } else if (transactionsData.size() == 0) {
                createPeerManagerFromCurrentHeadBlock(walletTime, blocksCount, peersCount);
            } else {
                createPeerManagerFromOldestBlock(transactionsData, walletTime, blocksCount,
                        peersCount);
            }
        } else {
            BRPeerManager.getInstance().create(walletTime, blocksCount, peersCount);
        }
        BRPeerManager.getInstance().updateFixedPeer(DigiByte.getContext());
        if (BRSharedPrefs.getStartHeight(DigiByte.getContext()) == 0) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(
                    () -> BRSharedPrefs.putStartHeight(DigiByte.getContext(),
                            BRPeerManager.getCurrentBlockHeight()));
        }
        initingPeerManager = false;
    }

    private void pingToUpdateExplorerDomain() {
        try {
            BRApiManager.getInstance().getBlockInfo(
                    DigiByte.getContext(), DIGIEXPLORER_URL);
        } catch(Exception e) {
            try {
                BRApiManager.getInstance().getBlockInfo(
                        DigiByte.getContext(), DIGIEXPLORER_URL_FALLBACK);
                DIGIEXPLORER_URL = DIGIEXPLORER_URL_FALLBACK;
            } catch (Exception ee) {
                try {
                    BRApiManager.getInstance().getBlockInfo(
                            DigiByte.getContext(), DIGIEXPLORER_URL_FALLBACK_2);
                    DIGIEXPLORER_URL = DIGIEXPLORER_URL_FALLBACK_2;
                } catch (Exception eee) {
                    DIGIEXPLORER_URL = "WTF??";
                }
            }
        }
    }

    private LinkedList<String> getAllTransactions() {
        String[] addresses = getPublicAddresses();
        LinkedList<String> transactions = new LinkedList<>();
        if (BuildConfig.DEBUG) {
            Log.d(BRWalletManager.class.getSimpleName(), Arrays.toString(addresses));
        }
        try {
            for (String address : addresses) {
                JSONObject transactionsData = new JSONObject(
                        BRApiManager.getInstance().getBlockInfo(
                                DigiByte.getContext(), DIGIEXPLORER_URL + "/api/addr/" + address));
                JSONArray transactionsJson = transactionsData.getJSONArray("transactions");
                for (int i = 0; i < transactionsJson.length(); i++) {
                    transactions.add(transactionsJson.getString(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
            //Returning 0 transactions should just use the last snapshot
            //An exception here likely means the server is down, or network connectivity isn't available
        }
        return transactions;
    }

    private void createPeerManagerFromCurrentHeadBlock(int walletTime, int blocksCount,
                                                       int peersCount) throws JSONException {
        JSONObject latestBlockData = getLatestBlockData();
        BRPeerManager.getInstance().createNew(walletTime, blocksCount, peersCount,
                latestBlockData.getString("hash"),
                latestBlockData.getInt("height"), latestBlockData.getLong("time"), 0);
    }

    public static JSONObject getLatestBlockData() throws JSONException {
        JSONObject latestBlockHashJson = new JSONObject(
                BRApiManager.getInstance().getBlockInfo(
                        DigiByte.getContext(),
                        DIGIEXPLORER_URL + "/api/status?q=getLastBlockHash"));
        String lastBlockHash = latestBlockHashJson.getString("lastblockhash");
        JSONObject latestBlockData = new JSONObject(BRApiManager.getInstance().getBlockInfo(
                DigiByte.getContext(),
                DIGIEXPLORER_URL + "/api/block/" + lastBlockHash));
        return latestBlockData;
    }

    private void createPeerManagerFromOldestBlock(LinkedList<String> transactions, int walletTime,
            int blocksCount, int peersCount) throws JSONException {
        String oldestBlockHash = "";
        Date oldestBlockTime = new Date(System.currentTimeMillis());
        for (String transaction : transactions) {
            String transactionData = BRApiManager.getInstance().getBlockInfo(
                    DigiByte.getContext(), DIGIEXPLORER_URL + "/api/tx/" + transaction);
            JSONObject transactionDataJson = new JSONObject(transactionData);
            Date blockTime = new Date(transactionDataJson.getLong("blocktime") * 1000L);
            if (blockTime.before(oldestBlockTime)) {
                oldestBlockTime = blockTime;
                oldestBlockHash = transactionDataJson.getString("blockhash");
            }
        }
        JSONObject blockJson = goBack5Blocks(oldestBlockHash);
        String blockHash = blockJson.getString("hash");
        int blockHeight = blockJson.getInt("height");
        long blockTime = blockJson.getLong("time");
        BRPeerManager.getInstance().createNew(walletTime, blocksCount, peersCount,
                blockHash, blockHeight, blockTime, 0);
    }

    private JSONObject goBack5Blocks(String oldestBlockHash) throws JSONException {
        JSONObject blockJson = new JSONObject(BRApiManager.getInstance().getBlockInfo(
                DigiByte.getContext(), DIGIEXPLORER_URL + "/api/block/" + oldestBlockHash));
        for (int i = 0; i < 5; i++) {
            oldestBlockHash = blockJson.getString("previousblockhash");
            blockJson = new JSONObject(BRApiManager.getInstance().getBlockInfo(
                    DigiByte.getContext(), DIGIEXPLORER_URL + "/api/block/" + oldestBlockHash));
        }
        return blockJson;
    }

    public void addBalanceChangedListener(OnBalanceChanged listener) {
        if (balanceListeners == null) {
            Log.e(TAG, "addBalanceChangedListener: statusUpdateListeners is null");
            return;
        }
        if (!balanceListeners.contains(listener)) {
            balanceListeners.add(listener);
        }
    }

    public void removeListener(OnBalanceChanged listener) {
        if (balanceListeners == null) {
            Log.e(TAG, "addBalanceChangedListener: statusUpdateListeners is null");
            return;
        }
        balanceListeners.remove(listener);

    }

    public interface OnBalanceChanged {
        void onBalanceChanged(long balance);

        void showSendConfirmDialog(final String message, final int error, byte[] txHash);
    }

    private native byte[] encodeSeed(byte[] seed, String[] wordList);

    public native void createWallet(int transactionCount, byte[] pubkey);

    public native void putTransaction(byte[] transaction, long blockHeight, long timeStamp);

    public native void createTxArrayWithCount(int count);

    public native byte[] getMasterPubKey(byte[] normalizedString);

    public static native String getReceiveAddress();

    public static native String[] getPublicAddresses();

    public native TxItem[] getTransactions();

    public static native boolean validateAddress(String address);

    public static native boolean addressContainedInWallet(String address);

    public native boolean addressIsUsed(String address);

    public native int feeForTransaction(String addressHolder, long amountHolder);

    public native int feeForTransactionAmount(long amountHolder);

    public native long getMinOutputAmount();

    public native long getMaxOutputAmount();

    public native boolean isCreated();

    public native byte[] tryTransaction(String addressHolder, long amountHolder);

    // returns the given amount (amount is in satoshis) in local currency units (i.e. pennies,
    // pence)
    // price is local currency units per bitcoin
    public native long localAmount(long amount, double price);

    // returns the given local currency amount in satoshis
    // price is local currency units (i.e. pennies, pence) per bitcoin
    public native long bitcoinAmount(long localAmount, double price);

    public native void walletFreeEverything();

    public native static boolean validateRecoveryPhrase(String[] words, String phrase);

    public native static String getFirstAddress(byte[] mpk);

    public native static byte[] publishSerializedTransaction(byte[] serializedTransaction, int dandelion,
                                                             byte[] phrase);

    public native long getTotalSent();

    public native long setFeePerKb(long fee, boolean ignore);

    public native boolean isValidBitcoinPrivateKey(String key);

    public native boolean isValidBitcoinBIP38Key(String key);

    public native String getAddressFromPrivKey(String key);

    public native void createInputArray();

    public native void addInputToPrivKeyTx(byte[] hash, int vout, byte[] script, long amount);

    public native boolean confirmKeySweep(byte[] tx, String key);

    public native ImportPrivKeyEntity getPrivKeyObject();

    public native String decryptBip38Key(String privKey, String pass);

    public static native String reverseTxHash(String txHash);

    public native String txHashToHex(byte[] txHash);

    //    public native String txHashSha256Hex(String txHash);

    public native long nativeBalance();

    public native long defaultFee();

    public native long maxFee();

    public native int getTxCount();

    public native long getMinOutputAmountRequested();

    public static native byte[] getAuthPrivKeyForAPI(byte[] seed);

    public static native String getAuthPublicKeyForAPI(byte[] privKey);

    public static native byte[] getSeedFromPhrase(byte[] phrase);

    public static native boolean isTestNet();

    public static native byte[] sweepBCash(byte[] pubKey, String address, byte[] phrase);

    public static native long getBCashBalance(byte[] pubKey);

    public static native int getTxSize(byte[] serializedTx);

    public static native byte[] parseSignSerialize(byte[] txhex, byte[] phrase);


}