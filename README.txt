INTRODUCTION

DropBoxCrypt application is used for uploading encrypted files on our dropbox account and decrypts them when they are downloaded. 

Scenario for using: with the main window we connect the application with our Dropbox account. 
If we want to upload file, we choose Upload button, search through the file system and choose the right one. After that, the application makes encryption to that file and sends it like that on the dropbox. At the same time, a .key file is made for future decryption and is stored at the device storage. 
If we want to download file from the dropbox we choose Download button, which redirect us to the dropbox storage. From the /Apps/DropBoxCrypt/ folder we choose the wanted file. The file is downloaded to the phone storage, the appropriate .key file is found and the decryption is done.

MORE INFORMATION

The application is consists of few activities:

MainActivity – this activity link the whole application together and give the application the basic view. Here the connection with dropbox is done, using OAuth protocol with OAuth 2 and OAuth 1 access token. In the onActivityResult() method, in case that we choose file from dropbox, we make DownloadFile object, which is used to download the selected file. In case that we choose file from the phone storage, we make DataEncryptionCrypto object to encrypt the choosen file, and UploadFile object to send that file to the dropbox.

DownloadFile – is class for downloading the selected file from the dropbox storage. When we reach the file, we create DataEncryptionCrypto object to decryption the file

UploadFile – is class for uploading file into the dropbox (encryption is already made before in MainActivity)

DataEncryptionCrypto – contains encryptFile() and decryptFile() methods. For its purpose we use javax.crypto with its Chiper and SecretKey classes with AES 128 (TODO: stronger encryption with AES/CBC/PKCS5Padding, IV, MAC or HMAC)

KeyStoreUtils – is used for manage the keys, and is compose of generateKey(), saveKey() and loadKey() methods.


The application also have 2 subprojects:
aFileChooser – to choose files from the phone storage
dropboxChooserSDK – for searching and selecting files into the dropbox storage 

More information about dropbox API and OAuth:
https://www.dropbox.com/developers/core/docs
https://www.dropbox.com/developers/reference/devguide
https://www.dropbox.com/developers/reference/oauthguide


BUILD INSTRUCTIONS

- this application is developed in Android Studio, compatible with android devices using API 22 or higher
- add sFileChooser and dropboxChooserSDK as modules to the main project
- add the following libraries:
	-dropbox-android-sdk-1.6.3.jar
	-json_simple-1.1.jar
	-commons-codec-1.7.jar
	-commons-io-2.4.jar

