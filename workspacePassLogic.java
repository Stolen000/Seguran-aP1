import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class workspacePassLogic {

    public static SecretKey createPassKeyLogic(String pass) throws Exception{
		byte[] passSalt = new byte[16]; 
        SecureRandom random = new SecureRandom();
        random.nextBytes(passSalt);
        System.out.println("Salt para passe de server gerado com sucesso.");
        PBEKeySpec keySpec = new PBEKeySpec(pass.toCharArray(), passSalt, 20);
        SecretKeyFactory kf;
        kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
        SecretKey key = kf.generateSecret(keySpec);
        return new SecretKeySpec(key.getEncoded(), "AES");
    }

    //enviar os bytes da cifra para depois no servidor colocar num ficheiro coeso
    public static byte[] cipherFileLogic(SecretKey secretKey, String alias) throws Exception {
        //
        FileInputStream kfile = new FileInputStream("keystore.server");  //keystore
        KeyStore kstore = KeyStore.getInstance("JCEKS");
        kstore.load(kfile, "keypass".toCharArray());           //password para aceder à keystore

        //verificar qual o alias que identifica o user para retirar o seu certificado
        Certificate cert = kstore.getCertificate(alias);  //alias do utilizador
        //
        PublicKey publicKey = cert.getPublicKey(); //chave publica

        //Encriptar o ficheiro        
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.WRAP_MODE, publicKey);
        byte[] wrappedKey = cipher.wrap(secretKey); 

        return wrappedKey;
    }

    //agarrar no ficheiro do owner do ws cifrado com a sua chave publica e decifrá lo, sacar a key e coloca la na funçao
    public SecretKey decipherWsKey(String ws, String user) throws Exception {

        FileInputStream kfile = new FileInputStream("keystore." + user);
        KeyStore kstore = KeyStore.getInstance("JCEKS");
        kstore.load(kfile, "keypass".toCharArray());  

        PrivateKey privateKey = (PrivateKey) kstore.getKey(user, "keypass".toCharArray()); 

        String filename = ws + ".key." + user;
        byte[] wrappedKey = Files.readAllBytes(Paths.get(filename));

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.UNWRAP_MODE, privateKey);
        SecretKey secretKey = (SecretKey) cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

        return secretKey;
    }

    //quando owner faz create
    //vem o owner
    //cria o salt aleatorio e por sua vez a chave com base na pass do ws
    //cifra com a sua chave publica usando (ws, secreKey, ownerName)
    //cria o ficheiro cifra com ws.key.ownerName
    //envia este file para o servidor -- para o ws criado

    //quando owner da add a user
    //recebe o file cifrado do lado do servidor
    //decifrar com o seu (ws, ownerName)
    //utilizar a chave vinda da decifraçao
    //coloca la no cifrar mas agr com (ws, decifradaKey, userToAddName)
    //cria o ficheiro cifra com ws.key.userToAdd
    //enviar para o servidor -- para o ws criado

    
}
