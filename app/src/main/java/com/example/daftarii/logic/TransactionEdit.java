package com.example.daftarii.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * منطق تعديل حركة (بدون Android، فبينفحص بـ JUnit):
 * - بالتعديل بيتغيّر المبلغ والوصف بس، النوع (دين/دفعة) ما بيتغيّر (قرار الـ PO).
 * - كل تعديل بينضاف لسجل التعديلات (editHistory).
 * - الرصيد بيتعدّل بالفرق بس (BalanceCalculator.editDiff).
 * المبالغ كلها بالأغورة (long).
 */
public final class TransactionEdit {

    /** سطر واحد بسجل التعديلات. */
    public static final class Entry {
        public final long at;
        public final long oldAmount;
        public final long newAmount;
        public final String oldNote;
        public final String newNote;

        public Entry(long at, long oldAmount, long newAmount, String oldNote, String newNote) {
            this.at = at;
            this.oldAmount = oldAmount;
            this.newAmount = newAmount;
            this.oldNote = oldNote == null ? "" : oldNote;
            this.newNote = newNote == null ? "" : newNote;
        }

        public boolean amountChanged() {
            return oldAmount != newAmount;
        }

        public boolean noteChanged() {
            return !oldNote.equals(newNote);
        }
    }

    private TransactionEdit() {}

    /** سطر السجل لهالتعديل، أو null إذا ما تغيّر إشي (فما في داعي نحفظ). */
    public static Entry entry(long oldAmount, long newAmount, String oldNote, String newNote, long now) {
        String o = oldNote == null ? "" : oldNote.trim();
        String n = newNote == null ? "" : newNote.trim();
        if (oldAmount == newAmount && o.equals(n)) return null;
        return new Entry(now, oldAmount, newAmount, o, n);
    }

    /** قديش بيتغيّر رصيد الزبون (موجب: بيزيد، سالب: بيقل). */
    public static long balanceDiff(TransactionType type, long oldAmount, long newAmount) {
        return BalanceCalculator.editDiff(type, oldAmount, newAmount);
    }

    /** المبلغ لما انعملت الحركة: من أول تعديل بالسجل، أو المبلغ الحالي إذا ما في تعديلات. */
    public static long originalAmount(List<Entry> history, long currentAmount) {
        if (history == null || history.isEmpty()) return currentAmount;
        Entry first = history.get(0);
        for (Entry e : history) if (e.at < first.at) first = e;
        return first.oldAmount;
    }

    /** السجل للعرض: الأحدث فوق. */
    public static List<Entry> newestFirst(List<Entry> history) {
        List<Entry> out = new ArrayList<>(history == null ? Collections.emptyList() : history);
        Collections.sort(out, (a, b) -> Long.compare(b.at, a.at));
        return out;
    }
}
