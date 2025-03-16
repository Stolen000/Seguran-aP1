import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.Scanner;





public class mySharingClient {
    public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException{
        System.out.println("cliente : main");
        
        
        //Recebe os argumentos e guarda o ServerAdress para dar connect, o Porto, o User e a Pass
        String inputs[] = mySharingClient.verifyInput(args);
        String serverIP = inputs[0];
        int port = Integer.parseInt(inputs[1]);
        String user_id = inputs[2];
        String password = inputs[3];        
        //


        //Open socket
        int finalPort = (port != -1) ? port : 12345;
        Socket clientSocket = new Socket(serverIP, finalPort);
        //----
        ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
       
        boolean respostaInvalida = true;
        String userInputUser;
        String userInputPassword;
        String respostaAutentificacao = "";

        Scanner scanner = new Scanner(System.in);
        userInputUser = user_id;
        userInputPassword = password;

        //----Handling CRl C
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    System.out.println("Shutting down ...");
                    outputStream.writeObject("CLOSING");
                    //Faz unstuck da main thread  no nextLine()
                    System.in.close();

                    

                    outputStream.flush();
                    inputStream.close();
                    outputStream.close();
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket in shutdown hook: " + e.getMessage());
                    e.printStackTrace();
                } catch (NoSuchElementException e){
                    System.err.println("Scanner closed");
                }
            }
        });
        
        try {

            while (respostaInvalida) {

                outputStream.writeObject(userInputUser);
                outputStream.writeObject(userInputPassword);

                //-----------------Se autentificado corretamente: OK-USER || Se novo user: OK-NEW-USER
                respostaAutentificacao = (String) inputStream.readObject();   
                respostaInvalida = respostaAutentificacao.equals("WRONG-PWD");
                if (respostaInvalida) {

                    //Fica a repetir o processo até introduzir a password correta ou um novo user e pass

                    System.out.print("Resposta Invalida, tente novamente (eg: Alberto:benfica): ");
                    
                    String input = scanner.nextLine();
                    String[] credentials = input.split(":");

                    userInputUser = credentials[0];
                    userInputPassword = credentials[1];
                    //System.out.println("Voce digitou: " + input);

                }
            }

            //Assegurar que a resposta do server é correta
            if(!respostaAutentificacao.equals("OK-NEW-USER") && !respostaAutentificacao.equals("OK-USER")){
                System.out.println("Resposta de Autentificacao falhada.");
            } else {
                //Servidor cria novo workspace e entra no loop de operaçoes ? || encontrou user

                //Declaracao de variaveis
                String inputDoUser;
                String comando;
                String[] arrayDeArgumentos;


                printMenuDeOperacoes();
                //Loop das operações
                while (true) {
                    //Menu das operacoes
                

                    //------------------v Input do comando do user
                    //inputDoUser = new String("CREATE workspace004");

                    boolean doneOperation = false;
                    System.out.print("Comando: ");
                    
                    inputDoUser = scanner.nextLine();
                    System.out.println();
                    //In progress:Tratar input
                    arrayDeArgumentos = inputDoUser.trim().split("\\s+");
                    comando = arrayDeArgumentos[0];

                    switch (comando) {
                        //CREATE <ws>
                        case "CREATE":
                            if(arrayDeArgumentos.length == 2){
                                sendAndReceive(inputStream, outputStream, inputDoUser);
                                doneOperation = true;
                            } 
                            break;
                            //se nao entrar no if ele cai no default

                        //ADD <user1> <ws>
                        case "ADD":
                            //precisa de mais tramento? (?)
                            if(arrayDeArgumentos.length == 3){
                                sendAndReceive(inputStream, outputStream, inputDoUser);
                                doneOperation = true;
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
                                    break;
                                } 
                                System.out.println(uploadFicheiros(inputStream, outputStream, arrayDeArgumentos));
                                doneOperation = true;
                            }    
                            break;    
                        //DW <ws> <file1> ... <filen>                
                        case "DW":
                            if(arrayDeArgumentos.length >= 3){
                                //mandou primeira mensagem
                                outputStream.writeObject(inputDoUser);
                                String respostaDoServer = (String) inputStream.readObject();
                                //Se nao foi validada a operacao, acabar
                                if(!respostaDoServer.equals("OK")){
                                    System.out.println("Resposta :" + respostaDoServer);
                                    doneOperation = true;
                                    break;
                                } 

                                //Preparar para receber
                                System.out.println(downloadFicheiros(inputStream,outputStream,arrayDeArgumentos)); 
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
                    

                    //------------------^

                




                    //Get comando do user, verificar permissoes para as op UP DW RM e LS;

                    //Create ws, se server aceitar recebe OK, se ja existe o ws recebe NOK
                    //
                    


                }
            }
        } catch (Exception e){
            System.out.println("");
        } 
        scanner.close();

        //inputStream.close();
        //outputStream.close();
        //----
        //clientSocket.close();
    }

    private static String downloadFicheiros(ObjectInputStream inputStream, ObjectOutputStream outputStream,
            String[] arrayDeArgumentos) throws ClassNotFoundException, IOException{
        
        String filePathAtual;
        boolean isFileNewInThisDir;
        String dirAtual = System.getProperty("user.dir");
        StringBuilder sBuilder = new StringBuilder("Resposta: ");
        
        //Percorre todos os ficheiros
        for (int i = 2; i < arrayDeArgumentos.length; i++) {
            //mete no strbuilder o path atual
            sBuilder.append(arrayDeArgumentos[i]).append(": ");
            //fica á espera do path para saber se existe no ws
            filePathAtual = (String) inputStream.readObject();
            if(!filePathAtual.equals("-1")){
                //eh valido (existe no server)
                //Pasta atual
                
                //checka se já existe na sua dir
                isFileNewInThisDir = !privateFunctions.isFileInWorkspace(filePathAtual, dirAtual);
                
                outputStream.writeObject(isFileNewInThisDir);
                if(isFileNewInThisDir){
                    privateFunctions.receiveFile(inputStream, filePathAtual, null);
                    sBuilder.append("OK\n");
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
            String[] arrayDeArgumentos) throws IOException, ClassNotFoundException {
        File ficheiroAtual;
        String pathFicheiroAtual;
        String respostaDoServer;
        StringBuilder stringBuilder = new StringBuilder("Resposta: ");
        boolean readBool;

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
                    privateFunctions.sendFile(outputStream, pathFicheiroAtual);
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
        StringBuilder sb =  new StringBuilder("Menu:\n").append("CREATE <ws> # Criar um novo workspace - utilizador é Owner.\n")
                                                        .append("ADD <user1> <ws> # Adicionar utilizador <user1> ao workspace <ws>." 
                                                        + "A operação ADD só funciona se o utilizador for o Owner do workspace <ws>.\n")
                                                        .append("UP <ws> <file1> ... <filen> # Adicionar ficheiros ao workspace.\n" )
                                                        .append("DW <ws> <file1> ... <filen> # Download de ficheiros do workspace para"
                                                            + "a máquina local.\n")
                                                        .append("RM <ws> <file1> ... <filen> # Apagar ficheiros do workspace.\n")
                                                        .append("LW # Lista os workspaces associados ao utilizador.\n")
                                                        .append("LS <ws> # Lista os ficheiros dentro de um workspace.");
        System.out.println(sb.toString());
    }

    private static void sendAndReceive(ObjectInputStream inputStream, ObjectOutputStream outputStream, String inputDoUser)
            throws IOException, ClassNotFoundException {
        String respostaDoServidor;
        outputStream.writeObject(inputDoUser);

        respostaDoServidor = (String) inputStream.readObject();

        System.out.println("Resposta: " + respostaDoServidor + "\n");
    }


    private static String[] verifyInput(String args[]){
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
            System.out.println("Input inválido");
        }


        input[0] = serverIP;
        input[1] = String.valueOf(port);
        input[2] = user_id;
        input[3] = password;
        return input;
    }
}
