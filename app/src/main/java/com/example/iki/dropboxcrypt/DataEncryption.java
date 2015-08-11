package com.example.iki.dropboxcrypt;

/**
 * Created by Iki on 8/6/2015.
 */
public interface DataEncryption {

    /*
     * return long (# of seconds to encrypt file: filepath )
     */
    public abstract long encryptFile(String encodedFilepath, String origFilepath);

    /*
     * return long (# of seconds to decrypt file: filepath )
     */
    public abstract long decryptFile(String decodedFilepath, String encodedFilepath);

    /*
     * return null. Change blocksize of bytes sent to cipher at a time
     */
    public abstract void setBlocksize(int blocksize);


}
