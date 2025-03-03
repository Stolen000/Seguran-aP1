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
		int port = -1;
		
		if (args.length > 0){
			try {
				port = Integer.parseInt(args[0]);
			} catch(NumberFormatException e){
				System.out.println("Porto não é um número válido");
				return;
			}
		}
		
		server.startServer(port);
	}

	public void startServer (int port){
		
		int finalPort = (port != -1) ? port : 12345;
		ServerSocket sSoc = null;
		try{
			System.err.println(finalPort);
			sSoc = new ServerSocket(finalPort);			
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
				boolean autentificado = false;
				boolean encontrouUser;

				//file dos users com passwords
				File db = new File("users.txt");
					if (!db.exists()) {
						db.createNewFile();
					}


				while(!autentificado){

				
				
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


					//Authentification ---------------------------------------------------------------------------------vv
					//AUTENTIFICAÇÂO //Se for invalido ficar á espera de novo input!! (do while) ou while
					if (user.length() != 0 && passwd.length() != 0){									
						encontrouUser = false;
						StringBuilder sb = new StringBuilder();
						
						
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
							autentificado = true;
							

						} else {
							while (sc.hasNextLine() && !encontrouUser) {
								String linha = sc.nextLine();
								if (linha.contains(":")) {
									String[] parts = linha.split(":", 2);
									if (parts.length == 2) {
										String username = parts[0].trim();
										String password = parts[1].trim();
										
										if (username.equals(user)) {
											if(password.equals(passwd)){
												outStream.writeObject("OK-USER"); //User encontrado
												autentificado = true;
											} else {
												outStream.writeObject("WRONG-PWD"); //Invalido
											}
											 
											encontrouUser = true;
										}
									}
								}

								//-------Codigo que pode substituir o de cima, porém um pouco menos "seguro" ja que so checka por ":" mas nao quantos :(
								
								//if(linha.startsWith(user + ":")){
								//	outStream.writeObject(linha.trim().contains(":" + passwd) ? "OK-USER" : "WRONG-PWD"); //User encontrado : Invalido
								//			encontrouUser = true;
								//}
							}
							if (!encontrouUser) {
								sb.append(user).append(":").append(passwd).append(System.lineSeparator());
								try (FileWriter writer = new FileWriter(db, true)) {
									writer.write(sb.toString());
								}
								outStream.writeObject("OK-NEW-USER"); // User novo
								autentificado = true;
							}
						}
			
						sc.close();

					} else {
						outStream.writeObject(new String("WRONG-PWD")); // Invalid
					}
				}
				//Authentification ---------------------------------------------------------------------------------^^

			//Servidor tem que manter estruturas de dados com os dados dos users??

			//------------------------------------------------v
				while (true) {
					//Servidor espera pelo commando do cliente
					String comandoDoCliente = (String) inStream.readObject();
					String[] arrayDeArgumentos = comandoDoCliente.trim().split(" ");
					String comando = arrayDeArgumentos[0];
					boolean foundWS = false;

					File workspaceFile = new File("worksapces.txt");
							if (!workspaceFile.exists()) {
								workspaceFile.createNewFile();
							}

					switch (comando) {
						//CREATE <ws>
						case "CREATE":
							//Checkar se <ws> já existe ou n
							Scanner sc = new Scanner(workspaceFile);
							StringBuilder sbc = new StringBuilder();
							String linhaDoFile;
							//ficheiro novo/vazio
							if(!sc.hasNextLine()){
								System.out.println("ws file vazio, adicionando primeiro workspace...");
								sbc.append(arrayDeArgumentos[1]).append(":").append(user)
																.append(">").append(user)
																.append(System.lineSeparator());
								try (FileWriter writer = new FileWriter(workspaceFile)) {
									writer.write(sbc.toString());
								}
								outStream.writeObject("OK");
								break;
							} else {
								while (sc.hasNextLine()) {
									linhaDoFile = sc.nextLine();
									//Encontrou um workspace com esse nome
									if (linhaDoFile.startsWith(arrayDeArgumentos[1] + ":")) {
										outStream.writeObject("NOK");
										foundWS = true;
										break;
									}
								}
								if(foundWS){
									break;
								}
								//System.out.println("Didnt find a ws com o nome dado (sucesso)");

								//Podemos abstrair este append grande, assim como o append da autentificacao --vv
								sbc.append(arrayDeArgumentos[1]).append(":").append(user)
																.append(">").append(user)
																.append(System.lineSeparator());
								try (FileWriter writer = new FileWriter(workspaceFile, true)) {
									writer.write(sbc.toString());
								}
								outStream.writeObject("OK");
								break;
							}
							//FORMAT for <ws> FILE : <ws>:<owner> ><user>,<user>,...
							
						//ADD <user1> <ws>
						case "ADD":
							
							
							break;
							


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
						default:
							System.out.println("Comando invalido, tente novamente.");
					}
				}


				//Envia de volta a resposta depois das ops
				//------------------------------------------------^

				//Tem que ser implementada o ficheiro para guardar os workplaces


				//Closing Stuff
				//outStream.close();
				//inStream.close();

				//socket.close();
                
                
            } catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}


            
        }
    }
}