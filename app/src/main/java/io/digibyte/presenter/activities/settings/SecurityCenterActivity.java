package io.digibyte.presenter.activities.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.digibyte.R;
import io.digibyte.databinding.ActivitySecurityCenterBinding;
import io.digibyte.presenter.activities.UpdatePinActivity;
import io.digibyte.presenter.activities.intro.WriteDownActivity;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.presenter.entities.BRSecurityCenterItem;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.security.BRKeyStore;
import io.digibyte.tools.util.Utils;

public class SecurityCenterActivity extends BRActivity {

    public List<BRSecurityCenterItem> itemList = new ArrayList<>();
    private SecurityCenterListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySecurityCenterBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_security_center);
        setupToolbar();
        setToolbarTitle(R.string.SecurityCenter_title);
        adapter = new SecurityCenterListAdapter(this, R.layout.menu_list_item, itemList);
        binding.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList();
        adapter.notifyDataSetChanged();
    }

    public class SecurityCenterListAdapter extends ArrayAdapter<BRSecurityCenterItem> {

        private List<BRSecurityCenterItem> items;
        private Context mContext;
        private int defaultLayoutResource = R.layout.security_center_list_item;

        public SecurityCenterListAdapter(@NonNull Context context, @LayoutRes int resource,
                @NonNull List<BRSecurityCenterItem> items) {
            super(context, resource);
            this.items = items;
            this.mContext = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                convertView = inflater.inflate(defaultLayoutResource, parent, false);
            }
            TextView title = convertView.findViewById(R.id.item_title);
            TextView text = convertView.findViewById(R.id.item_text);
            ImageView checkMark = convertView.findViewById(R.id.check_mark);

            title.setText(items.get(position).title);
            text.setText(items.get(position).text);
            checkMark.setImageResource(items.get(position).checkMarkResId);
            convertView.setOnClickListener(items.get(position).listener);
            return convertView;

        }

        @Override
        public int getCount() {
            return items == null ? 0 : items.size();
        }
    }

    private void updateList() {
        boolean isPinSet = BRKeyStore.getPinCode(this).length() == 6;
        itemList.clear();
        itemList.add(new BRSecurityCenterItem(getString(R.string.SecurityCenter_pinTitle),
                getString(R.string.SecurityCenter_pinDescription),
                isPinSet ? R.drawable.ic_check_mark_blue : R.drawable.ic_check_mark_grey, v -> {
            UpdatePinActivity.open(SecurityCenterActivity.this,
                    UpdatePinActivity.Mode.ENTER_CURRENT_PIN);
        }));

        int resId = BRSharedPrefs.getUseFingerprint(SecurityCenterActivity.this)
                ? R.drawable.ic_check_mark_blue
                : R.drawable.ic_check_mark_grey;

        if (Utils.isFingerprintAvailable(this)) {
            itemList.add(new BRSecurityCenterItem(
                    getString(R.string.SecurityCenter_touchIdTitle_android),
                    getString(R.string.SecurityCenter_touchIdDescription),
                    resId, v -> {
                Intent intent = new Intent(SecurityCenterActivity.this, FingerprintActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }));
        }

        boolean isPaperKeySet = BRSharedPrefs.getPhraseWroteDown(this);
        itemList.add(new BRSecurityCenterItem(getString(R.string.SecurityCenter_paperKeyTitle),
                getString(R.string.SecurityCenter_paperKeyDescription),
                isPaperKeySet ? R.drawable.ic_check_mark_blue : R.drawable.ic_check_mark_grey,
                v -> {
                    Intent intent = new Intent(SecurityCenterActivity.this,
                            WriteDownActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
                }));
    }
}