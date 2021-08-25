package io.digibyte.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import io.digibyte.R;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.animation.SpringAnimator;
import io.digibyte.tools.security.AuthManager;
import io.digibyte.tools.security.PostAuth;

public class UpdatePinActivity extends BasePinActivity {
    private Mode initialCreateMode;
    private Mode mode;

    private static final String START_MODE = "UpdatePinActivity:StartMode";

    public enum Mode {
        SET_PIN, ENTER_CURRENT_PIN, ENTER_NEW_PIN, RE_ENTER_NEW_PIN
    }

    public static void open(AppCompatActivity activity, Mode startMode) {
        Intent intent = new Intent(activity, UpdatePinActivity.class);
        intent.putExtra(START_MODE, startMode);
        activity.startActivity(intent);
    }

    @Override
    protected void onPinConfirmed() {
        switch (mode) {
            case SET_PIN:
                setMode(Mode.RE_ENTER_NEW_PIN);
                setPreviousPin();
                clearDots();
                break;
            case ENTER_CURRENT_PIN:
                if (AuthManager.getInstance().checkAuth(currentPin.toString(), this)) {
                    setMode(Mode.ENTER_NEW_PIN);
                } else {
                    SpringAnimator.failShakeAnimation(this, binding.pinLayout);
                }
                setPreviousPin();
                clearDots();
                break;
            case ENTER_NEW_PIN:
                setMode(Mode.RE_ENTER_NEW_PIN);
                setPreviousPin();
                clearDots();
                break;

            case RE_ENTER_NEW_PIN:
                if (pinsMatch()) {
                    AuthManager.getInstance().setPinCode(currentPin.toString(), this);
                    BRAnimator.showBreadSignal(this, getString(R.string.Alerts_pinSet), getString(R.string.UpdatePin_caption), R.raw.success_check, () -> {
                        switch(initialCreateMode) {
                            case SET_PIN:
                                PostAuth.instance.onCreateWalletAuth(UpdatePinActivity.this, false);
                                break;
                            default:
                                BRAnimator.startBreadActivity(UpdatePinActivity.this, false);
                                break;
                        }
                    });
                    AuthManager.getInstance().authSuccess(this);
                    AuthManager.getInstance().setPinCode(currentPin.toString(), this);
                } else {
                    SpringAnimator.failShakeAnimation(this, binding.pinLayout);
                    setMode(Mode.RE_ENTER_NEW_PIN);
                    currentPin = new StringBuilder("");
                    clearDots();
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialCreateMode = getMode();
        setMode(initialCreateMode);
    }

    private Mode getMode() {
        return (Mode) getIntent().getSerializableExtra(START_MODE);
    }

    private void setMode(Mode mode) {
        String text = "";
        this.mode = mode;
        switch (mode) {
            case SET_PIN:
                text = getString(R.string.UpdatePin_createInstruction);
                setToolbarTitle(R.string.UpdatePin_createTitle);
                break;
            case ENTER_CURRENT_PIN:
                text = getString(R.string.UpdatePin_enterCurrent);
                setToolbarTitle(R.string.UpdatePin_createTitle);
                break;
            case ENTER_NEW_PIN:
                setToolbarTitle(R.string.UpdatePin_createTitle);
                text = getString(R.string.UpdatePin_enterNew);
                break;
            case RE_ENTER_NEW_PIN:
                text = getString(R.string.UpdatePin_reEnterNew);
                setToolbarTitle(R.string.UpdatePin_createTitleConfirm);
                break;
        }

        binding.description.setText(text);
        SpringAnimator.springView(binding.description);
    }
}