package com.example.daftarii;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daftarii.auth.AuthRepository;
import com.example.daftarii.logic.DueAlerts;
import com.example.daftarii.logic.Money;
import com.example.daftarii.notifications.NotificationSync;
import com.example.daftarii.notifications.NotificationsAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * شاشة الإشعارات: تذكيرات مواعيد السداد (والتجربة للزائر).
 * الإشعار بيصير مقروء لما المستخدم يضغط عليه، أو "قراءة الكل".
 */
public class NotificationsActivity extends AppCompatActivity implements NotificationsAdapter.Listener {

    /** null = الكل، true = السداد، false = النظام. */
    private Boolean filter = null;
    private List<DueAlerts.Item> all = new ArrayList<>();

    private NotificationsAdapter adapter;
    private TextView tabAll, tabPayments, tabSystem;
    private View empty, markAll;
    private RecyclerView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        tabAll = findViewById(R.id.tabAll);
        tabPayments = findViewById(R.id.tabPayments);
        tabSystem = findViewById(R.id.tabSystem);
        empty = findViewById(R.id.notifEmpty);
        markAll = findViewById(R.id.btnMarkAllRead);
        list = findViewById(R.id.rvNotifications);

        adapter = new NotificationsAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.btnNotificationsBack).setOnClickListener(v -> finish());
        markAll.setOnClickListener(v -> {
            NotificationSync.markAllRead(this);
            all = DueAlerts.markAllRead(all);
            render();
        });
        tabAll.setOnClickListener(v -> setFilter(null));
        tabPayments.setOnClickListener(v -> setFilter(true));
        tabSystem.setOnClickListener(v -> setFilter(false));

        // المحفوظ فوراً، وبعدين بنفحص الزباين وبنحدّث
        all = NotificationSync.saved(this);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationSync.run(this, (items, added) -> {
            if (isFinishing()) return;
            all = items;
            render();
        });
    }

    private void setFilter(Boolean f) {
        filter = f;
        render();
    }

    private void render() {
        styleTab(tabAll, filter == null);
        styleTab(tabPayments, Boolean.TRUE.equals(filter));
        styleTab(tabSystem, Boolean.FALSE.equals(filter));

        List<DueAlerts.Item> shown = DueAlerts.filter(all, filter);
        adapter.submit(this, shown);
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);
        markAll.setVisibility(DueAlerts.unreadCount(all) > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private void styleTab(TextView tab, boolean selected) {
        ViewCompat.setBackgroundTintList(tab, ColorStateList.valueOf(selected
                ? ContextCompat.getColor(this, R.color.surface)
                : android.graphics.Color.TRANSPARENT));
        tab.setTextColor(ContextCompat.getColor(this, selected ? R.color.text_primary : R.color.text_secondary));
        tab.setElevation(selected ? 2f : 0f);
        tab.setSelected(selected);
    }

    /** الضغط على الإشعار أو "عرض الحساب": بيصير مقروء وبيفتح الزبون (أو تحويل الحساب للزائر). */
    @Override
    public void onOpen(DueAlerts.Item item) {
        markRead(item);
        if (!item.isPayment()) {
            startActivity(new Intent(this, GuestUpgradeActivity.class));
            return;
        }
        if (item.customerId == null) return;
        Intent intent = new Intent(this, CustomerDetailActivity.class);
        intent.putExtra("CUSTOMER_ID", item.customerId);
        intent.putExtra("CUSTOMER_NAME", item.customerName);
        intent.putExtra("CUSTOMER_PHONE", item.customerPhone);
        intent.putExtra("CUSTOMER_TOTAL_DEBT", item.amountAgorot / 100.0);
        intent.putExtra("CUSTOMER_DUE_DATE", item.dueDateMillis);
        startActivity(intent);
    }

    /** "تذكير واتساب": رسالة جاهزة فيها اسم الزبون والرصيد واسم المحل. */
    @Override
    public void onWhatsApp(DueAlerts.Item item) {
        markRead(item);
        String number = DueAlerts.whatsAppNumber(item.customerPhone);
        if (number.isEmpty()) {
            Toast.makeText(this, R.string.notif_no_phone, Toast.LENGTH_SHORT).show();
            return;
        }
        String shop = new AuthRepository().currentDisplayName();
        if (shop == null) shop = getString(R.string.app_name);
        String name = item.customerName != null ? item.customerName.trim() : "";
        String msg = getString(R.string.notif_whatsapp_message, name, Money.format(item.amountAgorot), shop);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + number + "?text=" + Uri.encode(msg))));
        } catch (Exception noApp) {
            Toast.makeText(this, R.string.notif_no_whatsapp, Toast.LENGTH_SHORT).show();
        }
    }

    private void markRead(DueAlerts.Item item) {
        if (item.read) return;
        NotificationSync.markRead(this, item.key);
        all = DueAlerts.markRead(all, item.key);
        render();
    }
}
