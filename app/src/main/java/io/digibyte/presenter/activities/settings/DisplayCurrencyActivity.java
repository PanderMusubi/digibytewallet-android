package io.digibyte.presenter.activities.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.Currency;

import io.digibyte.R;
import io.digibyte.databinding.ActivityDisplayCurrencyBinding;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.presenter.entities.CurrencyEntity;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.manager.FontManager;
import io.digibyte.tools.sqlite.CurrencyDataSource;
import io.digibyte.tools.util.BRConstants;
import io.digibyte.tools.util.BRCurrency;


public class DisplayCurrencyActivity extends BRActivity {

    private ActivityDisplayCurrencyBinding binding;
    private CurrencyListAdapter adapter;

    public static void show(AppCompatActivity activity) {
        Intent intent = new Intent(activity, DisplayCurrencyActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_display_currency);
        setupToolbar();
        setToolbarTitle(R.string.Settings_currency);

        adapter = new CurrencyListAdapter(this);
        adapter.addAll(CurrencyDataSource.getInstance(this).getAllCurrencies());
        binding.currencyListView.setOnItemClickListener((parent, view, position, id) -> {
            TextView currencyItemText = (TextView) view.findViewById(R.id.currency_item_text);
            final String selectedCurrency = currencyItemText.getText().toString();
            String iso = selectedCurrency.substring(0, 3);
            BRSharedPrefs.putIso(DisplayCurrencyActivity.this, iso);
            BRSharedPrefs.putCurrencyListPosition(DisplayCurrencyActivity.this, position);
            updateExchangeRate();
        });
        binding.currencyListView.setAdapter(adapter);
        updateExchangeRate();
    }

    private void updateExchangeRate() {
        //set the rate from the last saved
        String iso = BRSharedPrefs.getIso(this);
        CurrencyEntity entity = CurrencyDataSource.getInstance(this).getCurrencyByIso(iso);
        if (entity != null) {
            String finalExchangeRate = BRCurrency.getFormattedCurrencyString(DisplayCurrencyActivity.this, BRSharedPrefs.getIso(this), new BigDecimal(entity.rate));
            boolean bits = BRSharedPrefs.getCurrencyUnit(this) == BRConstants.CURRENT_UNIT_BITS;
            binding.exchangeText.setText(BRCurrency.getFormattedCurrencyString(this, "DGB", new BigDecimal(bits ? 1000000 : 1)) + " = " + finalExchangeRate);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    public class CurrencyListAdapter extends ArrayAdapter<CurrencyEntity> {
        public final String TAG = CurrencyListAdapter.class.getName();

        private final Context mContext;
        private TextView textViewItem;

        public CurrencyListAdapter(Context mContext) {
            super(mContext, R.layout.currency_list_item);
            this.mContext = mContext;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int tmp = BRSharedPrefs.getCurrencyListPosition(mContext);
            if (convertView == null) {
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                convertView = inflater.inflate(R.layout.currency_list_item, parent, false);
            }
            textViewItem = convertView.findViewById(R.id.currency_item_text);
            FontManager.overrideFonts(textViewItem);
            String iso = getItem(position).code;
            Currency c = null;
            try {
                c = Currency.getInstance(iso);
            } catch (IllegalArgumentException ignored) {
            }
            textViewItem.setText(c == null ? iso : String.format("%s (%s)", iso, c.getSymbol()));
            ImageView checkMark = convertView.findViewById(R.id.currency_checkmark);

            if (position == tmp) {
                checkMark.setVisibility(View.VISIBLE);
            } else {
                checkMark.setVisibility(View.GONE);
            }
            return convertView;

        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public int getItemViewType(int position) {
            return IGNORE_ITEM_VIEW_TYPE;
        }
    }
}