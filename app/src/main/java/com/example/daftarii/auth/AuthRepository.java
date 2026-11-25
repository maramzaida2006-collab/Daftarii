package com.example.daftarii.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.daftarii.logic.AuthError;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * المكان الوحيد بالتطبيق اللي بيحكي مع Firebase Authentication (ومع ملف المستخدم users/{uid}).
 * الشاشات ما بتستخدم FirebaseAuth مباشرة، بتمرّ من هون عن طريق الـ ViewModel.
 */
public class AuthRepository {

    /** نتيجة عملية: يا نجحت يا رجّعت خطأ مفهوم. */
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(@NonNull AuthError error);
    }

    private final FirebaseAuth auth;

    public AuthRepository() {
        this(FirebaseAuth.getInstance());
    }

    public AuthRepository(FirebaseAuth auth) {
        this.auth = auth;
    }

    // ---------------- حالة المستخدم الحالي ----------------

    @Nullable
    public FirebaseUser currentUser() {
        return auth.getCurrentUser();
    }

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    public boolean isGuest() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && user.isAnonymous();
    }

    public boolean isEmailVerified() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && !user.isAnonymous() && user.isEmailVerified();
    }

    @Nullable
    public String currentEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    @Nullable
    public String currentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /** اسم المحل (انحفظ كـ displayName وقت التسجيل). null إذا مش موجود. */
    @Nullable
    public String currentDisplayName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return null;
        String name = user.getDisplayName();
        return name == null || name.trim().isEmpty() ? null : name.trim();
    }

    /** وقت إنشاء الحساب، من سيرفر Firebase. للزائر هو بداية الفترة التجريبية. 0 إذا مش معروف. */
    public long accountCreatedAt() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return 0;
        FirebaseUserMetadata metadata = user.getMetadata();
        return metadata != null ? metadata.getCreationTimestamp() : 0;
    }

    // ---------------- العمليات ----------------

    /** تسجيل دخول. النتيجة true إذا الإيميل مفعّل، و false إذا لسا لازم يتفعّل. */
    public void login(String email, String password, Callback<Boolean> callback) {
        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    callback.onSuccess(user != null && user.isEmailVerified());
                })
                .addOnFailureListener(e -> callback.onError(toAuthError(e)));
    }

    /**
     * إنشاء حساب: بيعمل الحساب، وبيحفظ اسم المحل، وبيبعت رابط التفعيل.
     * المستخدم بيضل مسجّل دخول (بس مش مفعّل) عشان شاشة التفعيل تقدر تفحص.
     */
    public void register(String shopName, String email, String password, Callback<Void> callback) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError(AuthError.UNKNOWN);
                        return;
                    }
                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(shopName.trim())
                            .build();
                    // إذا حفظ الاسم فشل، مش مشكلة كبيرة: المهم رابط التفعيل
                    user.updateProfile(profile).addOnCompleteListener(ignored ->
                            user.sendEmailVerification()
                                    .addOnSuccessListener(v -> callback.onSuccess(null))
                                    .addOnFailureListener(e -> callback.onError(toAuthError(e))));
                })
                .addOnFailureListener(e -> callback.onError(toAuthError(e)));
    }

    /** إعادة إرسال رابط التفعيل. */
    public void resendVerificationEmail(Callback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(AuthError.UNKNOWN);
            return;
        }
        user.sendEmailVerification()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(toAuthError(e)));
    }

    /** بيجيب حالة المستخدم من جديد من السيرفر، وبيرجّع true إذا صار الإيميل مفعّل. */
    public void reloadAndCheckVerified(Callback<Boolean> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(AuthError.UNKNOWN);
            return;
        }
        user.reload()
                .addOnSuccessListener(v -> {
                    FirebaseUser refreshed = auth.getCurrentUser();
                    callback.onSuccess(refreshed != null && refreshed.isEmailVerified());
                })
                .addOnFailureListener(e -> callback.onError(toAuthError(e)));
    }

    /**
     * رابط إعادة تعيين كلمة السر. مع حماية تعداد الإيميلات بـ Firebase،
     * العملية بتنجح حتى لو الإيميل مش مسجّل، فالرسالة للمستخدم لازم تكون عامة.
     */
    public void sendPasswordReset(String email, Callback<Void> callback) {
        auth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(toAuthError(e)));
    }

    /** الدخول كزائر. لازم Anonymous يكون مفعّل بـ Firebase Console. */
    public void signInAsGuest(Callback<Void> callback) {
        auth.signInAnonymously()
                .addOnSuccessListener(r -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(toAuthError(e)));
    }

    /**
     * لشاشة التفعيل ("تغيير البريد"): بيمسح الحساب اللي لسا ما تفعّل وبيطلّع المستخدم.
     * هيك الإيميل الغلط ما بيضل محجوز.
     */
    public void deleteUnverifiedAccount(Callback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.isAnonymous() || user.isEmailVerified()) {
            auth.signOut();
            callback.onSuccess(null);
            return;
        }
        user.delete().addOnCompleteListener(task -> {
            auth.signOut();
            callback.onSuccess(null);
        });
    }

    /**
     * ملف المستخدم بـ Firestore: users/{uid}. حسابي وكشف الـ PDF بيقرأوا منه اسم المحل والإيميل.
     * بنحفظه بعد الدخول وإنشاء الحساب ودخول الزائر. merge: ما بيمسح حقول تانية (متل تعديلات حسابي).
     * ما بيوقف الدخول إذا فشل: الإنترنت لما يرجع Firestore بيكمّل الكتابة لحاله.
     *
     * @param shopName اسم المحل إذا معروف (وقت التسجيل). null: بناخده من displayName.
     * @param isNew    حساب جديد (تسجيل أو زائر): بنحفظ createdAt.
     */
    public void saveUserProfile(@Nullable String shopName, boolean isNew) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        long now = System.currentTimeMillis();
        Map<String, Object> doc = new HashMap<>();
        doc.put("uid", user.getUid());
        doc.put("isAnonymous", user.isAnonymous());
        doc.put("emailVerified", user.isEmailVerified());
        doc.put("lastLoginAt", now);
        if (isNew) doc.put("createdAt", now);
        if (user.getEmail() != null) doc.put("email", user.getEmail());
        String name = shopName != null ? shopName.trim() : user.getDisplayName();
        if (name != null && !name.trim().isEmpty()) {
            doc.put("shopName", name.trim());
            doc.put("displayName", name.trim());
        }
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .set(doc, SetOptions.merge());
    }

    public void signOut() {
        auth.signOut();
    }

    // ---------------- تحويل الأخطاء ----------------

    @NonNull
    static AuthError toAuthError(@Nullable Exception e) {
        if (e instanceof FirebaseNetworkException) return AuthError.NETWORK;
        if (e instanceof FirebaseTooManyRequestsException) return AuthError.TOO_MANY_REQUESTS;
        if (e instanceof FirebaseAuthException) {
            return AuthError.fromFirebaseCode(((FirebaseAuthException) e).getErrorCode());
        }
        return AuthError.UNKNOWN;
    }
}
