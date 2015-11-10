package com.example.iki.dropboxcrypt;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.chooser.android.DbxChooser;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    ///////////////////////////////////////////////////////////////////////////
    //                      Your app-specific settings for Dropbox enter.                      //
    ///////////////////////////////////////////////////////////////////////////

    // Replace this with your app key and secret assigned by Dropbox.
    private static final String APP_KEY = "7b0wzjaoe09sv94";
    private static final String APP_SECRET = "4gqjsjs86qojulo";
    private static final int DBX_CHOOSER_REQUEST = 0;
    private static final int REQUEST_CODE = 6384;

    ///////////////////////////////////////////////////////////////////////////
    //                      End app-specific settings.                       //
    ///////////////////////////////////////////////////////////////////////////


    // Needed for dropbox
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    private static final boolean USE_OAUTH1 = false;
    private final String FILE_DB_DIR = "/Apps/DropBoxCrypt/";

    DropboxAPI<AndroidAuthSession> mApi;
    private boolean mLoggedIn;

    // Android widgets
    private Button mSubmit;
    private LinearLayout mDisplay;
    private LinearLayout mDisplayFileInformation;
    private Button mUpload;
    private Button mRetrieve;
    private TextView mLink;
    private TextView mName;
    private Key publicKey = null;

    private DbxChooser mChooser;

    private Intent mPreferences;
    private DataEncryption mEncrypter;

    // Needed fot fingerprint
    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    //alias for key in the Android key store
    private static final String KEY_NAME = "my_key";
    private static final int FINGERPRINT_PERMISSION_REQUEST_CODE = 50;

    @Inject KeyguardManager mKeyguardManager;
    @Inject FingerprintManager mFingerprintManager;
    @Inject FingerprintAuthenticationDialogFragment mFragment;
    @Inject KeyStore mKeyStore;
    @Inject KeyGenerator mKeyGenerator;
    @Inject Cipher mCipher;
    @Inject SharedPreferences mSharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((InjectedApplication) getApplication()).inject(this);

        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);

        setContentView(R.layout.activity_main);

        checkAppKeySetup();

        // button for connection to the dropbox
        mSubmit = (Button)findViewById(R.id.auth_button);
        mSubmit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // This logs you out if you're logged in, or vice versa
                if (mLoggedIn) {
                    logOut();
                } else {
                    // Start the remote authentication
                    if (USE_OAUTH1) {
                        mApi.getSession().startAuthentication(MainActivity.this);
                    } else {
                        mApi.getSession().startOAuth2Authentication(MainActivity.this);
                    }
                }
            }
        });

        //layout for download/upload buttons for logged users
        mDisplay = (LinearLayout)findViewById(R.id.logged_in_display);
        mDisplayFileInformation = (LinearLayout)findViewById(R.id.file_information);


        // fingerprint
        // to check if a permission has not been previously granted and request it,
        // passing in an array of permission strings and a custom int request code for keeping track of application logic flow
        // handling retrieve button in onRequestPermissionsResult() method
        requestPermissions(new String[]{android.Manifest.permission.USE_FINGERPRINT}, FINGERPRINT_PERMISSION_REQUEST_CODE);

        // button for upload
        mUpload = (Button)findViewById(R.id.upload_button);
        mUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChooser();
            }
        });

        // Display the proper UI state if logged in or not
        setLoggedIn(mApi.getSession().isLinked());

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case DBX_CHOOSER_REQUEST:
                    // If the file selection into the dropbox was successful
                    if (resultCode == Activity.RESULT_OK) {
                        DbxChooser.Result result = new DbxChooser.Result(data);

                        Uri uri = result.getLink();

                        String path = "";
                        mLink = (TextView) findViewById(R.id.link_label);
                        mName = (TextView)findViewById(R.id.name_label);
                        //print link from the file
                        if(uri == null) {
                            mLink.setText("", TextView.BufferType.NORMAL);
                        }else{
                            mLink.setText(uri.toString(),TextView.BufferType.NORMAL);
                            mLink.setMovementMethod(LinkMovementMethod.getInstance());
                        }

                        String nameFile = String.valueOf(result.getName());
                        mName.setText(nameFile, TextView.BufferType.NORMAL);
                        DownloadFile download = new DownloadFile(MainActivity.this, mApi, FILE_DB_DIR,nameFile, publicKey);
                        download.execute();

                    } else {
                        // Failed or was cancelled by the user.
                    }
                break;

            case REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    Uri uri = null;
                    String path = "";
                    if (data != null) {
                        // Get the URI of the selected file
                        uri = data.getData();
                        try {
                            // Get the file path from the URI
                            path = FileUtils.getPath(this, uri);
                        } catch (Exception e) {
                            //Failed or was cancelled by the user.
                        }
                    }

                    if(uri != null){
                        // encrypt the file
                        String filename = path;
                        int pos = filename.lastIndexOf('.');
                        String filenameEnc = filename.substring(0,pos) + "-enc." + filename.substring(pos+1);

                        mEncrypter = new DataEncryptionCrypto();
                        //set blocksize
                        int blocksize = 128;
                        mEncrypter.setBlocksize(blocksize);
                        mEncrypter.encryptFile(filenameEnc, filename);

                        File mFile = new File(filenameEnc);

                        UploadFile upload = new UploadFile(this,mApi, FILE_DB_DIR, mFile);
                        upload.execute();
                    }
                }else {
                    //Log.w(TAG, "Unknown Activity Result from mediaImport: " + resultCode);
                }
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidAuthSession session = mApi.getSession();

        // The next part must be inserted in the onResume() method of the activity from which session.startAuthentication() was called,
        // so that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in the app for later use
                storeAuth(session);
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
                Log.i(TAG, "Error authenticating", e);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }


    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each time.
     */
    private void loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

        if (key.equals("oauth2:")) {
            // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
            session.setOAuth2AccessToken(secret);
        } else {
            // Still support using old OAuth 1 tokens.
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time.
     */
    private void storeAuth(AndroidAuthSession session) {
        // Store the OAuth 2 access token, if there is one.
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.commit();
            return;
        }
        // Store the OAuth 1 access token, if there is one.  This is only necessary if
        // we're still using OAuth 1.
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
            edit.commit();
            return;
        }
    }

    private void checkAppKeySetup() {
        // Check to make sure that we have a valid app key
        if (APP_KEY.startsWith("CHANGE") || APP_SECRET.startsWith("CHANGE")) {
            showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DropBoxCrypt ap before trying it.");
            finish();
            return;
        }
    }

    private void logOut() {
            // Remove credentials from the session
            mApi.getSession().unlink();
            // Clear our stored keys
            clearKeys();
            // Change UI state to display logged out version
            setLoggedIn(false);
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean loggedIn) {
        mLoggedIn = loggedIn;
        if (loggedIn) {
            mSubmit.setText("Disconnect from dropbox");
            mDisplay.setVisibility(View.VISIBLE);
            mDisplayFileInformation.setVisibility(View.VISIBLE);
        } else {
            mSubmit.setText("Connect with dropbox");
            mDisplay.setVisibility(View.GONE);
            mDisplayFileInformation.setVisibility(View.GONE);
        }
    }

    private void showChooser() {
        // use the GET_CONTENT intent from the utility class
        Intent target = FileUtils.createGetContentIntent();
        //create the chooser Intent
        Intent intent = Intent.createChooser(target, "HOME");
        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            // The reason for the existence of aFileChooser
        }
    }



    // Fingerprint Authentication

    //When the user is done with the dialogs, onRequestPermissionsResult is called
    //This is where we either start our feature or handle the situation where the user has denied one or more permissions.

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] state) {
        if (requestCode == FINGERPRINT_PERMISSION_REQUEST_CODE && state[0] == PackageManager.PERMISSION_GRANTED) {

            // This is the button to browse into Dropbox
            mRetrieve = (Button)findViewById(R.id.retrieve_button);

            if (!mKeyguardManager.isKeyguardSecure()) {
                // Show a message that the user hasn't set up a fingerprint or lock screen.
                showToast("Secure lock screen hasn't set up.\n" + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint");
                return;
            }
            if (!mFingerprintManager.hasEnrolledFingerprints()) {
                // Message when no fingerprints are registered.
                showToast("Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint");
                return;
            }

            createKey();

            mRetrieve.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Setting up the crypto object
                    // The object will be authenticated by use of the fingerprint
                    if (initCipher()) {
                        // Show the fingerprint dialog.
                        // The user can choose between useing the fingerprint with crypto or using a server-side verified password.
                        mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                        boolean useFingerprintPreference = mSharedPreferences.getBoolean(getString(R.string.use_fingerprint_to_authenticate_key), true);
                        if (useFingerprintPreference) {
                            mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
                        } else {
                            mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
                        }
                        mFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
                    } else {
                        // This happens if the lock screen has been disabled or a fingerprint got
                        // enrolled. Thus show the dialog to authenticate with their password first
                        // and ask the user if they want to authenticate with fingerprints in the
                        // future
                        mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                        mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
                        mFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
                    }
                }
            });
        }
    }

    /**
     * Initialize the Cipher instance with the created key in the createKey() method
     *
     * return true if initialization is successful
     * return false if the lock screen has been disabled or reset after the key was generated,
     * or if a fingerprint got enrolled after the key was generated
     */
    private boolean initCipher() {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            mCipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }


    public void onRetrieve(boolean withFingerprint) {
        if (withFingerprint) {
            // If authentication happened with fingerprint, verify that using cryptography
            tryEncrypt();
        } else {
            // Authentication happened with backup password
            showConfirmation(null);
        }
    }


    /**
     * Encrypt some data with the generated key in createKey() method, which is
     * only works if the user has just authenticated via fingerprint.
     */
    private void tryEncrypt() {
        try {
            byte[] encrypted = mCipher.doFinal(SECRET_MESSAGE.getBytes());
            showConfirmation(encrypted);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            showToast("Failed to encrypt the data with the generated key. "
                    + "Retry");
            Log.e(TAG, "Failed to encrypt the data with the generated key." + e.getMessage());
        }
    }


    // Show confirmation, if fingerprint was used go to dropbox chooser
    private void showConfirmation(byte[] encrypted) {
        if(encrypted != null) {
            showToast("Fingerprint authentication successfully done");

            // DbxChooser
            mChooser = new DbxChooser(APP_KEY);
            mChooser.forResultType(DbxChooser.ResultType.DIRECT_LINK).launch(MainActivity.this, DBX_CHOOSER_REQUEST);
        }
    }

    public void createKey() {
        // The enrolling flow for fingerprint. This is where we ask the user to set up fingerprint
        // for our flow. Use of keys is necessary if we need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains in the constructor of the Builder
            mKeyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                            // Require the user to authenticate with a fingerprint to authorize every use of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}

