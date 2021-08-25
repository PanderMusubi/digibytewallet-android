package io.digibyte.presenter.activities.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import io.digibyte.R;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.presenter.entities.BRSettingsItem;

import java.util.LinkedList;
import java.util.List;

import static io.digibyte.R.layout.settings_list_item;
import static io.digibyte.R.layout.settings_list_section;

public class AdvancedActivity extends BRActivity {
    private static final String TAG = AdvancedActivity.class.getName();
    private ListView listView;
    public List<BRSettingsItem> items = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced);
        listView = findViewById(R.id.settings_list);
        populateItems();
        listView.setAdapter(new SettingsListAdapter(this, R.layout.settings_list_item, items));
    }

    public class SettingsListAdapter extends ArrayAdapter<String> {

        private List<BRSettingsItem> items;
        private Context mContext;

        public SettingsListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<BRSettingsItem> items) {
            super(context, resource);
            this.items = items;
            this.mContext = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            View v = null;
            BRSettingsItem item = items.get(position);
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();

            switch (item.type) {
                case ITEM:
                    v = inflater.inflate(settings_list_item, parent, false);
                    TextView addon = v.findViewById(R.id.item_addon);
                    addon.setText(item.addonText);
                    v.setOnClickListener(item.listener);
                    ((TextView) v.findViewById(R.id.item_title)).setText(item.title);
                    break;
                case SECTION:
                    v = inflater.inflate(settings_list_section, parent, false);
                    ((TextView) v.findViewById(R.id.item_title)).setText(item.title);
                    break;
            }
            return v;

        }

        @Override
        public int getCount() {
            return items == null ? 0 : items.size();
        }

        @Override
        public int getItemViewType(int position) {
            return super.getItemViewType(position);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void populateItems() {

        items.add(new BRSettingsItem("", "", null, BRSettingsItem.Type.SECTION));

        items.add(new BRSettingsItem(getString(R.string.NodeSelector_title), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdvancedActivity.this, NodesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.empty_300);

            }
        }, BRSettingsItem.Type.ITEM));
    }
}
