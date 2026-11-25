package com.example.daftarii.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * منطق الإشعارات (بدون Android وبدون Firestore، فبينفحص بـ JUnit):
 * 1. من قائمة الزباين: مين لازم ينبعتله تذكير (بكرة، اليوم، متأخر).
 * 2. دمج التذكيرات الجديدة مع اللي محفوظين، بدون تكرار.
 * 3. كم إشعار مش مقروء، و"قراءة الكل".
 *
 * كل تذكير إله مفتاح (key) ثابت لنفس الزبون ونفس الموعد ونفس النوع،
 * فلو التطبيق فحص 10 مرات باليوم، الإشعار بينضاف مرة وحدة بس.
 */
public final class DueAlerts {

    /** نوع الإشعار. الثلاثة الأولى تبويب "السداد"، والأخير تبويب "النظام". */
    public enum Type { DUE_TOMORROW, DUE_TODAY, OVERDUE, TRIAL_ENDING }

    /** الإشعارات بتنمسح بعد 30 يوم، وما بنخزّن أكتر من 100. */
    public static final long KEEP_MILLIS = 30L * 24 * 60 * 60 * 1000;
    public static final int MAX_ITEMS = 100;

    /** زبون بالشكل اللي بيحتاجه الحساب (مستقل عن طريقة التخزين بـ Firestore). */
    public static final class CustomerInfo {
        public final String id;
        public final String name;
        public final String phone;
        public final long balanceAgorot;
        public final long dueDateMillis;

        public CustomerInfo(String id, String name, String phone, long balanceAgorot, long dueDateMillis) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.balanceAgorot = balanceAgorot;
            this.dueDateMillis = dueDateMillis;
        }
    }

    /** إشعار واحد. read و createdAt بيتغيّروا بالدمج و"قراءة الكل" (بنعمل نسخة جديدة). */
    public static final class Item {
        public final String key;
        public final Type type;
        public final String customerId;
        public final String customerName;
        public final String customerPhone;
        public final long amountAgorot;
        /** موعد السداد، أو (للتجربة) عدد الأيام الضايلة. */
        public final long dueDateMillis;
        public final long createdAt;
        public final boolean read;

        public Item(String key, Type type, String customerId, String customerName, String customerPhone,
                    long amountAgorot, long dueDateMillis, long createdAt, boolean read) {
            this.key = key;
            this.type = type;
            this.customerId = customerId;
            this.customerName = customerName;
            this.customerPhone = customerPhone;
            this.amountAgorot = amountAgorot;
            this.dueDateMillis = dueDateMillis;
            this.createdAt = createdAt;
            this.read = read;
        }

        Item withCreatedAt(long time) {
            return new Item(key, type, customerId, customerName, customerPhone,
                    amountAgorot, dueDateMillis, time, read);
        }

        public Item asRead() {
            return read ? this : new Item(key, type, customerId, customerName, customerPhone,
                    amountAgorot, dueDateMillis, createdAt, true);
        }

        /** تبويب "السداد" ولا "النظام". */
        public boolean isPayment() {
            return type != Type.TRIAL_ENDING;
        }
    }

    private DueAlerts() {}

    /**
     * التذكيرات المستحقة هلأ: موعد بكرة، أو اليوم، أو فات (والرصيد لسا أكبر من صفر).
     * زبون بدون موعد، أو مسدّد، أو موعده بعد يومين أو أكتر: ما إله تذكير.
     */
    public static List<Item> fromCustomers(List<CustomerInfo> customers, long now, TimeZone tz) {
        List<Item> out = new ArrayList<>();
        if (customers == null) return out;
        for (CustomerInfo c : customers) {
            if (c == null || c.id == null || c.balanceAgorot <= 0 || c.dueDateMillis <= 0) continue;
            CustomerStatus status = CustomerStatus.evaluate(c.balanceAgorot, c.dueDateMillis, now, tz);
            Type type;
            if (status.getType() == CustomerStatus.Type.OVERDUE) {
                type = Type.OVERDUE;
            } else if (status.getType() == CustomerStatus.Type.DUE_SOON && status.getDays() == 0) {
                type = Type.DUE_TODAY;
            } else if (status.getType() == CustomerStatus.Type.DUE_SOON && status.getDays() == 1) {
                type = Type.DUE_TOMORROW;
            } else {
                continue;
            }
            String key = type.name() + ":" + c.id + ":" + c.dueDateMillis;
            out.add(new Item(key, type, c.id, c.name, c.phone, c.balanceAgorot, c.dueDateMillis, now, false));
        }
        return out;
    }

    /**
     * إشعار نظام للزائر: لما يضل يومين أو أقل من التجربة (مرة لكل يوم).
     * null إذا مش لازم.
     */
    public static Item trialEnding(String uid, int daysLeft, long now) {
        if (uid == null || daysLeft <= 0 || daysLeft > 2) return null;
        return new Item(Type.TRIAL_ENDING.name() + ":" + uid + ":" + daysLeft, Type.TRIAL_ENDING,
                null, null, null, 0, daysLeft, now, false);
    }

    /**
     * بيضيف الجديد على المحفوظ: المفتاح الموجود ما بيتكرر (وبيضل مقروء إذا كان مقروء).
     * بيمسح القديم (أكتر من 30 يوم)، وبيرتّب الأحدث أول، وبيقص على 100.
     */
    public static List<Item> merge(List<Item> saved, List<Item> fresh, long now) {
        List<Item> out = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        if (saved != null) {
            for (Item i : saved) {
                if (i == null || keys.contains(i.key)) continue;
                if (now - i.createdAt > KEEP_MILLIS) continue;
                keys.add(i.key);
                out.add(i);
            }
        }
        if (fresh != null) {
            for (Item i : fresh) {
                if (i == null || keys.contains(i.key)) continue;
                keys.add(i.key);
                out.add(i.withCreatedAt(now));
            }
        }
        Collections.sort(out, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        return out.size() > MAX_ITEMS ? new ArrayList<>(out.subList(0, MAX_ITEMS)) : out;
    }

    /** اللي انضافوا بالدمج هلأ (عشان نبعتهم كإشعار على الجوال). */
    public static List<Item> added(List<Item> before, List<Item> after) {
        Set<String> old = new HashSet<>();
        if (before != null) for (Item i : before) old.add(i.key);
        List<Item> out = new ArrayList<>();
        if (after != null) for (Item i : after) if (!old.contains(i.key)) out.add(i);
        return out;
    }

    public static int unreadCount(List<Item> items) {
        int n = 0;
        if (items != null) for (Item i : items) if (!i.read) n++;
        return n;
    }

    public static List<Item> markAllRead(List<Item> items) {
        List<Item> out = new ArrayList<>();
        if (items != null) for (Item i : items) out.add(i.asRead());
        return out;
    }

    public static List<Item> markRead(List<Item> items, String key) {
        List<Item> out = new ArrayList<>();
        if (items != null) for (Item i : items) out.add(i.key.equals(key) ? i.asRead() : i);
        return out;
    }

    /** "الكل" (null)، "السداد" (true)، "النظام" (false). */
    public static List<Item> filter(List<Item> items, Boolean payments) {
        List<Item> out = new ArrayList<>();
        if (items == null) return out;
        for (Item i : items) if (payments == null || i.isPayment() == payments) out.add(i);
        return out;
    }

    /** كم يوم فات على الموعد (للمتأخر)، محسوب لهلأ مش لوقت الإشعار. */
    public static int daysLate(long dueDateMillis, long now, TimeZone tz) {
        return Math.max(0, -CustomerStatus.daysBetween(now, dueDateMillis, tz));
    }

    /**
     * رقم الجوال لرابط واتساب (wa.me): بدون + وبدون 0 بالأول.
     * جوال (059) وأوريدو (056): +970. باقي أرقام 05: +972. "" إذا ما في رقم.
     */
    public static String whatsAppNumber(String phone) {
        String p = Validators.normalizePhone(phone);
        if (p.isEmpty() || !p.matches("0\\d{8,9}")) return "";
        if (p.startsWith("059") || p.startsWith("056")) return "970" + p.substring(1);
        if (p.startsWith("05")) return "972" + p.substring(1);
        return "970" + p.substring(1);
    }
}
