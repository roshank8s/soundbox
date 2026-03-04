package com.soundbox.app.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.soundbox.app.R;
import com.soundbox.app.model.PaymentInfo;
import com.soundbox.app.service.PaymentNotificationService;
import com.soundbox.app.service.SoundBoxTTS;
import com.soundbox.app.util.PrefsManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements PaymentNotificationService.OnPaymentListener {

    private View statusIndicator;
    private TextView tvStatus;
    private TextView tvStatusDetail;
    private SwitchMaterial switchService;
    private MaterialButton btnEnableAccess;
    private LinearLayout btnTestSound;
    private RecyclerView rvPayments;
    private TextView tvNoPayments;
    private TextView tvDailyEarnings;
    private TextView tvTransactionCount;
    private PaymentAdapter adapter;
    private PrefsManager prefs;
    private SoundBoxTTS testTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PrefsManager(this);

        initViews();
        setupToolbar();
        setupListeners();
        setupRecyclerView();
    }

    private void initViews() {
        statusIndicator = findViewById(R.id.statusIndicator);
        tvStatus = findViewById(R.id.tvStatus);
        tvStatusDetail = findViewById(R.id.tvStatusDetail);
        switchService = findViewById(R.id.switchService);
        btnEnableAccess = findViewById(R.id.btnEnableAccess);
        btnTestSound = findViewById(R.id.btnTestSound);
        rvPayments = findViewById(R.id.rvPayments);
        tvNoPayments = findViewById(R.id.tvNoPayments);
        tvDailyEarnings = findViewById(R.id.tvDailyEarnings);
        tvTransactionCount = findViewById(R.id.tvTransactionCount);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupListeners() {
        btnEnableAccess.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        });

        btnTestSound.setOnClickListener(v -> {
            if (testTTS == null) {
                testTTS = new SoundBoxTTS(this);
            }
            testTTS.announce(getString(R.string.test_announcement));
        });

        switchService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setServiceEnabled(isChecked);
            updateStatus();
        });
    }

    private void setupRecyclerView() {
        adapter = new PaymentAdapter();
        rvPayments.setLayoutManager(new LinearLayoutManager(this));
        rvPayments.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        loadRecentPayments();
        PaymentNotificationService.setPaymentListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PaymentNotificationService.setPaymentListener(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (testTTS != null) {
            testTTS.shutdown();
        }
    }

    private void updateStatus() {
        boolean hasPermission = isNotificationListenerEnabled();
        boolean isEnabled = prefs.isServiceEnabled();

        switchService.setChecked(isEnabled);

        GradientDrawable indicator = (GradientDrawable) statusIndicator.getBackground();

        if (!hasPermission) {
            indicator.setColor(ContextCompat.getColor(this, R.color.status_inactive));
            tvStatus.setText("No Access");
            tvStatusDetail.setText("Permission needed");
            btnEnableAccess.setVisibility(View.VISIBLE);
        } else if (isEnabled) {
            indicator.setColor(ContextCompat.getColor(this, R.color.status_active));
            tvStatus.setText("Active");
            tvStatusDetail.setText("Service On");
            btnEnableAccess.setVisibility(View.GONE);
        } else {
            indicator.setColor(ContextCompat.getColor(this, R.color.status_inactive));
            tvStatus.setText("Paused");
            tvStatusDetail.setText("Service Off");
            btnEnableAccess.setVisibility(View.GONE);
        }
    }

    private boolean isNotificationListenerEnabled() {
        ComponentName cn = new ComponentName(this, PaymentNotificationService.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    private void loadRecentPayments() {
        List<PaymentInfo> payments = PaymentNotificationService.getRecentPayments();
        adapter.setPayments(payments);

        if (payments.isEmpty()) {
            tvNoPayments.setVisibility(View.VISIBLE);
            rvPayments.setVisibility(View.GONE);
        } else {
            tvNoPayments.setVisibility(View.GONE);
            rvPayments.setVisibility(View.VISIBLE);
        }

        updateDailyEarnings(payments);
    }

    private void updateDailyEarnings(List<PaymentInfo> payments) {
        double dailyTotal = 0;
        int dailyCount = 0;

        long todayStart = getTodayStartMillis();

        for (PaymentInfo payment : payments) {
            if (payment.getTimestamp() >= todayStart) {
                dailyCount++;
                String amountStr = payment.getAmount();
                if (amountStr != null && !amountStr.isEmpty()) {
                    try {
                        String cleaned = amountStr.replaceAll("[^\\d.]", "");
                        dailyTotal += Double.parseDouble(cleaned);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        String formatted;
        if (dailyTotal == (long) dailyTotal) {
            formatted = String.format(Locale.getDefault(), "\u20B9 %,d", (long) dailyTotal);
        } else {
            formatted = String.format(Locale.getDefault(), "\u20B9 %,.2f", dailyTotal);
        }
        tvDailyEarnings.setText(formatted);
        tvTransactionCount.setText(dailyCount + " " + getString(R.string.total_transactions));
    }

    private long getTodayStartMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @Override
    public void onPaymentReceived(PaymentInfo payment) {
        runOnUiThread(() -> {
            adapter.addPayment(payment);
            tvNoPayments.setVisibility(View.GONE);
            rvPayments.setVisibility(View.VISIBLE);

            // Update daily earnings with the new payment included
            List<PaymentInfo> payments = PaymentNotificationService.getRecentPayments();
            updateDailyEarnings(payments);
        });
    }
}
