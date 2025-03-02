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
    public static void main(String[] args){
        System.out.println("cliente : main");
        //Open socket
        Socket clientSocket = new Socket("localhost", 12345);
        //----
        ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

        //----------Guardar: UserID Password
        boolean respostaInvalida = null;

        do {
        String userInputUser = "UserD";
        String userInputPassword = "Password3";
        
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

        String respostaAutentificacao = (String) in.readObject();   
        respostaInvalida = respostaAutentificacao.equals("WRONG-PWD");
        if (respostaInvalida) {
            System.out.println("Resposta Invalida, tente novamente: ");
        }
        } while(respostaInvalida);
        if(respostaAutentificacao.equals("OK-NEW-USER") || respostaAutentificacao.equals("OK-USER")){
            //Servidor cria novo workspace e entra no loop de operaçoes ? || encontrou user
        }


        //Loop das operações
        while (true) {
            System.out.println("Menu:");
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

            //Get comando do user, verificar permissoes para as op UP DW RM e LS;


        }




        inputStream.close();
        outputStream.close();
        //----
        clientSocket.close();
    }
}
