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


                System.out.print("Comando: ");
                inputDoUser = scanner.nextLine();
                System.out.println();
                //In progress:Tratar input
                arrayDeArgumentos = inputDoUser.trim().split(" ");
                comando = arrayDeArgumentos[0];

                switch (comando) {
                    //CREATE <ws>
                    case "CREATE":
                        if(arrayDeArgumentos.length == 2){
                            sendAndReceive(inputStream, outputStream, inputDoUser);
                            
                            break;
                        } 
                        //se nao entrar no if ele cai no default

                    //ADD <user1> <ws>
                    case "ADD":
                        //precisa de mais tramento? (?)
                        if(arrayDeArgumentos.length == 3){
                            sendAndReceive(inputStream, outputStream, inputDoUser);
                            break;
                        }


                    //UP <ws> <file1> ... <filen>
                    case "UP":
                        if(arrayDeArgumentos.length >= 3){
                            //Checa se todos os ficheiros existem no cliente, se nao mete a "-1" para o serv responder com invalido
                            File ficheiroAtual;
                            //String mensagemParaServer = inputDoUser + sb.toString();
                            
                            //mandou primeira mensagem
                            outputStream.writeObject(inputDoUser);
                            
                            String respostaPrimeira = (String) inputStream.readObject();
                            //Se nao foi validada a operacao, acabar
                            if(!respostaPrimeira.equals("OK")){
                                System.out.println("Resposta: " + respostaPrimeira);
                                break;
                            } 

                            //Recebeu OK
                            boolean validade;

                            //Percorre todos os ficheiros e analisa se sao validos
                            for (int i = 2; i < arrayDeArgumentos.length; i++) {
                                ficheiroAtual = new File(arrayDeArgumentos[i]) ;
                                validade = ficheiroAtual.exists();
                                privateFunctions.sendFile(outputStream, arrayDeArgumentos[i], validade);
                            }

                            //receber a mensagem final do servidor
                            String respostaFinal = (String) inputStream.readObject();
                            System.out.println("Resposta: " + respostaFinal);
                            
                            //Servidor tem que saber o nome de todos os ficheiros, e saber qual é invalido

                            //send input -> esperar validação -> comecar a mandar -> esperar ok outra vez?
                            
                            //sendAndReceive(inputStream, outputStream, inputDoUser);
                            break;
                        }        



                        
                                               
                    case "DW":

                        //privateFunctions.sendFile(outputStream,filepath);
        
                        break;  
                    case "RM":
                        break;
                    
                    case "LW":
                        if(arrayDeArgumentos.length == 1){
                            sendAndReceive(inputStream, outputStream, inputDoUser);
                            break;
                        }
                    
                    case "LS":
                        break;
                    case "HELP":
                        printMenuDeOperacoes();
                        break;
                    default:
                        System.out.println("Comando invalido, tente novamente.");
                        printMenuDeOperacoes();
                }

                //------------------^

               




                //Get comando do user, verificar permissoes para as op UP DW RM e LS;

                //Create ws, se server aceitar recebe OK, se ja existe o ws recebe NOK
                //
                


            }
        }


        



        scanner.close();

        inputStream.close();
        outputStream.close();
        //----
        clientSocket.close();
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
