INTRODUCTION

DropBoxCrypt application is used for uploading encrypted files on our dropbox account and decrypts them when they are downloaded(if the fingerprint authentication pass). 

Scenario for using: with the main window we connect the application with our Dropbox account. 
If we want to upload file, we choose Upload button, search through the file system and choose the right one. After that, the application makes encryption to that file and sends it like that on the dropbox. At the same time, a .key file is made for future decryption and is stored at the device storage. 
If we want to download file from the dropbox we choose Download button. At this stage, the application shows another view for fingerprint authentication and prompt the user to touch the fingerprint sensor. Then the application starts listening for a fingerprint. When the fingerprint is available, the application check if the fingerprint or the system password exist (before using of that application, the fingerprint must be registered at the device with a backup password). The fingerprint dialog also allows using the backup password if it is users choose or in a case of a hardware error. After successful authentication, the user is redirected to the dropbox storage. From the /Apps/DropBoxCrypt/ folder we choose the wanted file. The file is downloaded to the phone storage, the appropriate .key file is found and the decryption is done.


MORE INFORMATION

The application is consists of few activities:

MainActivity - this activity link the whole application together and give the application the basic view. Here the connection with dropbox is done, using OAuth protocol with OAuth 2 and OAuth 1 access token. In the onActivityResult() method, in case that we choose file from dropbox, we authenticated with our fingerprint in the onRequestPermissionsResult() method, which handle the permissions. After permission is granted, we make DownloadFile object, which is used to download the selected file. In case that we choose file from the phone storage, we make DataEncryptionCrypto object to encrypt the chosen file, and UploadFile object to send that file to the dropbox.

DownloadFile - is class for downloading the selected file from the dropbox storage. When we reach the file, we create DataEncryptionCrypto object to decryption the file

UploadFile - is class for uploading file into the dropbox (encryption is already made before in MainActivity)

DataEncryptionCrypto - contains encryptFile() and decryptFile() methods. For its purpose we use javax.crypto with its Chiper and SecretKey classes with AES 128 

KeyStoreUtils - is used for manage the keys, and is compose of generateKey(), saveKey() and loadKey() methods.

FingerprintAuthenticationDialogFragment - handles the dialog which uses fingerprint APIs to authenticate the user

FingerprintUiHelper - small helper class to manage text/icon around fingerprint authentication UI

FingerprintModule - Dagger module for Fingerprint APIs

InjectedApplication - class of the sample which holds the ObjectGraph in Dagger and enables dependency injection


The application also have 2 subprojects:
aFileChooser - to choose files from the phone storage
dropboxChooserSDK - for searching and selecting files into the dropbox storage

More information about dropbox API and OAuth:
https://www.dropbox.com/developers/core/docs
https://www.dropbox.com/developers/reference/devguide
https://www.dropbox.com/develpers/reference/oauthguide


FINGERPRINT API

Android API 23 offers to authenticate users by using their fingerprint scans, which are carefully contained within secure hardware on supported devices. This guards against malicious actors, ensuring that users can safely use their fingerprint, even in untrusted applications. Android also provides protection for application developers, providing assurances that a user’s fingerprint has been positively identified before providing access to secure data or resources. This API provides cryptographic-level security for both offline data and online interactions. When a user activates their fingerprint reader, they’re unlocking a hardware-backed cryptographic vault. The developer can choose what type of key is stored in that vault. In this application, symmetric keys are used.
The fingerprint API is used in conjunction with the Android Key system. This system lets us to store cryptographic keys in a container to make it more difficult to extract from the device. Once keys are in the keystore, they can be used for cryptographic operations with the key material remaining non-exportable.
 

Using fingerprint in random application:

First is created a symmetric key in the Android Key Store using KeyGenerator which can only be used after the user has authenticated with fingerprint and pass a KeyGenParameterSpec. By setting KeyGenParameterSpec.Builder.setUserAuthenticationRequired to true, we require the user to authenticate with a fingerprint.
Authenticate the users via fingerprint scan is done with an instance of the new FingerprintManager class. To start listening to a fingerprint on the fingerprint sensor is done by calling FingerprintManager.authenticate with a Cipher initialized with the symmetric key created.  
Once the fingerprint (or password) is verified, the FingerprintManager.AuthenticationCallback#onAuthenticationSucceeded() callback is called.

For using fingerprint authentication in an application, must be added the USE_FINGERPRINT permission in the android manifest:
		<uses-permission  android:name="android.permission.USE_FINGERPRINT"/>


Testing this feature in the android emulator:

1. Install Android SDK Tools Revision 24.3
2. Enroll a new fingerprint in the emulator by going to Settings > Security > Fingerprint
3. Use an emulator to emulate fingerprint touch events with the following command:
			adb -e emu finger touch <finger_id>
			e.g. adb -s emulator-5554 emu finger touch 1
			
	On Windows, run:
			telnet 127.0.0.1 <emulator-id> 
			finger touch <finger_id>



More information:
http://developer.android.com/about/versions/marshmallow/android-6.0.html
http://developer.android.com/training/articles/keystore.html#SecurityFeatures
http://developer.android.com/reference/android/security/KeyChain.html
http://developer.android.com/reference/android/hardware/fingerprint/FingerprintManager.html
https://developer.android.com/reference/javax/crypto/KeyGenerator.html
https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html




BUILD INSTRUCTIONS

- this application is developed in Android Studio v.1.3.2, compatible with android devices using API 23
- add sFileChooser and dropboxChooserSDK as modules to the main project
- add the following libraries:
	-dropbox-android-sdk-1.6.3.jar
	-json_simple-1.1.jar
	-commons-codec-1.7.jar
	-commons-io-2.4.jar
	-javax-inject.jar
	-dragger-1.22.jar
	

