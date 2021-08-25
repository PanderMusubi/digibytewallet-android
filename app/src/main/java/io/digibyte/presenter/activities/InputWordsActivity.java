package io.digibyte.presenter.activities;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import io.digibyte.DigiByte;
import io.digibyte.R;
import io.digibyte.databinding.ActivityInputWordsBinding;
import io.digibyte.presenter.activities.callbacks.ActivityInputWordsCallback;
import io.digibyte.presenter.activities.intro.IntroActivity;
import io.digibyte.presenter.activities.models.InputWordsViewModel;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.tools.animation.BRDialog;
import io.digibyte.tools.animation.SpringAnimator;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.security.AuthManager;
import io.digibyte.tools.security.PostAuth;
import io.digibyte.tools.security.SmartValidator;
import io.digibyte.tools.util.Utils;
import io.digibyte.wallet.BRWalletManager;

public class InputWordsActivity extends BRActivity implements TextView.OnEditorActionListener {
    private static final String INPUT_WORDS_TYPE = "InputWordsActivity:Type";
    private FocusListener focusListener = new FocusListener();
    private ActivityInputWordsBinding binding;
    private InputWordsViewModel model = new InputWordsViewModel();

    private ActivityInputWordsCallback callback = () -> {
        final AppCompatActivity app = InputWordsActivity.this;
        String phraseToCheck = getPhrase();
        if (phraseToCheck == null) {
            return;
        }
        String cleanPhrase = SmartValidator.cleanPaperKey(app, phraseToCheck);
        if (SmartValidator.isPaperKeyValid(app, cleanPhrase)) {
            Utils.hideKeyboard(app);
            switch(getType()) {
                case WIPE:
                    BRDialog.showCustomDialog(InputWordsActivity.this, getString(R.string.WipeWallet_alertTitle), getString(R.string.WipeWallet_alertMessage), getString(R.string.WipeWallet_wipe), getString(R.string.Button_cancel),
                            brDialogView -> {
                                brDialogView.dismissWithAnimation();
                                BRWalletManager m = BRWalletManager.getInstance();
                                m.wipeWalletButKeystore(app);
                                m.wipeKeyStore(app);
                                Intent intent = new Intent(app, IntroActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }, brDialogView -> brDialogView.dismissWithAnimation(), null, 0);
                    break;
                case RESET_PIN:
                    if (SmartValidator.isPaperKeyCorrect(cleanPhrase, app)) {
                        AuthManager.getInstance().setPinCode("", InputWordsActivity.this);
                        UpdatePinActivity.open(InputWordsActivity.this,
                                UpdatePinActivity.Mode.ENTER_NEW_PIN);
                    } else {
                        BRDialog.showCustomDialog(app, "", getString(R.string.RecoverWallet_invalid), getString(R.string.AccessibilityLabels_close), null,
                                brDialogView -> brDialogView.dismissWithAnimation(), null, null, 0);
                    }
                    break;
                case RESTORE:
                    BRWalletManager m = BRWalletManager.getInstance();
                    m.wipeWalletButKeystore(app);
                    m.wipeKeyStore(app);
                    PostAuth.instance.setPhraseForKeyStore(cleanPhrase);
                    BRSharedPrefs.putAllowSpend(app, false);
                    //if this screen is shown then we did not upgrade to the new app, we installed it
                    BRSharedPrefs.putGreetingsShown(app, true);
                    PostAuth.instance.onRecoverWalletAuth(app, false);                    break;
            }
        } else {
            BRDialog.showCustomDialog(app, "", getResources().getString(R.string.RecoverWallet_invalid), getString(R.string.AccessibilityLabels_close), null,
                    brDialogView -> brDialogView.dismissWithAnimation(), null, null, 0);
        }
    };

    public enum Type {
        WIPE, RESET_PIN, RESTORE
    }

    public static void open(AppCompatActivity activity, Type type) {
        Intent intent = new Intent(activity, InputWordsActivity.class);
        intent.putExtra(INPUT_WORDS_TYPE, type);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_input_words);
        setupToolbar();
        binding.setData(model);
        binding.setFocusListener(focusListener);
        binding.setEditorAction(this);
        binding.setCallback(callback);

        if (Utils.isUsingCustomInputMethod(this)) {
            BRDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title), getString(R.string.Alert_customKeyboard_android),
                    getString(R.string.Button_ok), getString(R.string.JailbreakWarnings_close),
                    brDialogView -> {
                        InputMethodManager imeManager =
                                (InputMethodManager) getApplicationContext().getSystemService(
                                        INPUT_METHOD_SERVICE);
                        imeManager.showInputMethodPicker();
                        brDialogView.dismissWithAnimation();
                    }, brDialogView -> brDialogView.dismissWithAnimation(), null, 0);
        }

        switch(getType()) {
            case WIPE:
                setToolbarTitle(R.string.MenuViewController_recoverButton);
                binding.setDescription(getString(R.string.WipeWallet_instruction));
                break;
            case RESTORE:
                setToolbarTitle(R.string.RecoverWallet_header);
                binding.setDescription(getString(R.string.RecoverWallet_subheader));
                break;
            case RESET_PIN:
                setToolbarTitle(R.string.RecoverWallet_header_reset_pin);
                binding.setDescription(getString(R.string.RecoverWallet_subheader_reset_pin));
                break;
        }
    }

    private Type getType() {
        return (Type) getIntent().getSerializableExtra(INPUT_WORDS_TYPE);
    }

    private String getPhrase() {
        boolean success = true;
        if (Utils.isNullOrEmpty(model.getWord1())) {
            SpringAnimator.failShakeAnimation(this, binding.word1);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord2())) {
            SpringAnimator.failShakeAnimation(this, binding.word2);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord3())) {
            SpringAnimator.failShakeAnimation(this, binding.word3);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord4())) {
            SpringAnimator.failShakeAnimation(this, binding.word4);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord5())) {
            SpringAnimator.failShakeAnimation(this, binding.word5);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord6())) {
            SpringAnimator.failShakeAnimation(this, binding.word6);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord7())) {
            SpringAnimator.failShakeAnimation(this, binding.word7);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord8())) {
            SpringAnimator.failShakeAnimation(this, binding.word8);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord9())) {
            SpringAnimator.failShakeAnimation(this, binding.word9);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord10())) {
            SpringAnimator.failShakeAnimation(this, binding.word10);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord11())) {
            SpringAnimator.failShakeAnimation(this, binding.word11);
            success = false;
        }
        if (Utils.isNullOrEmpty(model.getWord12())) {
            SpringAnimator.failShakeAnimation(this, binding.word12);
            success = false;
        }

        if (!success) return null;

        return w(model.getWord1()) + " " + w(model.getWord2()) + " " + w(model.getWord3()) +
                " " + w(model.getWord4()) + " " + w(model.getWord5()) + " " + w(model.getWord6()) +
                " " + w(model.getWord7()) + " " + w(model.getWord8()) + " " + w(model.getWord9()) +
                " " + w(model.getWord10()) + " " + w(model.getWord11()) + " " + w(model.getWord12());
    }

    private String w(String word) {
        return word.replaceAll(" ", "");
    }

    private void clearWords() {
        model.setWord1("");
        model.setWord2("");
        model.setWord3("");
        model.setWord4("");
        model.setWord5("");
        model.setWord6("");
        model.setWord7("");
        model.setWord8("");
        model.setWord9("");
        model.setWord10("");
        model.setWord11("");
        model.setWord12("");
    }

    public static class FocusListener implements View.OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                validateWord((EditText) v);
            } else {
                ((EditText) v).setTextColor(ContextCompat.getColor(v.getContext(), R.color.white));
            }
        }

        private void validateWord(EditText view) {
            String word = view.getText().toString();
            boolean valid = SmartValidator.isWordValid(DigiByte.getContext(), word);
            view.setTextColor(ContextCompat.getColor(view.getContext(), valid ? R.color.white : R.color.red_text));
            if (!valid)
                SpringAnimator.failShakeAnimation(DigiByte.getContext(), view);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
            binding.sendButton.performClick();
        }
        return false;
    }
}