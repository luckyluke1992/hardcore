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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;

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

    private static KeyHandler instance;
    private Key pubKey;
    private Key privKey;
    private Key sKey;
    //private IvParameterSpec sIvSpec;
    private byte[] keyBlock;

    public static KeyHandler getInstance() {
        if (instance == null) {
            instance = new KeyHandler();
        }
        return instance;
    }

    public KeyHandler() {
    }

    public void generateAndStoreKeys() {
        SecureRandom random = Utils.createFixedRandom();
        // create the RSA Key
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA", "BC");
            generator.initialize(1024, random);

            KeyPair pair = generator.generateKeyPair();
            Key pubKey = pair.getPublic();
            Key privKey = pair.getPrivate();
            this.setPrivKey(privKey);
            this.setPubKey(pubKey);
            Log.i("Hardcore", "Public key:" + pubKey.toString());
            Log.i("Hardcore", "Private key:" + privKey.toString());

            String FILENAME = "KeyPair.txt";
            FileOutputStream fos = MainActivity.getAppContext().openFileOutput(FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream oout = new ObjectOutputStream(fos);
            oout.writeObject(pair);
            oout.close();
            fos.close();

            //write keys in file with the awesome pem writer class
            /*try {

                //private Key
                File file = new File(MainActivity.getAppContext().getFilesDir(), "privateKey.txt");
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileOutputStream fos = MainActivity.getAppContext().openFileOutput("privateKey.txt", Context.MODE_PRIVATE);
                FileWriter fileWriter = new FileWriter(fos.getFD());
                PEMWriter pemWriter = new PEMWriter(fileWriter);
                pemWriter.writeObject(pair.getPrivate());
                pemWriter.flush();
                pemWriter.close();
                fileWriter.close();
                fos.close();

                //private Key
                File file2 = new File(MainActivity.getAppContext().getFilesDir(), "publicKey.txt");
                if (!file2.exists()) {
                    file2.createNewFile();
                }
                FileOutputStream fos2 = MainActivity.getAppContext().openFileOutput("publicKey.txt", Context.MODE_PRIVATE);
                FileWriter fileWriter2 = new FileWriter(fos2.getFD());
                PEMWriter pemWriter2 = new PEMWriter(fileWriter2);
                pemWriter2.writeObject(pair.getPublic());
                pemWriter2.flush();
                pemWriter2.close();
                fileWriter2.close();
                fos2.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Key getPubKey() {
        return pubKey;
    }

    private void setPubKey(Key pubKey) {
        this.pubKey = pubKey;
    }

    public Key getPrivKey() {
        return privKey;
    }

    //use this function to serialize a key to encoded String, which can be used to send to servr or store in db
    public String getSerializationFromKey(Key key) {
        byte[] res = null;
        byte[] encodedBytes = null;
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = null;
            //Mikes idea to make code cleaner
            //Class<?> c = Key.class;
            //c.getField("publicKey");
            //Key newKey = (PublicKey)PublicKeyFactory.createKey(pubKey.getEncoded());
            o = new ObjectOutputStream(b);
            o.writeObject(key);
            res = b.toByteArray();
            encodedBytes = Base64.encode(res);
            o.close();
            b.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(encodedBytes);
    }

    public Key getKeyFromSerialization(String encodedKey) {
        Object obj = null;
        byte[] decodedBytes = Base64.decode(encodedKey);
        ByteArrayInputStream bi = new ByteArrayInputStream(decodedBytes);
        ObjectInputStream oi = null;
        try {
            oi = new ObjectInputStream(bi);
            obj = oi.readObject();
            assert (obj instanceof Key);
            oi.close();
            bi.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (Key) obj;
    }

    private void setPrivKey(Key privKey) {
        this.privKey = privKey;
    }

    public void readInKeys() {
        try {
            FileInputStream fis = MainActivity.getAppContext().openFileInput("KeyPair.txt");
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object obj = ois.readObject();
            assert (obj instanceof KeyPair);
            ois.close();
            fis.close();
            Log.i("Hardcore input key from file", ((KeyPair) obj).getPrivate().toString());
            setPubKey(((KeyPair) obj).getPublic());
            setPrivKey(((KeyPair) obj).getPrivate());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public String encryptMessage(String messageText, Key publicKey) {
        SecureRandom     random = Utils.createFixedRandom();
        byte[] cipherText=null;
        // create the symmetric key and iv
        try {
            sKey = Utils.createKeyForAES(256, random);
            IvParameterSpec sIvSpec = Utils.createCtrIvForAES(0, random);
            // symmetric key/iv wrapping step
            Cipher           xCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding", "BC");
            xCipher.init(Cipher.ENCRYPT_MODE, pubKey, random);
            keyBlock = xCipher.doFinal(packKeyAndIv(sKey, sIvSpec));

            // encryption step
            Cipher          sCipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
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
        return new String(Base64.encode(cipherText));
    }

    public String decryptMessage(String messageText){
        Cipher xCipher = null;
        byte[] plainText = null;
        try {
            xCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding", "BC");
            Cipher          sCipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
            // symmetric key/iv unwrapping step
            xCipher.init(Cipher.DECRYPT_MODE, privKey);
            Object[]keyIv = unpackKeyAndIV(xCipher.doFinal(keyBlock));

            // decryption step
            sCipher.init(Cipher.DECRYPT_MODE, (Key)keyIv[0], (IvParameterSpec)keyIv[1]);

            byte[] decodedBytes = Base64.decode(messageText);
            plainText = sCipher.doFinal(decodedBytes);
            Log.i("Hardcore, Base64 decoded:", plainText.toString());

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