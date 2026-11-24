package com.example.daftarii.logic;

/**
 * أخطاء الحساب اللي بتهم المستخدم. كود الخطأ من Firebase بيتحوّل لواحد منهم،
 * والواجهة بتحوّل كل واحد لرسالة من strings.xml.
 */
public enum AuthError {
    INVALID_EMAIL,
    WRONG_CREDENTIALS,
    USER_DISABLED,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    TOO_MANY_REQUESTS,
    NETWORK,
    OPERATION_NOT_ALLOWED,
    UNKNOWN;

    /** بيحوّل كود FirebaseAuthException.getErrorCode() لخطأ مفهوم. */
    public static AuthError fromFirebaseCode(String code) {
        if (code == null) return UNKNOWN;
        switch (code) {
            case "ERROR_INVALID_EMAIL":
                return INVALID_EMAIL;
            // مع حماية تعداد الإيميلات بـ Firebase، الإيميل الغلط وكلمة السر الغلط بيرجعوا نفس الكود
            case "ERROR_WRONG_PASSWORD":
            case "ERROR_USER_NOT_FOUND":
            case "ERROR_INVALID_CREDENTIAL":
                return WRONG_CREDENTIALS;
            case "ERROR_USER_DISABLED":
                return USER_DISABLED;
            case "ERROR_EMAIL_ALREADY_IN_USE":
            case "ERROR_CREDENTIAL_ALREADY_IN_USE":
                return EMAIL_IN_USE;
            case "ERROR_WEAK_PASSWORD":
                return WEAK_PASSWORD;
            case "ERROR_TOO_MANY_REQUESTS":
                return TOO_MANY_REQUESTS;
            case "ERROR_NETWORK_REQUEST_FAILED":
                return NETWORK;
            case "ERROR_OPERATION_NOT_ALLOWED":
            case "ERROR_ADMIN_RESTRICTED_OPERATION":
                return OPERATION_NOT_ALLOWED;
            default:
                return UNKNOWN;
        }
    }
}
