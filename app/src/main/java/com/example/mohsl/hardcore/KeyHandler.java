package com.example.mohsl.hardcore;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import org.spongycastle.crypto.util.PublicKeyFactory;
import org.spongycastle.jcajce.provider.asymmetric.dh.BCDHPublicKey;
import org.spongycastle.openssl.PEMWriter;
import org.spongycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by mohsl on 16.04.2015.
 */
public class KeyHandler {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }
    private static KeyHandler instance;
    private PublicKey pubKey;
    private PrivateKey privKey;

    public static KeyHandler getInstance() {
        if (instance == null) {
            instance = new KeyHandler();
        }
        return instance;
    }

    public KeyHandler() {
    }

    public boolean generateAndStoreKeys() {
        SecureRandom random = new SecureRandom();
        // create the RSA Key
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA", "SC");
            generator.initialize(1024, random);

            KeyPair pair = generator.generateKeyPair();
            PublicKey pubKey = pair.getPublic();
            PrivateKey privKey = pair.getPrivate();
            this.setPrivKey(privKey);
            this.setPubKey(pubKey);
            Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), "Public key:" + pubKey.toString());
            Log.i(MainActivity.getAppContext().getString(R.string.debug_tag), "Private key:" + privKey.toString());

            FileOutputStream fos = MainActivity.getAppContext().openFileOutput(String.valueOf(R.string.file_name_for_saved_keys), Context.MODE_PRIVATE);
            ObjectOutputStream oout = new ObjectOutputStream(fos);
            oout.writeObject(pair);
            oout.close();
            fos.close();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            return false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public PublicKey getPubKey() {
        return pubKey;
    }

    public PrivateKey getPrivKey() {
        return privKey;
    }

    private void setPubKey(PublicKey pubKey) {
        this.pubKey = pubKey;
    }

    //use this function to serialize a key to encoded String, which can be used to send to server or store in db
    public String getSerializationFromKey(Key key) {

        //TODO: Check alternative
        // Send the public key bytes to the other party...
        byte[] publicKeyBytes = key.getEncoded();

        //Convert Public key to String
        String pubKeyStr = new String(Base64.encode(publicKeyBytes));
        return pubKeyStr;
    }

    public String getEncodedFromKeyBlock(byte[] keyBlock) {
        byte[] encodedBytes;
        encodedBytes = Base64.encode(keyBlock);
        return new String(encodedBytes);
    }

    public byte[] getKeyBlockfromEncoded(String encodedKeyBlock){
        byte[] decodedBytes = Base64.decode(encodedKeyBlock);
        return decodedBytes;
    }

    public PublicKey getKeyFromSerialization(String encodedKey) {
        Object obj = null;
        byte[] decodedBytes = Base64.decode(encodedKey);
        ByteArrayInputStream bi = new ByteArrayInputStream(decodedBytes);
        ObjectInputStream oi = null;
        PublicKey pubKey2 = null;

            try {
                //encodedBytes = Base64.encode(pubKey.getEncoded());
                X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(decodedBytes);
                KeyFactory keyFact = KeyFactory.getInstance("RSA", "SC");
                pubKey2 = keyFact.generatePublic(x509KeySpec);
                Log.i(String.valueOf(R.string.debug_tag), pubKey2.toString());
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            }
        return pubKey2;
    }

    private void setPrivKey(PrivateKey privKey) {
        this.privKey = privKey;
    }

    public void readInKeys() {
        try {
            FileInputStream fis = MainActivity.getAppContext().openFileInput(String.valueOf(R.string.file_name_for_saved_keys));
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object obj = ois.readObject();
            assert (obj instanceof KeyPair);
            ois.close();
            fis.close();
            Log.i(String.valueOf(R.string.debug_tag), ((KeyPair) obj).getPrivate().toString());
            setPubKey(((KeyPair) obj).getPublic());
            setPrivKey(((KeyPair) obj).getPrivate());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public String[] getEncryptedMessageAndKeyBlock(String messageText, Key publicKey) {
        String[] returnValue = new String[2];
        //0--> encoded message
        //1-->keyblock
        byte[] keyBlock=null;
        SecureRandom     random = new SecureRandom();
        byte[] cipherText=null;
        // create the symmetric key and iv
        try {
            Key sKey = Utils.createKeyForAES(256, random);
            IvParameterSpec sIvSpec = Utils.createCtrIvForAES(0, random);
            // symmetric key/iv wrapping step
            Cipher           xCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding", "SC");
            xCipher.init(Cipher.ENCRYPT_MODE, publicKey, random);
            keyBlock = xCipher.doFinal(packKeyAndIv(sKey, sIvSpec));

            // encryption step
            Cipher          sCipher = Cipher.getInstance("AES/CTR/NoPadding", "SC");
            sCipher.init(Cipher.ENCRYPT_MODE, sKey, sIvSpec);
            cipherText = sCipher.doFinal(messageText.getBytes());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        returnValue[0]=new String(Base64.encode(cipherText));
        returnValue[1]=getEncodedFromKeyBlock(keyBlock);

        return returnValue;
    }

    public String decryptMessage(String messageText, byte[] keyBlock){
        Cipher xCipher = null;
        byte[] plainText = null;
        try {
            xCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding", "SC");
            Cipher          sCipher = Cipher.getInstance("AES/CTR/NoPadding", "SC");
            // symmetric key/iv unwrapping step
            xCipher.init(Cipher.DECRYPT_MODE, privKey);
            Object[]keyIv = unpackKeyAndIV(xCipher.doFinal(keyBlock));

            // decryption step
            sCipher.init(Cipher.DECRYPT_MODE, (Key) keyIv[0], (IvParameterSpec) keyIv[1]);

            byte[] decodedBytes = Base64.decode(messageText);
            plainText = sCipher.doFinal(decodedBytes);
            Log.i(String.valueOf(R.string.debug_tag), plainText.toString());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return new String(plainText);
    }


    public String getSignatureAndEncode(String messageText, PrivateKey privateKey) {
        Signature signature = null;
        byte[] sigBytes = null, messageBytes = messageText.getBytes();
        try {
            signature = Signature.getInstance("RSA", "SC");
            signature.initSign(privateKey, new SecureRandom());
            signature.update(messageBytes);
            sigBytes = signature.sign();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return new String(Base64.encode(sigBytes));
    }

    /**
     *
     * @param strMessage
     * @param strSignature
     * @param publicKey
     * @return {@code true} if the signature was verified, {@code false} otherwise.
     */
    public boolean verifySignature(String strMessage, String strSignature, PublicKey publicKey) {
        boolean result = false;
        Signature signatureObj = null;
        byte[] messageBytes = strMessage.getBytes(),
               signatureBytes = Base64.decode(strSignature);
        try {
            signatureObj = Signature.getInstance("RSA", "SC");
            signatureObj.initVerify(publicKey);
            signatureObj.update(messageBytes);
            result = signatureObj.verify(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static byte[] packKeyAndIv(
            Key key,
            IvParameterSpec ivSpec)
            throws IOException
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        bOut.write(ivSpec.getIV());
        bOut.write(key.getEncoded());
        return bOut.toByteArray();
    }

    private static Object[] unpackKeyAndIV(
            byte[]    data)
    {
        byte[]    keyD = new byte[16];
        byte[]    iv = new byte[data.length - 16];

        return new Object[] {
                new SecretKeySpec(data, 16, data.length - 16, "AES"),
                new IvParameterSpec(data, 0, 16)
        };
    }

}