import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;




public class mySharingClient {
        
        public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, KeyStoreException, InvalidKeyException, NoSuchPaddingException, SignatureException{
            System.out.println("cliente : main");
            if (args.length < 3){
                System.out.println("Input invalido"); 
                System.exit(0);
            }
            System.setProperty("javax.net.ssl.trustStore", "truststore.client");
            System.setProperty("javax.net.ssl.trustStorePassword", "keypass"); 
            Scanner sc = new Scanner(System.in);

            //Recebe os argumentos e guarda o ServerAdress para dar connect, o Porto, o User e a Pass
            String inputs[] = mySharingClient.verifyInput(args, sc);
            

            SocketFactory sf = SSLSocketFactory.getDefault();
            SSLSocket clientSocket = (SSLSocket) sf.createSocket(inputs[0], Integer.parseInt(inputs[1]));
    
            //Socket clientSocket = new Socket(inputs[0], Integer.parseInt(inputs[1]));
    
            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
    
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            createHookShutdown(inputStream, outputStream, clientSocket);

            if(mySharingClient.startAuthentication(inputStream, outputStream, inputs[2], inputs[3], sc)){
                mySharingClient.runClient(inputStream, outputStream, clientSocket, sc, inputs[2]);
            }
            sc.close();
    }

    private static boolean startAuthentication(ObjectInputStream inputStream, ObjectOutputStream outputStream, String username, String password, Scanner scanner){
       
        boolean respostaInvalida = true;
        String userInputUser = username;
        String userInputPassword = password;
        String respostaAutentificacao = "";

        
        try {
            while (respostaInvalida) {
                outputStream.writeObject(userInputUser);
                outputStream.writeObject(userInputPassword);
                //-----------------Se autentificado corretamente: OK-USER || Se novo user: OK-NEW-USER
                respostaAutentificacao = (String) inputStream.readObject();   
                respostaInvalida = respostaAutentificacao.equals("WRONG-PWD");
                if (respostaInvalida) {

                    //Fica a repetir o processo até introduzir a password correta ou um novo user e pass
                    System.out.print("Resposta Invalida, tente novamente (eg: Beto seguranca2025): ");
                    String[] credentials = getValidCredentials(scanner);
                    userInputUser = credentials[0];
                    userInputPassword = credentials[1];
                }
            }
            if(respostaAutentificacao.equals("OK-NEW-USER")){
                SecretKey wsKey = (SecretKey) wsPassLogic.createPassKeyLogic(userInputUser);
                wsPassLogic.keyFileToWs(userInputUser, outputStream, wsKey);
            }

            //Assegurar que a resposta do server é correta
            else if(!respostaAutentificacao.equals("OK-USER")){
                System.out.println("Resposta de Autentificacao falhada.");
            }        

        } catch (Exception e){
            //System.out.println(e.getMessage());
            return false;
        } 
        return true;
    }

    private static void runClient(ObjectInputStream inputStream, ObjectOutputStream outputStream, Socket clientSocket, Scanner sc, String username) throws NoSuchAlgorithmException, 
                                        CertificateException, KeyStoreException, UnrecoverableKeyException, InvalidKeyException, NoSuchPaddingException, SignatureException{

        //Servidor cria novo workspace e entra no loop de operaçoes ? || encontrou user
        //Declaracao de variaveis
        String inputDoUser;
        String comando;
        String[] arrayDeArgumentos;


        printMenuDeOperacoes();
        //Loop das operações
        

        try{
            while (true) {
                //Menu das operacoes
            
                //------------------v Input do comando do user
                //inputDoUser = new String("CREATE workspace004");

                boolean doneOperation = false;
                System.out.print("Comando: ");
                
                if (!sc.hasNextLine()){
                    break;
                }
                inputDoUser = sc.nextLine();
                System.out.println();
                //In progress:Tratar input
                arrayDeArgumentos = inputDoUser.trim().split("\s+");
                comando = arrayDeArgumentos[0];

                switch (comando) {
                    //CREATE <ws> <password>
                    //preciso de acessar username de user 
                    //precisar de criar o salt, ficheiro cifrado com chave publica do owner
                    //enviar para o servidor
                    //fazer isso depois da resposta OK do server
                    case "CREATE":
                        if(arrayDeArgumentos.length == 3){
                            String result = sendAndReceive(inputStream, outputStream, inputDoUser);
                            doneOperation = true;
                            if(result.equals("OK")){
                                //executar logica de password key

                                            //criar chave secreta com password do ws
                                SecretKey wsKey = wsPassLogic.createPassKeyLogic(arrayDeArgumentos[2]);
                                wsPassLogic.keyFileToWs(username, outputStream, wsKey);
                            }
                        } 
                    break;
                        //se nao entrar no if ele cai no default

                    //ADD <user1> <ws>
                    case "ADD":
                    //apos receber o ok logica das passes
                        //precisa de mais tramento? (?)
                        if(arrayDeArgumentos.length == 3){
                            String result = sendAndReceive(inputStream, outputStream, inputDoUser);
                            doneOperation = true;

                            if(result.equals("OK")){
                                byte[] keyFileData = privateFunctions.receiveBytes(inputStream);
                                if(keyFileData != null){
                                    //recebi a data do file ws.key.owner
                                    //quero dar unwrap com chave privada e sacar a key disso
                                    SecretKey secretKey = wsPassLogic.decipherWsKey(username, keyFileData);
                                
                                    //dar wrap com chave publica do gajo to add
                                    byte[] wrappedData = wsPassLogic.cipherFileLogic(secretKey, arrayDeArgumentos[1]);
                                    //voltar a enviar estes bytes e o server receber e criar o file
                                    privateFunctions.sendBytes(outputStream, wrappedData);
                                }

                            }
                        }
                        break;
                    //UP <ws> <file1> ... <filen>
                    case "UP":
                        if(arrayDeArgumentos.length >= 3){
                            //mandou primeira mensagem
                            outputStream.writeObject(inputDoUser);
                            String respostaDoServer = (String) inputStream.readObject();
                            //Se nao foi validada a operacao, acabar
                            if(!respostaDoServer.equals("OK")){
                                doneOperation = true;
                                System.out.println("Resposta: " + respostaDoServer + System.lineSeparator());
                                break;
                            } 
                            //RECEBE A PASS DO WS ENCRYPTADA COM A SUA CHAVE PUBLICA
                            //Declaraçao de vars para a decif
                            FileInputStream fis;
                            
                            privateFunctions.receiveFile(inputStream, arrayDeArgumentos[1] + ".key." + username , null);
                            
                            //DECIFRA COM A SUA CHAVE PRIVADA
                            //-Ir buscar a sua chave privada do seu certificado/truststore
                            //
                            FileInputStream kfile = new FileInputStream("keystore." + username); 
                            KeyStore kstore = KeyStore.getInstance("PKCS12");
                            kstore.load(kfile, "keypass".toCharArray());           //password para aceder à keystore
                            //Certificate cert = kstore.getCertificate("keyrsa");  //alias do utilizador
                            //
                            PrivateKey myPrivateKey = (PrivateKey) kstore.getKey(username, "keypass".toCharArray());
                            File wsKey = new File(arrayDeArgumentos[1] + ".key." + username);
                            //- Iniciar decifracao da chave do WS.

                            Cipher c = Cipher.getInstance("AES");//PBEWithHmacSHA256AndAES_128

                            byte[] wrapedkey = wsPassLogic.readToWrappedKey(wsKey);
                            //byte[] wrapedkey = fis.readAllBytes();
                            //decipherWsKey(username,wrapedkey);
                            
                            Cipher desencryptWithPublicKey = Cipher.getInstance("RSA");
                            desencryptWithPublicKey.init(Cipher.UNWRAP_MODE, myPrivateKey);
                           
                            Key unwrappedKey = desencryptWithPublicKey.unwrap(wrapedkey,"AES",Cipher.SECRET_KEY);

                            //--Fechar vars
                            kfile.close();

                            c.init(Cipher.ENCRYPT_MODE, unwrappedKey);
                            //MUITO MAU MAS PARA TESTAR SIGNATURES, retirar var global after
                            
                            //CIFRA OS FICHEIROS COM A CHAVE DO WS
                            //ENVIA ESSES FICHEIROS (CIFRADOS LA DENTRO)
                            System.out.println(uploadFicheiros(inputStream, outputStream, arrayDeArgumentos,c, myPrivateKey, username));
                            wsKey.delete();
                            
                            doneOperation = true;
                        }    
                        break;    
                    //DW <ws> <file1> ... <filen>                
                    case "DW":
                        if(arrayDeArgumentos.length >= 3){
                            //mandou primeira mensagem
                            outputStream.writeObject(inputDoUser);
                            String respostaDoServer = (String) inputStream.readObject();
                            File wsKey = new File(arrayDeArgumentos[1] + ".key." + username);
                            //Se nao foi validada a operacao, acabar
                            if(!respostaDoServer.equals("OK")){
                                
                                System.out.println("Resposta :" + respostaDoServer + System.lineSeparator());
                                doneOperation = true;
                                break;
                            } 

                            //Preparar para receber
                            System.out.println(downloadFicheiros(inputStream,outputStream,arrayDeArgumentos, username)); 
                            //Recebeu os files encriptados + a chave encriptada do WS.
                            //Opcoes : Desencriptar e substituir aqui, ou quando se recebe ja. recebendo primeiro o passWS
                            
                            wsKey.delete();
                            doneOperation = true;
                        }
                        break;
                    case "RM":
                        if(arrayDeArgumentos.length >= 3){
                            sendAndReceive(inputStream, outputStream, inputDoUser);
                            doneOperation = true;
                        }
                        break;
                    case "LW":
                        if(arrayDeArgumentos.length == 1){
                            sendAndReceive(inputStream, outputStream, inputDoUser);
                            doneOperation = true;
                        }
                        break;
                    case "LS":
                        if(arrayDeArgumentos.length == 2){
                            sendAndReceive(inputStream, outputStream, inputDoUser);
                            doneOperation = true;
                        }
                        //mandar msg de erro?
                        //So n das break fora do if para ele ir pro default e printar o menu
                        break;
                    default:
                        //N faz nada
                        break;
                }
                if(!doneOperation){
                    System.out.println("Comando invalido, tente novamente.");
                    printMenuDeOperacoes();
                }
            }
        }catch(IOException | ClassNotFoundException e){
                System.out.println(e.getMessage());
        }               
    }
    private static String downloadFicheiros(ObjectInputStream inputStream, ObjectOutputStream outputStream,
            String[] arrayDeArgumentos, String username) throws ClassNotFoundException, IOException, KeyStoreException, 
                NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, NoSuchPaddingException, InvalidKeyException, SignatureException{
        
        String filePathAtual;
        boolean isFileNewInThisDir;
        StringBuilder sBuilder = new StringBuilder("Resposta: ");
        
        //Recebe o passWS encriptado
        //Declaraçao de vars para a decif
        FileInputStream fis;
        FileOutputStream fos;
        CipherInputStream cis;
        
        privateFunctions.receiveFile(inputStream, arrayDeArgumentos[1] + ".key." + username , null); 
              
        //Desencripta
        //DECIFRA COM A SUA CHAVE PRIVADA
        //-Ir buscar a sua chave privada do seu certificado/truststore
        //
        FileInputStream kfile = new FileInputStream("keystore." + username);  //keystore ## Tou a usar a cllientkeys nao sabendo se temos que usar a truststore
        KeyStore kstore = KeyStore.getInstance("PKCS12");
        kstore.load(kfile, "keypass".toCharArray());           //password para aceder à keystore
        //Certificate cert = kstore.getCertificate(username);  //alias do utilizador
        //
        Key myPrivateKey = kstore.getKey(username, "keypass".toCharArray());
        File keyFile = new File(arrayDeArgumentos[1] + ".key." + username);
        //- Iniciar decifracao da chave do WS.

        Cipher c = Cipher.getInstance("AES"); //PBEWithHmacSHA256AndAES_128
        
        byte[] wrapedkey = wsPassLogic.readToWrappedKey(keyFile);

        Cipher desencryptWithPublicKey = Cipher.getInstance("RSA");
        desencryptWithPublicKey.init(Cipher.UNWRAP_MODE, myPrivateKey);

        Key unwrappedKey =  desencryptWithPublicKey.unwrap(wrapedkey,"AES",Cipher.SECRET_KEY);
        //SecretKey unwrappedKey = wsPassLogic.decipherWsKey(username, wrapedkey);

        //--Fechar vars
        kfile.close();
        
        try {
            c.init(Cipher.DECRYPT_MODE, unwrappedKey);
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       //Erro? Invalid Key
        //Percorre todos os ficheiros
        for (int i = 2; i < arrayDeArgumentos.length; i++) {
            //System.out.println(arrayDeArgumentos[i]);

            //mete no strbuilder o path atual
            sBuilder.append(arrayDeArgumentos[i]).append(": ");
            //fica á espera do path para saber se existe no ws
            filePathAtual = (String) inputStream.readObject();
            if(!filePathAtual.equals("-1")){
                //eh valido (existe no server)
                //Pasta atual
                
                //checka se já existe na sua dir
                isFileNewInThisDir = !privateFunctions.isFileInWorkspace(filePathAtual, null);
                
                outputStream.writeObject(isFileNewInThisDir);
                if(isFileNewInThisDir){
                   
                    privateFunctions.receiveFile(inputStream, filePathAtual, null);

                    //Apanha o sign nome + asserio
                    String signName = new String(privateFunctions.receiveBytes(inputStream), StandardCharsets.UTF_8);

                    Path path = Paths.get(signName);
                    signName = path.getFileName().toString();
                    byte signature[] = privateFunctions.receiveBytes(inputStream);

                    File signatureFile = new File(signName); 
                    try (FileOutputStream fosSig = new FileOutputStream(signatureFile)) {
                        fosSig.write(signature);
                        fosSig.flush();
                    }
                    
                    //Ir buscar a public key do sign
                    FileInputStream kfile1 = new FileInputStream("truststore.client");  //keystore ## Tou a usar a cllientkeys nao sabendo se temos que usar a truststore
                    KeyStore kstore1 = KeyStore.getInstance("PKCS12");
                    kstore1.load(kfile1, "keypass".toCharArray());  
                    //Deve dar o user que deu sign
                    String aliasSigned = signName.split(filePathAtual+".signed.")[1]; 
                    Certificate certificate = kstore1.getCertificate(aliasSigned);
                    PublicKey pk = certificate.getPublicKey();
                    //Signature file handling
                   
                    Signature s = Signature.getInstance("MD5withRSA");
                    s.initVerify(pk);
                    
                    sBuilder.append("OK\n");
                    //Desencripta o ficheiro
                    File encryptedFile = new File(filePathAtual);
                    File tempDecryptedFile = new File(filePathAtual + ".tmp");

                    fis = new FileInputStream(filePathAtual);
                    fos = new FileOutputStream(filePathAtual + ".tmp");
                    try {
                        cis = new CipherInputStream(fis, c);
                        byte[] b = new byte[16];
                        int j = 0;
                        while ((j=cis.read(b) ) != -1) {
                        fos.write(b, 0, j);
                        }
                        cis.close();
                    } catch (Exception e) {
                        //System.err.println(e);
                        System.out.println("Ficheiro Corrompido");
                    }
                    
                    fos.close();
                    fis.close();
                    //Verify signature
                    byte[] bufFicheiroAtual = Files.readAllBytes(tempDecryptedFile.toPath());
                    s.update(bufFicheiroAtual);

                    if (s.verify(signature)){
                        // dah replace ao ficheiro encriptado
                        if (!encryptedFile.delete()) {
                            System.err.println("Error deleting encrypted file!");
                        }
                        if (!tempDecryptedFile.renameTo(encryptedFile)) {
                            System.err.println("Error renaming decripted file!");
                        }
                    } else{
                        System.out.println("Ficheiro Corrompido");
                        // dah replace ao ficheiro encriptado
                        if (!encryptedFile.delete()) {
                            System.err.println("Error deleting encrypted file!");
                        }
                        if (!tempDecryptedFile.delete()) {
                            System.err.println("Error renaming decripted file!");
                        }
                    }
                    
                } else {
                    //Maybe perguntar se ele quer dar override ou cancelar o download para esse ficheiro.
                    sBuilder.append("Existe um ficheiro com o mesmo nome na diretoria\n"); 
                }
                //Nao deveria ter que mandar resposta
                //outputStream.writeObject(stringDeResposta);
            } else {
                sBuilder.append("Nao existe no WS\n");
            }
            //Nao era valido, passa á frente
        }
        return sBuilder.toString();
    }


    //Envia ficheiros presentes no arrayDeArgumentos
    //Retorna a string que representa a resposta do servidor
    private static String uploadFicheiros(ObjectInputStream inputStream, ObjectOutputStream outputStream,
            String[] arrayDeArgumentos, Cipher c, PrivateKey privateKey, String username) throws IOException, ClassNotFoundException,
                                                                NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        File ficheiroAtual;
        String pathFicheiroAtual;
        String respostaDoServer;
        StringBuilder stringBuilder = new StringBuilder("Resposta: ");
        boolean readBool;
        //v
        FileInputStream fis;
        FileOutputStream fos;
        CipherOutputStream cos;
        //v2
        FileOutputStream fosSig;
        Signature s = Signature.getInstance("MD5withRSA");
        s.initSign(privateKey);


        //Percorre todos os ficheiros e analisa se sao validos
        for (int i = 2; i < arrayDeArgumentos.length; i++) {
            pathFicheiroAtual = arrayDeArgumentos[i];
            ficheiroAtual = new File(pathFicheiroAtual);
            
            //escreve no strBuilder o pathname
            stringBuilder.append(pathFicheiroAtual).append(": ");
            if(ficheiroAtual.exists()){
                //Envia o pathname
                outputStream.writeObject(pathFicheiroAtual);
                
                //Recebe validação do server (boolean)
                /////////////
                readBool = (boolean) inputStream.readObject();
                if(readBool){
                    //Validou entao envia ficheiro
                    //File tempFile = new File(pathFicheiroAtual + ".enc"); ??
                    //Cifrar?

                    //Sign dos dados
                    byte[] bufFicheiroAtual = Files.readAllBytes(ficheiroAtual.toPath());
                    s.update(bufFicheiroAtual);

                    fis = new FileInputStream(pathFicheiroAtual);
                    fos = new FileOutputStream(pathFicheiroAtual + ".enc");
                    fosSig = new FileOutputStream(pathFicheiroAtual + ".signed." + username);

                    cos = new CipherOutputStream(fos, c);
                    byte[] b = new byte[16]; 
                    int j = fis.read(b);
                    while (j != -1) {
                        cos.write(b, 0, j);
                        j = fis.read(b);
                    }
                    //cos.write(s.sign()); //Maybe works like this
                    fosSig.write(s.sign());
                    
                    cos.close();
                    fis.close();
                    fos.close();
                    fosSig.close();

                    privateFunctions.sendFile(outputStream, pathFicheiroAtual + ".enc");
                    //manda a signature á parte, pode ou n ser necessario tirar de la
                    privateFunctions.sendFile(outputStream, pathFicheiroAtual + ".signed." + username);

                    //Delete on client side
                    File sign = new File(pathFicheiroAtual + ".signed." + username);
                    File enc = new File(pathFicheiroAtual + ".enc");
                    sign.delete();
                    enc.delete();
                } 
                // Append no strBuilder a resposta do server
                respostaDoServer = (String) inputStream.readObject();
                stringBuilder.append(respostaDoServer).append("\n");
            } else {
                //envia o "-1" como pathname para simbolizar nao existe no cliente ao servidor
                //(para nao ficar á espera)
                outputStream.writeObject("-1");
                //Da logo append da mensagem correta
                stringBuilder.append("Nao Existe\n");
            }
        }
        return stringBuilder.toString();
    }

    private static void printMenuDeOperacoes() {
        StringBuilder sb =  new StringBuilder("Menu:\n").append("CREATE <ws> <pass> # Criar um novo workspace.\n")
                                                        .append("   ADD <user1> <ws> # Adicionar utilizador <user1> ao workspace <ws>.\n") 
                                                        .append("   UP <ws> <file1> ... <filen> # Adicionar ficheiros ao workspace.\n" )
                                                        .append("   DW <ws> <file1> ... <filen> # Download de ficheiros do workspace para a maquina local.\n")
                                                        .append("   RM <ws> <file1> ... <filen> # Apagar ficheiros do workspace.\n")
                                                        .append("   LW # Lista os workspaces associados ao utilizador.\n")
                                                        .append("   LS <ws> # Lista os ficheiros dentro de um workspace.");
        System.out.println(sb.toString());
    }

    //alterei esta funçao para retornar resposta de server
    //comandos que nao necessitam de trabalhar resultado nao sao afetadas
    //comandos que necessitam passam a saber resposta do servidor
    private static String sendAndReceive(ObjectInputStream inputStream, ObjectOutputStream outputStream, String inputDoUser)
            throws IOException, ClassNotFoundException {
        String respostaDoServidor;
        outputStream.writeObject(inputDoUser);

        respostaDoServidor = (String) inputStream.readObject();

        System.out.println("Resposta: " + respostaDoServidor + "\n");
        return respostaDoServidor;
    }


    /*
     * Funcao que coloca os inputs do @args num array de tamanho 4. Input[0] corressponde ao IP_Server. Input[1] corressponde ao porto 
     * (O default eh 1234 caso nao seja escolhido nenhum). O Input[2] corresponde ao username. O Input[3] corresponde a password.
     * 
     */
    private static String[] verifyInput(String args[], Scanner sc){
        String input[] = new String[4];
        String serverAddress = "";
        String serverIP = "localHost";
        int port = -1;
        String user_id = "";
        String password = "";

        if (args.length > 2){
            try {
                serverAddress = args[0];
                String[] address = serverAddress.split(":");
                if (address.length > 1){
                    serverIP = address[0];
                    port = Integer.parseInt(address[1]);
                } else {
                    serverIP = address[0];
                    port = 12345;
                }

                user_id = args[1];
                password = args[2];

            } catch (NumberFormatException e){
                System.out.println("Porto não é um número válido");
            }

        } else {
            System.out.println("Tamanho dos argumentos insuficiente");
            return new String[1];
        }
        String[] credentials = getValidCredentials(sc, user_id, password);
        user_id = credentials[0];
        password = credentials[1];

        input[0] = serverIP;
        input[1] = String.valueOf(port);
        input[2] = user_id;
        input[3] = password;
        return input;
    }

    private static boolean isAlphanumeric(String str) {
        return str.matches("[a-zA-Z0-9]+");
    }
    

    /*
     * Ha duas versoes desta funcao. Esta recebe uns inputs inicias para verificar e depois mantem o loop ate
     * ser escrito no terminal inputs validos
     */
    private static String[] getValidCredentials(Scanner sc, String user_id, String password) {
        while (!isAlphanumeric(user_id) || !isAlphanumeric(password)) {
            System.out.println("Erro: user_id e password devem conter apenas letras e numeros.");
            System.out.print("Introduza um user e uma password separados por espaco:");
            
            String inputLine = sc.nextLine().trim();
            String[] parts = inputLine.split("\\s+", 2); // Divide no máximo em 2 partes
    
            if (parts.length < 2) {
                System.out.print("Erro: precisa de inserir um user e uma password separados por espaco.");
                continue;
            }
            user_id = parts[0];
            password = parts[1];
        }
        return new String[]{user_id, password};
    }

    /*
     * Ha duas versoes desta funcao. Esta recebe apenas o scanner e espera por inputa validos
     */
    private static String[] getValidCredentials(Scanner sc) {
        String user_id = "";
        String password = "";
    
        while (true) {
            System.out.println("Introduza um user_id e uma password separados por espaco:");

            String inputLine = sc.nextLine().trim();
           
            String[] parts = inputLine.split("\\s+", 2); // Divide no máximo em 2 partes
    
            if (parts.length < 2) {
                System.out.print("Erro: precisa de inserir um user e uma password separados por espaco.");
                continue;
            }
            user_id = parts[0];
            password = parts[1];
    
            if (!user_id.matches("[a-zA-Z0-9]+") || !password.matches("[a-zA-Z0-9]+")) {
                System.out.print("Erro: user e password devem conter apenas letras e numeros.");
                continue;
            }
            break; // Se chegou aqui, os valores são válidos
        }
        return new String[]{user_id, password};
    }

    private static void createHookShutdown(ObjectInputStream inputStream, ObjectOutputStream outputStream, Socket clientSocket){

        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {

                try {

                    System.out.println("Shutting down ...");

                    outputStream.writeObject("CLOSING");

                    outputStream.flush();

                    inputStream.close();

                    outputStream.close();

                    clientSocket.close();

                    //sc.close();

                } catch (IOException e) {

                    System.err.println("Error closing socket in shutdown hook: " + e.getMessage());

                } catch (NoSuchElementException e) {

                    System.err.println("Scanner closed");

                }
            }
        });
    }
}
