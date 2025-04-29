import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class wsPassLogic {
    

    public static SecretKey createPassKeyLogic(String pass){
        byte[] passSalt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(passSalt);
        //System.out.println("Salt para passe de server gerado com sucesso.");

        try {
            PBEKeySpec keySpec = new PBEKeySpec(pass.toCharArray(), passSalt, 20, 128);
            SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
            SecretKey key = kf.generateSecret(keySpec);

            byte[] encoded = key.getEncoded();

            byte[] finalKey = new byte[16];
            System.arraycopy(encoded, 0, finalKey, 0, Math.min(encoded.length, 16));

            return new SecretKeySpec(finalKey, "AES");

        } catch(Exception e) {
            System.out.println("Erro a criar key com salt + ws password com PBE");
            e.printStackTrace();
        }
        return null;
    }

    //enviar os bytes da cifra para depois no servidor colocar num ficheiro coeso
    public static byte[] cipherFileLogic(SecretKey secretKey, String alias){
        //
        try{
            FileInputStream kfile = new FileInputStream("truststore.client");  //truststore
            KeyStore kstore = KeyStore.getInstance("PKCS12");
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
        }catch(Exception e){
            System.out.println("Ocorreu um erro a cifrar a secretkey no user");
            e.printStackTrace();
        }
        return null;
    }

    //agarrar no ficheiro do owner do ws cifrado com a sua chave publica e decifrá lo, sacar a key e coloca la na funçao
    public static SecretKey decipherWsKey(String user, byte[] wrappedKey){
        try{
            FileInputStream kfile = new FileInputStream("keystore." + user);
            //se for esta a instance da keyStore
            KeyStore kstore = KeyStore.getInstance("PKCS12");
            kstore.load(kfile, "keypass".toCharArray());  
    
            //assumindo que o alias eh o user
            Key privateKey = kstore.getKey(user, "keypass".toCharArray()); 
    
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.UNWRAP_MODE, privateKey);
            SecretKey secretKey = (SecretKey) cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
    
            return secretKey;
        }catch(Exception e){;
            System.out.println("Ocorreu um erro a decifrar secretkey no user ADD");
            e.printStackTrace();
        }
        return null;
    }

    public static void keyFileToWs(String username, ObjectOutputStream outputStream, SecretKey wsKey){
        try{
            //cifrar com chave publica do owner
            byte[] passCif = wsPassLogic.cipherFileLogic(wsKey, username);

            //mandar para server data da funcao anterior, e ele criar e inicializar o ficheiro 
            privateFunctions.sendBytes(outputStream, passCif);
            
        }catch(Exception e){
            System.out.println("erro a executar cifra da ws key ");
            e.printStackTrace();
        }
    }

    //Returns wrapped key, or in error case a byte array with 0 length
    public static byte[] readToWrappedKey(File keyFile){
        try{
            FileInputStream fis = new FileInputStream(keyFile);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int bytesRead;
            byte[] data = new byte[4096];

            while ((bytesRead = fis.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            fis.close();
            return buffer.toByteArray();
        }catch(Exception e){
            System.out.println("erro a executar cifra da ws key ");
            e.printStackTrace();
        }
        return new byte[0];
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
