package com.example.iki.dropboxcrypt;

/**
 * Created by Iki on 8/6/2015.
 */
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
    //private SecretKey mKey;

    public DataEncryptionCrypto() {
        mBlocksize = 128;
       /* KeyGenerator kgen;
        mKey = null;
        try {
            kgen = KeyGenerator.getInstance("AES");
            mKey = kgen.generateKey();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }*/
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
            String filename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download" + outfileKey;// + encFilepath + ".key";
            tmpFile = new File(filename);
            if(!tmpFile.exists())
                tmpFile.createNewFile();
            KeyStoreUtils.saveKey(mKey, tmpFile);

            // Create Cipher using "AES" provider
            Cipher encipher = Cipher.getInstance("AES");
            encipher.init(Cipher.ENCRYPT_MODE, mKey);
            CipherOutputStream cos = new CipherOutputStream(encfos, encipher);

            // capture time it takes to encrypt file
            start = System.nanoTime();
            Log.d(TAG, String.valueOf(start));

            byte[] block = new byte[mBlocksize];

            while ((read = fis.read(block,0,mBlocksize)) != -1) {
                cos.write(block,0, read);
            }
            cos.close();
            stop = System.nanoTime();

            Log.d(TAG, String.valueOf(stop));
            seconds = (stop - start) / 1000000;// for milliseconds
            Log.d(TAG, String.valueOf(seconds));

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

            // capture time it takes to decrypt file
            start = System.nanoTime();
            Log.d(TAG, String.valueOf(start));

            byte[] block = new byte[mBlocksize];

            while ((read = cis.read(block,0,mBlocksize)) != -1) {
                decfos.write(block,0, read);
            }
            cis.close();
            stop = System.nanoTime();

            Log.d(TAG, String.valueOf(stop));
            seconds = (stop - start) / 1000000;// for milliseconds
            Log.d(TAG, String.valueOf(seconds));

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