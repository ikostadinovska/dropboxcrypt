package com.example.iki.dropboxcrypt;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class DataEncryptionCrypto implements DataEncryption {
    private String TAG= "DataEncryptionCrypto";
    private int mBlocksize;
    private File tmpFile;

    public DataEncryptionCrypto() {
        mBlocksize = 128;
    }

    @Override
    public long encryptFile(String encFilepath,String origFilepath) {
        long start, stop, seconds;
        seconds = -1;

        try{
            Log.d(TAG, "Crypto: Encrypting file:" + origFilepath);

            // open stream to read origFilepath. We are going to save encrypted contents to outfile
            InputStream fis = new FileInputStream(origFilepath);
            File outfile = new File(encFilepath);
            int read = 0;
            if (!outfile.exists())
                outfile.createNewFile();

            FileOutputStream encfos = new FileOutputStream(outfile);
            //Create key for the file
            SecretKey mKey = KeyStoreUtils.generateKey();
            int pos = outfile.getName().lastIndexOf('.');
            String outfileKey = outfile.getName().substring(0,pos) + ".key";
            String filename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download" + outfileKey;
            tmpFile = new File(filename);
            if(!tmpFile.exists())
                tmpFile.createNewFile();
            KeyStoreUtils.saveKey(mKey, tmpFile);

            // Create Cipher using "AES" provider
            Cipher encipher;
            encipher = Cipher.getInstance("AES");
            encipher.init(Cipher.ENCRYPT_MODE, mKey);
            CipherOutputStream cos = new CipherOutputStream(encfos, encipher);

            byte[] block = new byte[mBlocksize];

            while ((read = fis.read(block,0,mBlocksize)) != -1) {
                cos.write(block,0, read);
            }
            cos.close();
            fis.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return seconds;
    }

    @Override
    public long decryptFile(String decFilepath, String encFilepath) {
        long start, stop, seconds;
        seconds = -1;

        try{
            Log.d(TAG, "Crypto: Decrypting file:" + encFilepath);

            // open stream to read encFilepath. We are going to save decrypted contents to outfile
            InputStream fis = new FileInputStream(encFilepath);
            File outfile = new File(decFilepath);
            int read = 0;
            if (!outfile.exists())
                outfile.createNewFile();

            FileOutputStream decfos = new FileOutputStream(outfile);

            // Get the key saved in the .key file
            //String filename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download" + encFilepath + ".key";
            int pos = encFilepath.lastIndexOf('.');
            String filename = encFilepath.substring(0,pos)+".key";
            tmpFile = new File(filename);
            SecretKey mKey = KeyStoreUtils.loadKey(tmpFile);

            // Create Cipher using "AES" provider
            Cipher decipher = Cipher.getInstance("AES");
            decipher.init(Cipher.DECRYPT_MODE, mKey);
            CipherInputStream cis = new CipherInputStream(fis, decipher);

            byte[] block = new byte[mBlocksize];

            while ((read = cis.read(block,0,mBlocksize)) != -1) {
                decfos.write(block,0, read);
            }
            cis.close();

            decfos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return seconds;

    }

    @Override
    public void setBlocksize(int blocksize) {
        mBlocksize = blocksize;
    }

}