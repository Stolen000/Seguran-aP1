package auxx;

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
        //Open socket
        Socket clientSocket = new Socket("localhost", 12345);
        //----
        ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

       
        boolean respostaInvalida = true;
        String userInputUser;
        String userInputPassword;
        String respostaAutentificacao = "";

        Scanner scanner = new Scanner(System.in);

        while (respostaInvalida) {
            
        
            //----------Guardar: UserID Password
            
            userInputUser = "UserD";
            userInputPassword = "Password3";
            
            //System.out.print("User:_ ");
            //String userInputUser = scanner.nextLine();

            //System.out.print("Password:_ ");
            //String userInputPassword = scanner.nextLine();

            //.....................................................

            //-----------------Envia o user e pass ao server para autentificaçao

            outputStream.writeObject(userInputUser);
            outputStream.writeObject(userInputPassword);

            //

            //-----------------Se autentificado corretamente: OK-USER || Se novo user: OK-NEW-USER
            respostaAutentificacao = (String) inputStream.readObject();   
            respostaInvalida = respostaAutentificacao.equals("WRONG-PWD");
            if (respostaInvalida) {
                System.out.println("Resposta Invalida, tente novamente: ");
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
                            File ficheiroAtual;
                            for (int i = 2; i < arrayDeArgumentos.length; i++) {
                                ficheiroAtual = new File(arrayDeArgumentos[i]) ;
                                if(!ficheiroAtual.exists()){
                                    //Mandar um sinal ao serv?
                                    //TODO Resto desta logica

                                }
                                
                                //Mandamos o comando por completo primeiro ao server para ele se perparar e dps começamos a mandar?
                                //Podemos mandar o comando + quantos ficheiros tentaremos (o length - 2) e dps o processo comeca?? 
                            }
                            break;
                        }        
                                               
                    case "DW":
        
                        break;  
                    case "RM":
                        break;
                    
                    case "LW":
                        break;
                    
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
}
