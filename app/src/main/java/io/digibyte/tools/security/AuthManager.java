package io.digibyte.tools.security;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import android.util.Log;
import android.view.View;

import com.appolica.flubber.Flubber;

import io.digibyte.DigiByte;
import io.digibyte.R;
import io.digibyte.presenter.activities.DisabledActivity;
import io.digibyte.presenter.activities.util.ActivityUTILS;
import io.digibyte.presenter.fragments.FragmentFingerprint;
import io.digibyte.presenter.fragments.FragmentPin;
import io.digibyte.presenter.interfaces.BRAuthCompletion;
import io.digibyte.tools.animation.BRDialog;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.util.Utils;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/20/15.
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

public class AuthManager {
    public static final String TAG = AuthManager.class.getName();
    private static AuthManager instance;
    private String previousTry;
    private static Drawable pinSelected =
            AppCompatResources.getDrawable(DigiByte.getContext(), R.drawable.pin_selected);
    private static Drawable pinUnselected =
            AppCompatResources.getDrawable(DigiByte.getContext(), R.drawable.pin_unselected);

    private AuthManager() {
        previousTry = "";
    }

    public static AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public boolean checkAuth(CharSequence passSequence, Context context) {
        Log.e(TAG, "checkAuth: ");
        String tempPass = passSequence.toString();
        if (!previousTry.equals(tempPass)) {
            int failCount = BRKeyStore.getFailCount(context);
            BRKeyStore.putFailCount(failCount + 1, context);
        }
        previousTry = tempPass;

        String pass = BRKeyStore.getPinCode(context);
        boolean match = pass != null && tempPass.equals(pass);
        if (!match) {
            if (BRKeyStore.getFailCount(context) >= 3) {
                setWalletDisabled((Activity) context);
            }
        }
        return match;
    }

    //when currentPin auth success
    public void authSuccess(final Context app) {
        BRKeyStore.putFailCount(0, app);
    }

    public void authFail(Context app) {

    }

    public boolean isWalletDisabled(Activity app) {
        int failCount = BRKeyStore.getFailCount(app);
        return failCount >= 3 && disabledUntil(app) > BRSharedPrefs.getSecureTime(app);
    }

    public long disabledUntil(Activity app) {
        int failCount = BRKeyStore.getFailCount(app);
        long failTimestamp = BRKeyStore.getFailTimeStamp(app);
        double pow = Math.pow(6, failCount - 3) * 60;
        return (long) ((failTimestamp + pow * 1000));
    }

    public void setWalletDisabled(Activity app) {
        if (!(app instanceof DisabledActivity)) {
            ActivityUTILS.showWalletDisabled(app);
        }
    }

    public void setPinCode(String pass, Activity context) {
        BRKeyStore.putFailCount(0, context);
        BRKeyStore.putPinCode(pass, context);
    }

    public void updateDots(String pin, View dot1, View dot2,
                           View dot3, View dot4, View dot5, View dot6,
                           final OnPinSuccess onPinSuccess) {
        int selectedDots = pin.length();

        for (int i = 0; i < pin.length(); i++) {
            switch (i) {
                case 0:
                    setDotSelected(dot1);
                    break;
                case 1:
                    setDotSelected(dot2);
                    break;
                case 2:
                    setDotSelected(dot3);
                    break;
                case 3:
                    setDotSelected(dot4);
                    break;
                case 4:
                    setDotSelected(dot5);
                    break;
                case 5:
                    setDotSelected(dot6);
                    break;
            }
        }
        for (int i = pin.length(); i < 6; i++) {
            switch (i) {
                case 0:
                    setDotUnSelected(dot1);
                    break;
                case 1:
                    setDotUnSelected(dot2);
                    break;
                case 2:
                    setDotUnSelected(dot3);
                    break;
                case 3:
                    setDotUnSelected(dot4);
                    break;
                case 4:
                    setDotUnSelected(dot5);
                    break;
                case 5:
                    setDotUnSelected(dot6);
                    break;
            }
        }
        if (pin.length() == 6) {
            new Handler().postDelayed(() -> onPinSuccess.onSuccess(), 100);
        }
    }

    private void setDotSelected(View view) {
        if (view.getBackground() != pinSelected) {
            view.setBackground(pinSelected);
            Flubber.with()
                    .animation(Flubber.AnimationPreset.FLIP_Y)
                    .interpolator(Flubber.Curve.BZR_EASE_IN_OUT_QUAD)
                    .duration(500)
                    .autoStart(true)
                    .createFor(view);
        }
    }

    private void setDotUnSelected(View view) {
        view.setBackground(pinUnselected);
    }

    /**
     * This version of authPrompt is used when either fingerprint or pin can be used
     * @param context
     * @param title
     * @param message
     * @param type
     */
    public void authPrompt(final Context context, String title, String message, BRAuthCompletion.AuthType type) {
        authPrompt(context, title, message, isFingerPrintAvailableAndSetup(context), type);
    }

    /**
     * This version of authPrompt is used when fingerprint enabled or not is desired to be set specifically
     * @param context
     * @param title
     * @param message
     * @param fingerprint
     * @param type
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void authPrompt(final Context context, String title, String message, boolean fingerprint, BRAuthCompletion.AuthType type) {
        if (context instanceof Activity) {
            final AppCompatActivity app = (AppCompatActivity) context;
            final KeyguardManager keyguardManager =
                    (KeyguardManager) context.getSystemService(Activity.KEYGUARD_SERVICE);
            if (keyguardManager.isKeyguardSecure()) {
                if (fingerprint) {
                    FragmentFingerprint.show(app, title, message, type);
                } else {
                    FragmentPin.show(app, title, message, type);
                }
            } else {
                BRDialog.showCustomDialog(app,
                        "",
                        app.getString(R.string.Prompts_NoScreenLock_body_android),
                        app.getString(R.string.AccessibilityLabels_close), null,
                        brDialogView -> app.finish(), null, dialog -> app.finish(), 0);
            }
        }
    }

    public static boolean isFingerPrintAvailableAndSetup(Context context) {
        return Utils.isFingerprintAvailable(context) && BRSharedPrefs.getUseFingerprint(context);
    }

    public interface OnPinSuccess {
        void onSuccess();
    }
}