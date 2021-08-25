package io.digibyte.presenter.activities;

import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;

import io.digibyte.R;
import io.digibyte.databinding.ActivityPinTemplateBinding;
import io.digibyte.presenter.activities.models.PinActivityModel;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.tools.security.AuthManager;

public abstract class BasePinActivity extends BRActivity {

    private static final String PIN_STATE = "BasePinActivity:PinState";
    protected StringBuilder currentPin = new StringBuilder();
    protected StringBuilder previousPin = new StringBuilder();
    protected ActivityPinTemplateBinding binding;
    protected abstract void onPinConfirmed();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pin_template);
        setupToolbar();
        binding.brkeyboard.addOnInsertListener(key -> handleClick(key));
        binding.brkeyboard.setShowDot(false);
        binding.setData(new PinActivityModel());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDots();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(PIN_STATE, currentPin.toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getSerializable(PIN_STATE) != null) {
            currentPin = new StringBuilder(savedInstanceState.getString(PIN_STATE));
        }
    }

    protected void setPreviousPin() {
        previousPin = new StringBuilder(currentPin);
        currentPin = new StringBuilder("");
    }

    protected boolean pinsMatch() {
        return previousPin.toString().equals(currentPin.toString());
    }

    private void handleClick(String key) {
        if (key == null) {
            return;
        }
        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        }
    }


    private void handleDigitClick(Integer dig) {
        if (currentPin.length() < 6)
            currentPin.append(dig);
        updateDots();
    }

    private void handleDeleteClick() {
        if (currentPin.length() > 0)
            currentPin.deleteCharAt(currentPin.length() - 1);
        updateDots();
    }

    protected void updateDots() {
        AuthManager.getInstance().updateDots(currentPin.toString(), binding.dot1, binding.dot2, binding.dot3, binding.dot4, binding.dot5, binding.dot6, () -> new Handler().postDelayed(() -> {
            onPinConfirmed();
        }, 100));
    }

    protected void clearDots() {
        currentPin = new StringBuilder();
        AuthManager.getInstance().updateDots(currentPin.toString(), binding.dot1, binding.dot2, binding.dot3, binding.dot4, binding.dot5, binding.dot6, () -> new Handler().postDelayed(() -> {
            onPinConfirmed();
        }, 100));
    }
}
