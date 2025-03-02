package auxx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;


public class mySharingServer{
	public static void main(String[] args) {
		System.out.println("servidor: main");
		mySharingServer server = new mySharingServer();
		server.startServer();
	}

	public void startServer (){
		ServerSocket sSoc = null;
		try {
			sSoc = new ServerSocket(12345);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
         
		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		}
		//sSoc.close();
	}


	//Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}
 
		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());


                String user = null;
				String passwd = null;
                try {
					user = (String)inStream.readObject();
					System.out.print("User:");
					System.out.println(user);
					passwd = (String)inStream.readObject();
					System.out.print("Pass:");
					System.out.println(passwd);
					System.out.println("thread: depois de receber a password e o user");
				}catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
                //------------------------


                //Authentification
				//AUTENTIFICAÇÂO //Se for invalido ficar á espera de novo input!! (do while) ou while
				if (user.length() != 0 && passwd.length() != 0){									
					boolean escritaFeita = false;
					StringBuilder sb = new StringBuilder();
					File db = new File("users.txt");
					if (!db.exists()) {
						db.createNewFile();
					}
					
					//-------
					Scanner sc = new Scanner(db);
					// Ficheiro user.txt vazio
					if (!sc.hasNextLine()) {
						System.out.println("Arquivo vazio, adicionando primeira entrada...");
						sb.append(user).append(":").append(passwd).append(System.lineSeparator());
						try (FileWriter writer = new FileWriter(db)) {
							writer.write(sb.toString());
						}
						outStream.writeObject("OK-NEW-USER");
						receiveFile(inStream, user);
					} else {
						while (sc.hasNextLine() && !escritaFeita) {
							String linha = sc.nextLine();
							if (linha.contains(":")) {
								String[] parts = linha.split(":", 2);
								if (parts.length == 2) {
									String username = parts[0].trim();
									String password = parts[1].trim();
									
									if (username.equals(user)) {
										outStream.writeObject(password.equals(passwd) ? "OK-USER" : "WRONG-PWD"); //User encontrado : Invalido
										if (password.equals(passwd)) sendFile(outStream, user);
										escritaFeita = true;
									}
								}
							}
						}
						if (!escritaFeita) {
							sb.append(user).append(":").append(passwd).append(System.lineSeparator());
							try (FileWriter writer = new FileWriter(db, true)) {
								writer.write(sb.toString());
							}
							outStream.writeObject("OK-NEW-USER"); // User novo
							receiveFile(inStream, user);
						}
					}
		
					sc.close();

				} else {
					outStream.writeObject(new String("WRONG-PWD")); // Invalid
				}

			//Servidor tem que manter estruturas de dados com os dados dos users??

			//Tem que ser implementada o ficheiro para guardar os workplaces


                //Closing Stuff
                outStream.close();
                inStream.close();

                socket.close();
                
                
            } catch (IOException e) {
				e.printStackTrace();
			}


            
        }
    }
}