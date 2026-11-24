package com.example.daftarii.util;

import androidx.annotation.Nullable;

/**
 * قيمة بتنقرأ مرة وحدة بس (رسالة Toast، أو انتقال لشاشة).
 * بدونها، لما الشاشة تلف (rotate) الـ LiveData بيرجع يبعت نفس الرسالة.
 */
public class Event<T> {

    private final T content;
    private boolean handled = false;

    public Event(T content) {
        this.content = content;
    }

    /** بيرجّع القيمة أول مرة، وبعدين null. */
    @Nullable
    public T getIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }

    public T peek() {
        return content;
    }
}
