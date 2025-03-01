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




public class clientInterface {
    public static void main(String[] args){
        System.out.println("cliente : main");
        //Open socket
        Socket clientSocket = new Socket("localhost", 12345);
        //----
        ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

        //----------Guardar: UserID Password

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

        //-----------------Se autentificado corretamente: OK-USER || Se novo user: OK-NEW-USER








        inputStream.close();
        outputStream.close();
        //----
        clientSocket.close();
    }
}
