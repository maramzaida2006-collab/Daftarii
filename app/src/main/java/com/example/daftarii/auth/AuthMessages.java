package com.example.daftarii.auth;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.daftarii.R;
import com.example.daftarii.logic.AuthError;
import com.example.daftarii.logic.Validators;

/** بيحوّل الأخطاء لنصوص من strings.xml. ما في نص مكتوب مباشرة بالكود. */
public final class AuthMessages {

    private AuthMessages() {}

    @StringRes
    public static int of(AuthError error) {
        switch (error) {
            case INVALID_EMAIL:         return R.string.error_email_invalid;
            case WRONG_CREDENTIALS:     return R.string.auth_error_wrong_credentials;
            case USER_DISABLED:         return R.string.auth_error_user_disabled;
            case EMAIL_IN_USE:          return R.string.auth_error_email_in_use;
            case WEAK_PASSWORD:         return R.string.auth_error_weak_password;
            case TOO_MANY_REQUESTS:     return R.string.auth_error_too_many;
            case NETWORK:               return R.string.auth_error_network;
            case OPERATION_NOT_ALLOWED: return R.string.auth_error_not_allowed;
            default:                    return R.string.auth_error_unknown;
        }
    }

    /** رسالة خطأ حقل الإيميل، أو null إذا صحيح. */
    @Nullable @StringRes
    public static Integer emailError(Validators.Result r) {
        switch (r) {
            case OK:    return null;
            case EMPTY: return R.string.error_email_empty;
            default:    return R.string.error_email_invalid;
        }
    }

    @Nullable @StringRes
    public static Integer passwordError(Validators.Result r) {
        switch (r) {
            case OK:        return null;
            case EMPTY:     return R.string.error_password_empty;
            default:        return R.string.error_password_short;
        }
    }

    @Nullable @StringRes
    public static Integer confirmationError(Validators.Result r) {
        switch (r) {
            case OK:    return null;
            case EMPTY: return R.string.error_password_empty;
            default:    return R.string.error_password_mismatch;
        }
    }

    @Nullable @StringRes
    public static Integer shopNameError(Validators.Result r) {
        switch (r) {
            case OK:    return null;
            case EMPTY: return R.string.error_shop_name_empty;
            default:    return R.string.error_shop_name_long;
        }
    }

    /** مبلغ حركة (إضافة أو تعديل). */
    @Nullable @StringRes
    public static Integer amountError(Validators.Result r) {
        switch (r) {
            case OK:           return null;
            case EMPTY:        return R.string.error_amount_empty;
            case NOT_POSITIVE: return R.string.error_amount_not_positive;
            case TOO_LARGE:    return R.string.error_amount_too_large;
            default:           return R.string.error_amount_invalid;
        }
    }
}
