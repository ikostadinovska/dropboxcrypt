package com.example.iki.dropboxcrypt;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.preference.PreferenceManager;
import android.security.keystore.KeyProperties;
import android.view.inputmethod.InputMethodManager;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import dagger.Module;
import dagger.Provides;

/**
 * Dagger module for Fingerprint APIs.
 */
@Module(
        library = true,
        injects = {MainActivity.class}
)
public class FingerprintModule {

    private final Context mContext;

    public FingerprintModule(Context context) {
        mContext = context;
    }

    @Provides
    public Context providesContext() {
        return mContext;
    }

    @Provides
    public FingerprintManager providesFingerprintManager(Context context) {
        return context.getSystemService(FingerprintManager.class);
    }

    @Provides
    public KeyguardManager providesKeyguardManager(Context context) {
        return context.getSystemService(KeyguardManager.class);
    }

    @Provides
    public KeyStore providesKeystore() {
        try {
            return KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
    }

    @Provides
    public KeyGenerator providesKeyGenerator() {
        try {
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }
    }

    @Provides
    public Cipher providesCipher(KeyStore keyStore) {
        try {
            return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }
    }

    @Provides
    public InputMethodManager providesInputMethodManager(Context context) {
        return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Provides
    public SharedPreferences providesSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
