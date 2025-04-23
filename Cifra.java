import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Cifra {

    public static void main(String[] args) throws Exception {

    //gerar uma chave aleatória para utilizar com o AES
    KeyGenerator kg = KeyGenerator.getInstance("AES");
    kg.init(128);
    SecretKey key = kg.generateKey(); //chave privada



    //
    FileInputStream kfile = new FileInputStream("keystore.server");  //keystore
    KeyStore kstore = KeyStore.getInstance("JCEKS");
    kstore.load(kfile, "keypass".toCharArray());           //password para aceder à keystore
    Certificate cert = kstore.getCertificate(username);  //alias do utilizador
    //
    PublicKey publickKey = cert.getPublicKey(); //chave publica
    //

    
    Cipher c = Cipher.getInstance("AES");
    c.init(Cipher.ENCRYPT_MODE, key);
    //Encriptar o ficheiro
    FileInputStream fis;
    FileOutputStream fos;
    CipherOutputStream cos;
    
    fis = new FileInputStream("a.txt");

    
    FileOutputStream kos = new FileOutputStream(workspace + ".key." + username);


    cos = new CipherOutputStream(fos, c);
    byte[] b = new byte[16];  
    int i = fis.read(b);
    while (i != -1) {
        cos.write(b, 0, i);
        i = fis.read(b);
    }
    
    cos.close();
    fis.close();
    fos.close();

    Cipher c1 = Cipher.getInstance("RSA");
    c1.init(Cipher.WRAP_MODE, publickKey);
    byte[] keyEncoded = c1.wrap(key);
    FileOutputStream kos = new FileOutputStream("a.key");
    ObjectOutputStream oos = new ObjectOutputStream(kos);
    oos.writeObject(keyEncoded);
    oos.close();
    kos.close();

    //Dicas para decifrar
    //byte[] keyEncoded2 - lido do ficheiro
    //SecretKeySpec keySpec2 = new SecretKeySpec(keyEncoded2, "AES");
    //c.init(Cipher.DECRYPT_MODE, keySpec2);    //SecretKeySpec é subclasse de secretKey
    }
}
//cifrar com simeetira
//cifrar com publica a chave secreta
//guardar a cifrada?

//Decif eh o inverso