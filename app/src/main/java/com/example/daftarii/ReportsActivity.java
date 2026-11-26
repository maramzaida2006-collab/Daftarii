package com.example.daftarii;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.example.daftarii.data.CustomerRepository;
import com.example.daftarii.data.TransactionRepository;
import com.example.daftarii.logic.ReportStats;
import com.example.daftarii.logic.TransactionType;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * التقارير من بيانات الحساب الحقيقية (users/{uid}): الديون الجديدة والمحصّل ونسبة التحصيل
 * حسب الفترة، والزباين المتأخرين، والزباين الجدد، ومخطط آخر 6 أشهر.
 * الحسابات نفسها بـ logic/ReportStats (عليها JUnit).
 */
public class ReportsActivity extends AppCompatActivity {

    private static final int CHART_MONTHS = 6;
    private static final int BAR_MAX_DP = 84;

    private View btnReportsNotifications;
    private TextView tabThisMonth, tabThreeMonths, tabThisYear;

    private TextView tvCollectionSummary, tvCollectionPercent;
    private TextView tvStatNewDebtsAmount, tvStatNewDebtsCount;
    private TextView tvStatCollectedAmount, tvStatCollectedCount;
    private TextView tvStatOverdueCount, tvStatOverdueTotal;
    private TextView tvStatNewCustomersCount, tvStatNewCustomersSub;
    private LinearLayout monthlyBarsContainer;

    private FrameLayout navReportsTabHome, navReportsTabProfile;
    private LinearLayout navReportsTabActive;

    private ReportStats.Period selectedPeriod = ReportStats.Period.THIS_MONTH;

    private final List<ReportStats.Tx> transactions = new ArrayList<>();
    private final List<ReportStats.Cust> customers = new ArrayList<>();

    private ListenerRegistration customersListener, transactionsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        initViews();
        highlightPeriodTab();
        setupListeners();
        render(); // أصفار لحد ما توصل البيانات (بدل أرقام التصميم الوهمية)
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopListening();
    }

    private void initViews() {
        btnReportsNotifications = findViewById(R.id.btnReportsNotifications);

        tabThisMonth = findViewById(R.id.tabThisMonth);
        tabThreeMonths = findViewById(R.id.tabThreeMonths);
        tabThisYear = findViewById(R.id.tabThisYear);

        tvCollectionSummary = findViewById(R.id.tvCollectionSummary);
        tvCollectionPercent = findViewById(R.id.tvCollectionPercent);

        tvStatNewDebtsAmount = findViewById(R.id.tvStatNewDebtsAmount);
        tvStatNewDebtsCount = findViewById(R.id.tvStatNewDebtsCount);

        tvStatCollectedAmount = findViewById(R.id.tvStatCollectedAmount);
        tvStatCollectedCount = findViewById(R.id.tvStatCollectedCount);

        tvStatOverdueCount = findViewById(R.id.tvStatOverdueCount);
        tvStatOverdueTotal = findViewById(R.id.tvStatOverdueTotal);

        tvStatNewCustomersCount = findViewById(R.id.tvStatNewCustomersCount);
        tvStatNewCustomersSub = findViewById(R.id.tvStatNewCustomersSub);

        monthlyBarsContainer = findViewById(R.id.monthlyBarsContainer);

        navReportsTabHome = findViewById(R.id.navReportsTabHome);
        navReportsTabActive = findViewById(R.id.navReportsTabActive);
        navReportsTabProfile = findViewById(R.id.navReportsTabProfile);
    }

    private void setupListeners() {
        if (btnReportsNotifications != null) {
            btnReportsNotifications.setOnClickListener(v ->
                    startActivity(new Intent(this, NotificationsActivity.class)));
        }

        if (tabThisMonth != null) tabThisMonth.setOnClickListener(v -> selectPeriod(ReportStats.Period.THIS_MONTH));
        if (tabThreeMonths != null) tabThreeMonths.setOnClickListener(v -> selectPeriod(ReportStats.Period.THREE_MONTHS));
        if (tabThisYear != null) tabThisYear.setOnClickListener(v -> selectPeriod(ReportStats.Period.THIS_YEAR));

        if (navReportsTabHome != null) {
            navReportsTabHome.setOnClickListener(v -> {
                startActivity(new Intent(ReportsActivity.this, MainActivity.class));
                finish();
            });
        }
        if (navReportsTabProfile != null) {
            navReportsTabProfile.setOnClickListener(v -> {
                startActivity(new Intent(ReportsActivity.this, ProfileActivity.class));
                finish();
            });
        }
    }

    // ---------------- البيانات ----------------

    private void startListening() {
        stopListening();
        customersListener = CustomerRepository.listen(false, list -> {
            customers.clear();
            for (Customer c : list) {
                customers.add(new ReportStats.Cust(Math.round(c.getTotalDebt() * 100),
                        c.getDueDate(), c.getCreatedAt()));
            }
            render();
        }, null);

        transactionsListener = TransactionRepository.listenAll(list -> {
            transactions.clear();
            for (Transaction t : list) {
                TransactionType type = "PAYMENT".equals(t.getType()) ? TransactionType.PAYMENT : TransactionType.DEBT;
                transactions.add(new ReportStats.Tx(type, Math.round(Math.abs(t.getAmount()) * 100), t.getTimestamp()));
            }
            render();
        });
    }

    private void stopListening() {
        if (customersListener != null) customersListener.remove();
        if (transactionsListener != null) transactionsListener.remove();
        customersListener = null;
        transactionsListener = null;
    }

    private void selectPeriod(ReportStats.Period period) {
        selectedPeriod = period;
        highlightPeriodTab();
        render();
    }

    // ---------------- العرض ----------------

    private void render() {
        long now = System.currentTimeMillis();
        ReportStats r = ReportStats.compute(transactions, customers, selectedPeriod, now);

        int summaryRes;
        int periodRes;
        switch (selectedPeriod) {
            case THREE_MONTHS:
                summaryRes = R.string.reports_summary_three;
                periodRes = R.string.reports_new_customers_three;
                break;
            case THIS_YEAR:
                summaryRes = R.string.reports_summary_year;
                periodRes = R.string.reports_new_customers_year;
                break;
            default:
                summaryRes = R.string.reports_summary_month;
                periodRes = R.string.reports_new_customers_month;
        }

        setText(tvCollectionSummary, getString(summaryRes, money(r.collected), money(r.newDebts)));
        setText(tvCollectionPercent, String.format(Locale.US, "%d%%", r.percent));

        setText(tvStatNewDebtsAmount, "₪ " + money(r.newDebts));
        setText(tvStatNewDebtsCount, getString(R.string.reports_tx_count, r.newDebtsCount));

        setText(tvStatCollectedAmount, "₪ " + money(r.collected));
        setText(tvStatCollectedCount, getString(R.string.reports_payment_count, r.collectedCount));

        setText(tvStatOverdueCount, String.valueOf(r.overdueCount));
        setText(tvStatOverdueTotal, getString(R.string.reports_overdue_total, money(r.overdueTotal)));

        setText(tvStatNewCustomersCount, String.valueOf(r.newCustomers));
        setText(tvStatNewCustomersSub, getString(periodRes));

        renderChart(ReportStats.monthly(transactions, now, CHART_MONTHS), now);
    }

    /** المخطط: عمودين لكل شهر (تحصيل بنفسجي، ديون خوخي)، بالنسبة لأعلى رقم بالـ 6 أشهر. */
    private void renderChart(long[][] months, long now) {
        if (monthlyBarsContainer == null) return;
        monthlyBarsContainer.removeAllViews();

        long max = 0;
        for (long[] m : months) max = Math.max(max, Math.max(m[0], m[1]));

        SimpleDateFormat monthName = new SimpleDateFormat("MMM", Locale.getDefault());
        for (int i = 0; i < months.length; i++) {
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            column.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

            LinearLayout bars = new LinearLayout(this);
            bars.setOrientation(LinearLayout.HORIZONTAL);
            bars.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            bars.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0, 1f));
            bars.addView(bar(months[i][1], max, R.color.violet, true));
            bars.addView(bar(months[i][0], max, R.color.peach, false));
            column.addView(bars);

            TextView label = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(4);
            label.setLayoutParams(lp);
            label.setText(monthName.format(new Date(ReportStats.monthStart(now, months.length - 1 - i))));
            label.setTextColor(getColor(R.color.text_secondary));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            label.setTypeface(ResourcesCompat.getFont(this, R.font.readex_pro_regular));
            label.setMaxLines(1);
            column.addView(label);

            monthlyBarsContainer.addView(column);
        }
    }

    private View bar(long value, long max, int colorRes, boolean withGap) {
        int h = max > 0 ? (int) Math.round(dp(BAR_MAX_DP) * (double) value / max) : 0;
        if (h < dp(2)) h = dp(2); // خط صغير حتى للصفر، عشان الشهر يبيّن
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(9), h);
        if (withGap) lp.setMarginEnd(dp(2));
        v.setLayoutParams(lp);
        v.setBackgroundResource(R.drawable.shape_r16);
        v.setBackgroundTintList(getColorStateList(colorRes));
        if (value == 0) v.setAlpha(0.35f);
        return v;
    }

    private void highlightPeriodTab() {
        if (tabThisMonth == null || tabThreeMonths == null || tabThisYear == null) return;
        style(tabThisMonth, selectedPeriod == ReportStats.Period.THIS_MONTH);
        style(tabThreeMonths, selectedPeriod == ReportStats.Period.THREE_MONTHS);
        style(tabThisYear, selectedPeriod == ReportStats.Period.THIS_YEAR);
    }

    private void style(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.shape_r16 : 0);
        tab.setBackgroundTintList(selected ? getColorStateList(R.color.background) : null);
        tab.setTextColor(getColor(selected ? R.color.text_primary : R.color.text_secondary));
    }

    // ---------------- مساعدات ----------------

    /** 412050 أغورة = "4,121" (التقارير بدون قروش). */
    private static String money(long agorot) {
        return String.format(Locale.US, "%,d", Math.round(agorot / 100.0));
    }

    private static void setText(TextView tv, CharSequence text) {
        if (tv != null) tv.setText(text);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
