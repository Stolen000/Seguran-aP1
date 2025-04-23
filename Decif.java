import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Decif {
    @SuppressWarnings("resource")
    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, IOException, ClassNotFoundException, InvalidKeyException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        
        
    //
    FileInputStream kfile = new FileInputStream("myKeys");  //keystore
    KeyStore kstore = KeyStore.getInstance("JCEKS");
    kstore.load(kfile, "keypass".toCharArray());           //password para aceder à keystore
    Certificate cert = kstore.getCertificate("keyrsa");  //alias do utilizador
    //
    Key myPrivateKey = kstore.getKey("keyRSA", "keypass".toCharArray());
    
        
        // faltam buffered streams
        FileInputStream fis;
        FileOutputStream fos;
        CipherInputStream cis;
        ObjectInputStream ois;
        ObjectOutputStream ous;
        Cipher c = Cipher.getInstance("AES");

        fis = new FileInputStream("a.key");
        ois = new ObjectInputStream(fis);
        byte[] key = (byte[]) ois.readObject();

        Cipher c1 = Cipher.getInstance("RSA");
        c1.init(Cipher.UNWRAP_MODE, myPrivateKey);
        
        Key keyEncoded = c1.unwrap(key,"AES",Cipher.SECRET_KEY);



        //SecretKeySpec keySpec2 = new SecretKeySpec(key, "AES");
        //SecretKeySpec é subclasse de secretKey
        c.init(Cipher.DECRYPT_MODE, keyEncoded);

        ois.close();
        fis.close();

        fis = new FileInputStream("a.cif");
        fos = new FileOutputStream("decif.txt");

        cis = new CipherInputStream(fis, c);
        byte[] b = new byte[16];
        int i = 0;
        while ((i=cis.read(b) ) != -1) {
        fos.write(b, 0, i);
        }
        fos.close();
    }
    
    





    //Dicas para decifrar
    //byte[] keyEncoded2 - lido do ficheiro
    //SecretKeySpec keySpec2 = new SecretKeySpec(keyEncoded2, "AES");
    //c.init(Cipher.DECRYPT_MODE, keySpec2);    //SecretKeySpec é subclasse de secretKey
}
