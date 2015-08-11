package com.example.iki.dropboxcrypt;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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
import java.security.Key;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    ///////////////////////////////////////////////////////////////////////////
    //                      Your app-specific settings.                      //
    ///////////////////////////////////////////////////////////////////////////

    // Replace this with your app key and secret assigned by Dropbox.
    // Note that this is a really insecure way to do this, and you shouldn't
    // ship code which contains your key & secret in such an obvious way.
    // Obfuscation is good.
    private static final String APP_KEY = "7b0wzjaoe09sv94";
    private static final String APP_SECRET = "4gqjsjs86qojulo";
    private static final int DBX_CHOOSER_REQUEST = 0;
    private static final int REQUEST_CODE = 6384;

    ///////////////////////////////////////////////////////////////////////////
    //                      End app-specific settings.                       //
    ///////////////////////////////////////////////////////////////////////////


    // You don't need to change these, leave them alone.
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    private static final boolean USE_OAUTH1 = false;

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
    private  byte[] bmFile;
    private Key publicKey = null;
    private Key privateKey = null;

    private DbxChooser mChooser;

    private String mCameraFileName;
    private final String FILE_DB_DIR = "/Apps/DropBoxCrypt/";

    private Intent mPreferences;
    private DataEncryption mEncrypter;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ///////////////////   da se koregira /////////////////
         if (savedInstanceState != null) {
            mCameraFileName = savedInstanceState.getString("mCameraFileName");
        }
        ///////////////////////////////////////////////////


        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);

        setContentView(R.layout.activity_main);

        checkAppKeySetup();

        ////////////////////////////////////
        // button for connect to the dropbox

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

        //////////////
        // DbxChooser

        mChooser = new DbxChooser(APP_KEY);
        // This is the button to browse into Dropbox
        mRetrieve = (Button)findViewById(R.id.retrieve_button);
        mRetrieve.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //DownloadRandomPicture download = new DownloadRandomPicture(DBRoulette.this, mApi, PHOTO_DIR, mImage);
                //download.execute();
                mChooser.forResultType(DbxChooser.ResultType.DIRECT_LINK).launch(MainActivity.this, DBX_CHOOSER_REQUEST);
            }
        });


        ////////////////////////////////
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




///////////////////////////////////////////////////////////////
    // This is what gets called on finishing a media piece to import


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case DBX_CHOOSER_REQUEST:
                    if (resultCode == Activity.RESULT_OK) {
                        DbxChooser.Result result = new DbxChooser.Result(data);
                        //Log.d("main", "Link to selected file: " + result.getLink());

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
                       // Toast.makeText(MainActivity.this,"Name Selected: " +uri, Toast.LENGTH_LONG).show();

                        mName.setText(nameFile, TextView.BufferType.NORMAL);

                        DownloadFile download = new DownloadFile(MainActivity.this, mApi, FILE_DB_DIR,nameFile, publicKey);
                        download.execute();


                    } else {
                        //Log.i(TAG, "Failed or was cancelled by the user.");
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
                        Log.i(TAG, "Uri = " + uri.toString());
                        try {
                            // Get the file path from the URI
                            path = FileUtils.getPath(this, uri);
                            //Toast.makeText(MainActivity.this,"File Selected: " + path, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            //Log.e("FileSelectorTestActivity", "File select error", e);
                        }
                    }


                    if(uri != null){
                            //// encryptVideo()
                        Log.e("!!!!! PATH TEST", path);
                        String filename = path;
                        Toast.makeText(this, path, Toast.LENGTH_LONG).show();
                        //String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                        //String filename = filePath + "/cleartext.txt";  //"/20150729_170612.jpg";
                        int pos = filename.lastIndexOf('.');
                        //String filenameDec = filename.substring(0,pos) + "-dec." + filename.substring(pos+1);
                        String filenameEnc = filename.substring(0,pos) + "-enc." + filename.substring(pos+1);

                        mEncrypter = new DataEncryptionCrypto();

                        //set blocksize
                        int blocksize = 128;
                        mEncrypter.setBlocksize(blocksize);

                        mEncrypter.encryptFile(filenameEnc, filename);
                        //mEncrypter.decryptFile(filenameDec,filenameEnc);


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

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                storeAuth(session);
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
               // Log.i(TAG, "Error authenticating", e);
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
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
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
     * time (which is not to be done, ever).
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
        // you're still using OAuth 1.
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
        if (APP_KEY.startsWith("CHANGE") ||
                APP_SECRET.startsWith("CHANGE")) {
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
           // mImage.setImageDrawable(null);
        }
    }

    private void showChooser() {
        // use the GET_CONTENT intent from the utility class
        Intent target = FileUtils.createGetContentIntent();
        //create the chooser Intent
        Intent intent = Intent.createChooser(
                target, "HOME");
        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            // The reason for the existence of aFileChooser
        }
    }


}


/*
/////////////RSA

// Generate key pair for 1024-bit RSA encryption and decryption
Key publicKey = null;
Key privateKey = null;
try {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        KeyPair kp = kpg.genKeyPair();
        publicKey = kp.getPublic();
        privateKey = kp.getPrivate();
        } catch (Exception e) {
        Log.e(TAG, "RSA key pair error");
        }

        // Encode the original data with RSA private key
        byte[] encodedBytes = null;
        //byte[]  data = Files.readAllBytes(mFile.getPath());
        try {
        Cipher c = Cipher.getInstance("RSA");
        c.init(Cipher.ENCRYPT_MODE, privateKey);
        encodedBytes = c.doFinal(bmFile);
        } catch (Exception e) {
        Log.e(TAG, "RSA encryption error");
        }
//TextView tvencoded = (TextView) findViewById(R.id.tvencoded);
//tvencoded.setText("[ENCODED]:\n" +  Base64.encodeToString(encodedBytes, Base64.DEFAULT) + "\n");
*/
