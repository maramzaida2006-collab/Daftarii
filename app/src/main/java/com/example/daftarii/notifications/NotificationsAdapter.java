package com.example.daftarii.notifications;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daftarii.R;
import com.example.daftarii.logic.DueAlerts;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** قائمة الإشعارات مقسومة حسب اليوم (اليوم / أمس / التاريخ). */
public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onOpen(DueAlerts.Item item);
        void onWhatsApp(DueAlerts.Item item);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    /** كل صف: يا String (عنوان يوم) يا DueAlerts.Item. */
    private final List<Object> rows = new ArrayList<>();
    private final Listener listener;

    public NotificationsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(Context context, List<DueAlerts.Item> items) {
        rows.clear();
        String lastHeader = null;
        for (DueAlerts.Item item : items) {
            String header = dayHeader(context, item.createdAt);
            if (!header.equals(lastHeader)) {
                rows.add(header);
                lastHeader = header;
            }
            rows.add(item);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inf.inflate(R.layout.item_notification_header, parent, false));
        }
        return new ItemHolder(inf.inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).title.setText((String) row);
        } else {
            ((ItemHolder) holder).bind((DueAlerts.Item) row, listener);
        }
    }

    private static String dayHeader(Context c, long time) {
        if (DateUtils.isToday(time)) return c.getString(R.string.notif_today);
        if (DateUtils.isToday(time + DateUtils.DAY_IN_MILLIS)) return c.getString(R.string.notif_yesterday);
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(time));
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(View v) {
            super(v);
            title = v.findViewById(R.id.tvNotifHeader);
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        final View card, iconBox, actions, unreadDot, btnWhatsApp, btnOpen;
        final ImageView icon;
        final TextView title, body, time;

        ItemHolder(View v) {
            super(v);
            card = v.findViewById(R.id.notifCard);
            iconBox = v.findViewById(R.id.notifIconBox);
            icon = v.findViewById(R.id.notifIcon);
            title = v.findViewById(R.id.tvNotifTitle);
            body = v.findViewById(R.id.tvNotifBody);
            actions = v.findViewById(R.id.notifActions);
            btnWhatsApp = v.findViewById(R.id.btnNotifWhatsApp);
            btnOpen = v.findViewById(R.id.btnNotifOpen);
            time = v.findViewById(R.id.tvNotifTime);
            unreadDot = v.findViewById(R.id.notifUnreadDot);
        }

        void bind(DueAlerts.Item item, Listener listener) {
            Context c = itemView.getContext();
            int bg, fg, iconRes;
            switch (item.type) {
                case OVERDUE:
                    bg = R.color.debt_bg; fg = R.color.debt_fg; iconRes = R.drawable.ic_clock;
                    break;
                case DUE_TODAY:
                    bg = R.color.due_bg; fg = R.color.due_fg; iconRes = R.drawable.ic_calendar;
                    break;
                case DUE_TOMORROW:
                    bg = R.color.payment_bg; fg = R.color.payment_fg; iconRes = R.drawable.ic_calendar;
                    break;
                case TRIAL_ENDING:
                default:
                    bg = R.color.lavender_soft; fg = R.color.violet; iconRes = R.drawable.ic_shield;
                    break;
            }
            ViewCompat.setBackgroundTintList(iconBox, ColorStateList.valueOf(ContextCompat.getColor(c, bg)));
            icon.setImageResource(iconRes);
            icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(c, fg)));

            title.setText(NotificationText.title(c, item));
            body.setText(NotificationText.body(c, item));
            time.setText(DateUtils.getRelativeTimeSpanString(item.createdAt,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

            unreadDot.setVisibility(item.read ? View.INVISIBLE : View.VISIBLE);
            card.setAlpha(item.read ? 0.85f : 1f);

            // أزرار: واتساب للمتأخر واللي موعده اليوم (إذا في رقم)، و"عرض الحساب" لكل السداد.
            // إشعار التجربة: الزر بيصير "إنشاء حساب".
            boolean payment = item.isPayment();
            boolean whatsapp = (item.type == DueAlerts.Type.OVERDUE || item.type == DueAlerts.Type.DUE_TODAY)
                    && !DueAlerts.whatsAppNumber(item.customerPhone).isEmpty();
            actions.setVisibility(View.VISIBLE);
            btnWhatsApp.setVisibility(whatsapp ? View.VISIBLE : View.GONE);
            ((TextView) btnOpen).setText(payment ? R.string.notif_action_open : R.string.notif_action_upgrade);
            btnWhatsApp.setOnClickListener(v -> listener.onWhatsApp(item));
            btnOpen.setOnClickListener(v -> listener.onOpen(item));
            card.setOnClickListener(v -> listener.onOpen(item));
        }
    }
}
