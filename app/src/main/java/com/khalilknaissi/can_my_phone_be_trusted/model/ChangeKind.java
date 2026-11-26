package com.khalilknaissi.can_my_phone_be_trusted.model;

/** Every kind of difference the app can detect between the baseline and a check. */
public enum ChangeKind {
    APP_INSTALLED(Category.APPS, true),
    APP_REMOVED(Category.APPS, false),
    APP_UPDATED(Category.APPS, true),
    APP_DOWNGRADED(Category.APPS, true),
    SIGNATURE_CHANGED(Category.APPS, false),
    SIDELOADED(Category.APPS, true),
    PERMISSION_GRANTED(Category.PERMISSIONS, false),
    PERMISSION_REVOKED(Category.PERMISSIONS, false),
    SPECIAL_ACCESS_ON(Category.PERMISSIONS, false),
    SPECIAL_ACCESS_OFF(Category.PERMISSIONS, false),
    SETTING_CHANGED(Category.SETTINGS, false);

    public enum Category { APPS, PERMISSIONS, SETTINGS }

    public final Category category;
    /** True when Android records the exact time (installs, updates). Otherwise we only know it happened between two checks. */
    public final boolean exact;

    ChangeKind(Category category, boolean exact) {
        this.category = category;
        this.exact = exact;
    }
}
