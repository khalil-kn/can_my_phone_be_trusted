package com.khalilknaissi.can_my_phone_be_trusted.viewmodel;

/**
 * A value meant to be used once, like a toast message. Without this, rotating the
 * phone would show the same message again, because LiveData replays its last value.
 */
public class Event<T> {
    private final T value;
    private boolean handled;

    public Event(T value) {
        this.value = value;
    }

    /** Returns the value the first time, then null. */
    public T take() {
        if (handled) return null;
        handled = true;
        return value;
    }
}
