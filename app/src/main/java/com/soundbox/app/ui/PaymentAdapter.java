package com.soundbox.app.ui;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.soundbox.app.R;
import com.soundbox.app.model.PaymentInfo;

import java.util.ArrayList;
import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {

    private final List<PaymentInfo> payments = new ArrayList<>();

    public void setPayments(List<PaymentInfo> newPayments) {
        payments.clear();
        payments.addAll(newPayments);
        notifyDataSetChanged();
    }

    public void addPayment(PaymentInfo payment) {
        payments.add(0, payment);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        holder.bind(payments.get(position));
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAmount;
        private final TextView tvSource;
        private final TextView tvApp;
        private final TextView tvTime;

        PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvApp = itemView.findViewById(R.id.tvApp);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(PaymentInfo payment) {
            tvAmount.setText(payment.getDisplayAmount());

            String source = payment.getSource();
            tvSource.setText(source != null ? "From: " + source : "Payment received");

            tvApp.setText("via " + payment.getAppName());

            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    payment.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            tvTime.setText(timeAgo);
        }
    }
}
