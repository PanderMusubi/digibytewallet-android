package io.digibyte.presenter.activities;

import android.app.PendingIntent;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;

import com.platform.tools.BRBitId;

import io.digibyte.R;
import io.digibyte.databinding.ActivityPinBinding;
import io.digibyte.presenter.activities.callbacks.LoginActivityCallback;
import io.digibyte.presenter.activities.models.PinActivityModel;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.presenter.fragments.FragmentFingerprint;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.animation.SpringAnimator;
import io.digibyte.tools.security.AuthManager;
import io.digibyte.tools.security.BitcoinUrlHandler;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.wallet.BRWalletManager;

public class LoginActivity extends BRActivity implements BRWalletManager.OnBalanceChanged {
    private static final String TAG = LoginActivity.class.getName();
    ActivityPinBinding binding;
    private StringBuilder pin = new StringBuilder();
    private boolean inputAllowed = true;
    private Handler handler = new Handler(Looper.getMainLooper());
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingNfcIntent;

    private LoginActivityCallback callback = () -> {
        BRAnimator.openScanner(this);
    };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pin);
        binding.setData(new PinActivityModel());
        binding.setCallback(callback);
        binding.brkeyboard.addOnInsertListener(key -> handleClick(key));
        binding.brkeyboard.setShowDot(false);
        binding.brkeyboard.setDeleteImage(R.drawable.ic_delete_white);
        if (!processDeepLink(getIntent()) &&
                AuthManager.isFingerPrintAvailableAndSetup(this)) {
            showFingerprintDialog();
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            pendingNfcIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                    getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processDeepLink(intent);
        processNFC(intent);
        setIntent(new Intent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDots();

        inputAllowed = true;
        BRWalletManager.getInstance().init();
        BRWalletManager.getInstance().addBalanceChangedListener(this);
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingNfcIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        BRWalletManager.getInstance().removeListener(this);
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    private final boolean processDeepLink(@Nullable final Intent intent) {
        Uri data = intent.getData();
        if (data != null && BRBitId.isBitId(data.toString())) {
            BRBitId.digiIDAuthPrompt(this, data.toString(), true);
            return true;
        } else if (data != null && BitcoinUrlHandler.isBitcoinUrl(data.toString())) {
            BRAnimator.showOrUpdateSendFragment(this, data.toString());
            return true;
        }
        return false;
    }

    private final void processNFC(@Nullable final Intent intent) {
        if (intent == null) {
            return;
        }
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                return;
            }
            NdefMessage ndefMessage = ndef.getCachedNdefMessage();
            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                try {
                    String record = new String(ndefRecord.getPayload());
                    if (record.contains("digiid")) {
                        Log.d(LoginActivity.class.getSimpleName(),
                                record.substring(record.indexOf("digiid")));
                        BRBitId.digiIDAuthPrompt(this, record, false);
                    } else if (record.contains("digibyte")) {
                        BRAnimator.showOrUpdateSendFragment(this,
                                record.substring(record.indexOf("digibyte")));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final void handleClick(String key) {
        if (!inputAllowed) {
            Log.e(TAG, "handleClick: input not allowed");
            return;
        }
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else {
            Log.e(TAG, "handleClick: oops: " + key);
        }
    }

    private void showFingerprintDialog() {
        if (getSupportFragmentManager().findFragmentByTag(FragmentFingerprint.class.getName())
                != null) {
            return;
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AuthManager.getInstance().authPrompt(LoginActivity.this, "", "", new AuthType(
                    AuthType.Type.LOGIN));
        }, 500);
    }


    private final void handleDigitClick(Integer dig) {
        if (pin.length() < 6) {
            pin.append(dig);
        }
        updateDots();
    }

    private final void handleDeleteClick() {
        if (pin.length() > 0) {
            pin.deleteCharAt(pin.length() - 1);
        }
        updateDots();
    }

    private final void unlockWallet() {
        BRAnimator.startBreadActivity(this, false);
    }

    private final void showFailedToUnlock() {
        SpringAnimator.failShakeAnimation(LoginActivity.this, binding.pinLayout);
        pin = new StringBuilder("");
        new Handler().postDelayed(() -> {
            inputAllowed = true;
            updateDots();
        }, 1000);
    }

    private final void updateDots() {
        AuthManager.getInstance().updateDots(pin.toString(), binding.dot1,
                binding.dot2, binding.dot3, binding.dot4,
                binding.dot5, binding.dot6, () -> {
                    inputAllowed = false;
                    if (AuthManager.getInstance().checkAuth(pin.toString(), LoginActivity.this)) {
                        handler.postDelayed(() -> {
                            AuthManager.getInstance().authSuccess(LoginActivity.this);
                            unlockWallet();
                        }, 350);
                    } else {
                        AuthManager.getInstance().authFail(LoginActivity.this);
                        showFailedToUnlock();
                    }
                });
    }

    @Override
    public void onComplete(AuthType authType) {
        switch (authType.type) {
            case LOGIN:
                unlockWallet();
                break;
            default:
                super.onComplete(authType);
        }
    }

    @Override
    public void onCancel(AuthType authType) {

    }

    @Override
    public void onBalanceChanged(long balance) {

    }

    @Override
    public void showSendConfirmDialog(String message, int error, byte[] txHash) {
        BRExecutor.getInstance().forMainThreadTasks().execute(() -> {
            BRAnimator.showBreadSignal(LoginActivity.this,
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
}