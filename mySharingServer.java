import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
			//System.err.println(finalPort);
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
		private String user;
		private String passwd;
		private int lastAutoWS_id = 1;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}
 
		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());


				//file dos users com passwords
				File db = new File("users.txt");
					if (!db.exists()) {
						db.createNewFile();
					}

				boolean encontrouUser = authentification(outStream, inStream, false, db);

				System.out.println(user);
				
			//Servidor tem que manter estruturas de dados com os dados dos users??

			//------------------------------------------------v
				while (true) {
					//Servidor espera pelo commando do cliente
					String comandoDoCliente = (String) inStream.readObject();
					String[] arrayDeArgumentos = comandoDoCliente.trim().split(" ");
					String comando = arrayDeArgumentos[0];

					File workspaceFile = new File("workspaces.txt");
							if (!workspaceFile.exists()) {
								workspaceFile.createNewFile();
							}

					switch (comando) {
						//CREATE <ws>
						case "CREATE":
							//Checkar se <ws> já existe ou n
							//ficheiro novo/vazio
							if(workspaceFile.length() == 0){
								System.out.println("ws file vazio, adicionando primeiro workspace...");
								escreveLinhaNovaDoWsFile(arrayDeArgumentos[1],user);
								outStream.writeObject("OK");
								break;
							} else {
								if (!findWorkspace(arrayDeArgumentos[1]).equals("-1")){
									//encontrou ws com o nome dado
									outStream.writeObject("NOK");
									break;
								} 
								escreveLinhaNovaDoWsFile(arrayDeArgumentos[1],user);
								outStream.writeObject("OK");
								break;
							}
							
							//FORMAT for <ws> FILE : <ws>:<owner> ><user>,<user>,...
							
						//ADD <user1> <ws>
						case "ADD":
							//procurar o user
							// Ficheiro user.txt vazio
							// || 
							//User nao existe
							if(!findUser(user) || db.length() == 0){
								outStream.writeObject("NOUSER");
								break;
							}
							
							//procurar o ws e vê se o user é o owner
							//----------------------Checkar se <ws> já existe ou n
							outStream.writeObject(addUserToWS(arrayDeArgumentos[2], arrayDeArgumentos[1], user));
							//-----------------------------
							break;
							
						//UP <ws> <file1> ... <filen>
						//Funciona tudo bem mas n funciona caso ja exista ficheiro
						case "UP":
							StringBuilder sBuilder = new StringBuilder();
							int quantosFiles = arrayDeArgumentos.length - 2;
							String ws = findWorkspace(arrayDeArgumentos[1]);
							//verificar Se ws existe, cliente n pertence ao ws NOWS | NOPERM
							if(ws.equals("-1")){
								//nao existe ws
								outStream.writeObject("NOWS");
								break;
							}
							//Se a ws contem o user como owner ou utilizador
							if(!ws.contains(":" + user) && !ws.contains(", " + user)){
								//nao tem permissoes
								outStream.writeObject("NOPERM");
								break;
							}
							outStream.writeObject("OK");

							String respostaString;
							String pathAtual;
							
							//Receber os ficheiros
							for (int i = 0; i < quantosFiles; i++) {
								pathAtual = arrayDeArgumentos[i+2];
								respostaString = privateFunctions.receiveFile(inStream, arrayDeArgumentos[1]);
								if(respostaString.equals("1")){
									//Correu bem 
									sBuilder.append(pathAtual + ": OK\n		  ");
								} else if(respostaString.equals("-2")){
									//se já existe no ws o file
									sBuilder.append(pathAtual + ": Já existe no Ws\n		  ");
								} else if(!respostaString.equals("0")){
									//invalido e respostaString tem o path invalido
									sBuilder.append(pathAtual + ": Nao Existe\n		  ");
								} else{
									sBuilder.append(pathAtual + ": ERROR\n		  ");
								}
							}
							
							//Final mandamos a mensagem completa para o cliente.
							outStream.writeObject(sBuilder.toString());	
							
							      
													
						case "DW":
			
							break;  
						case "RM":
							if(arrayDeArgumentos.length >= 2){
								String returned = remove(arrayDeArgumentos);
								outStream.writeObject(returned);
							}
							break;
												
						//LW
						case "LW":
							//Lista as WS associadas com um user no formato {<ws1>, <ws2>}
							List<String> userWs = ListOfAssociatedWS(user);
							String[] lista = userWs.toArray(new String[0]);
							outStream.writeObject(formatMsg(lista));
							break;

						case "LS":
							String filepath = arrayDeArgumentos[0] + File.separator;
							File wsFolder = new File(filepath);
							String [] files = wsFolder.list();
							outStream.writeObject(formatMsg(files));

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

		private String formatMsg(String[] lista){
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			for(int i = 0; i < lista.length; i++){
				if(i == lista.length - 1){
					sb.append(lista[i]);
				}
				else{
					sb.append(lista[i] + ", ");
				}
			}
			sb.append("}");
			return sb.toString();
		}

		private String remove(String[] arrayDeArgumentos) throws FileNotFoundException {
			//argumentos
			//workspace
				//workspace existente
				//user com autorizacao no workspace
			
			//files
				//file existe
			//apagar file
			List <String> userWs = ListOfAssociatedWS(user);
			StringBuilder sb = new StringBuilder();
			if(findWorkspace(arrayDeArgumentos[0]) != "-1"){
				if(userWs.contains(arrayDeArgumentos[0])){
					for(int i = 2; i < arrayDeArgumentos.length; i++){
						boolean removed = removeFile(arrayDeArgumentos[1], arrayDeArgumentos[i]);
						if(removed){
							sb.append(arrayDeArgumentos[i] + ": APAGADO \n\n");
						}
						else{
							sb.append("O ficheiro " + arrayDeArgumentos[i] + " não existe no workspace indicado\n\n");
						}
					}
				}
				else{
					// mandar msg NOPERM
					sb.append(("NOPERM"));
				}
			}
			else{
				//mandar msg NOWS
				sb.append(("NOWS"));
			}
			return sb.toString();
		}

		//formatar resultado depois para a LW
		private List<String> ListOfAssociatedWS(String user) throws FileNotFoundException {
			//StringBuilder strBuilder = new StringBuilder("{ ");
			//Pesquisar ws e ficar com os que tem o user la associado, meter no sb no formato {<ws1>, <ws2>}
			List <String> userWs = new ArrayList<>();
			File file = new File("workspaces.txt");
			Scanner scanner = new Scanner(file);
			String linha;

			System.out.println("User is " + user + "|");
			while (scanner.hasNextLine()) {
				linha = scanner.nextLine();
				if(linha.contains(":" + user) || linha.contains(", " + user)){
					//eh o owner ou faz parte
					//da append do nome do workspace
					userWs.add(linha.substring(0, linha.indexOf(":")));
				}
				
			}
			scanner.close();
			return userWs;
		}

		private boolean removeFile(String ws, String fileName){
			boolean suces = false;
			String filepath = ws + File.separator + fileName;

			File toRemove = new File(filepath);
			if(toRemove.exists()){
				toRemove.delete();
				suces = true;
			}

			return suces;
		}

		private boolean authentification(ObjectOutputStream outStream, ObjectInputStream inStream, boolean findUser, File db) throws ClassNotFoundException{
			boolean encontrouUser = findUser;
			boolean autentificado = false;
			while(!autentificado){

				try {
					user = (String)inStream.readObject();
					System.out.print("User:" + user);
					passwd = (String)inStream.readObject();
					System.out.print("Pass:" + passwd);

					System.out.println("thread: depois de receber a password e o user");
				}catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}


				//Verificar credencias
				try{
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
							System.out.println("NOVO USER!!! UPI");;
							create_new_ws(user,passwd);
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
								System.out.println("NOVO USER!!! UPI");;
								create_new_ws(user,passwd);
								autentificado = true;
							}
						}
			
						sc.close();

					} else {
						outStream.writeObject(new String("WRONG-PWD")); // Invalid
					}
				}catch(IOException e){
					e.printStackTrace();
				}
			}

			return encontrouUser;
		}

		private void create_new_ws(String username, String password){
			int index = lastAutoWS_id;
			boolean created = false;
			StringBuilder sb = new StringBuilder();
			System.out.println("Estou aqui a criar uma nova workspace para o: " + username);
			try{
				File workspaceFile = new File("workspaces.txt");
				if (!workspaceFile.exists()) {
					workspaceFile.createNewFile();
				}
				Scanner sc = new Scanner(workspaceFile);
				System.out.println(index);
				System.out.println(sc.hasNextLine());
				while(sc.hasNextLine() && !created){
					String nextLine = sc.nextLine();
					if(nextLine.startsWith("workspace" + String.valueOf(index) + ":")){
						index++;
						System.out.println("Estou aqui com index iguais");
					}else{
						sb.append("workspace").append(String.valueOf(index)).append(":").append(username)
						.append(">").append(username)
						.append(System.lineSeparator());
						try (FileWriter writer = new FileWriter(workspaceFile)) {
							writer.write(sb.toString());
						}
						lastAutoWS_id = index;
						created = true;
					};
				}
				if(!created){
					System.out.println("Estou aqui a criar uma nova workspace porque cheguei ao fim do txt");
					sb.append("workspace").append(String.valueOf(index)).append(":").append(username)
					.append(">").append(username)
					.append(System.lineSeparator());
					try (FileWriter writer = new FileWriter(workspaceFile, true)) {
						writer.write(sb.toString());
					}
					lastAutoWS_id = index;
				}
				sc.close();
			}catch(IOException e){
				e.printStackTrace();
			}



		}


		//Encontra user no ficheiro user.txt
		//Returns true se encontrou, falso caso contrario
		private boolean findUser(String userToFind) throws FileNotFoundException{
			File file = new File("users.txt");
			Scanner scanner = new Scanner(file);
			String linha;
			String[] parts;
			while (scanner.hasNextLine()) {
				linha = scanner.nextLine();
				if (linha.contains(":")) {
					parts = linha.split(":", 2);
					if (parts.length == 2 && parts[0].trim().equals(userToFind)) {
						scanner.close();
						return true;
					}
				}
				else{
					System.err.println("users.txt formato incorreto!");
				}
			}
			scanner.close();
			return false; 
		}
		
		private String findWorkspace(String workspaceToFind) throws FileNotFoundException{
			File file = new File("workspaces.txt");
			Scanner scanner = new Scanner(file);
			String linha;
			while (scanner.hasNextLine()) {
				linha = scanner.nextLine();
				//Encontrou um workspace com esse nome
				if (linha.startsWith(workspaceToFind + ":")) {
					//Encontrou ja o ws
					scanner.close();
					return linha;

				}
			}
			scanner.close();
			return "-1";
		}
    

		//Metodo do ADD, retorna o output para mandar ao servidor
		//NOPERM se user nao for owner do ws
		//NOWS se nao existir o ws 
		//Retorna OK se sucesso
		private String addUserToWS(String workspace, String userToAdd, String user) throws IOException{
			boolean foundAndOwner = false;
			File wsFile = new File("workspaces.txt");
			Scanner scanner = new Scanner(wsFile);
			File tempFile = new File("temp.txt");
			try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
				String linha;

				while (scanner.hasNextLine()) {
					linha = scanner.nextLine();
					//Encontrou um workspace com esse nome
					if (linha.startsWith(workspace + ":")) {
						if(!linha.startsWith(workspace + ":" + user)){
							//O user não é owner
							scanner.close();
							return "NOPERM";
						} else {
							//É o owner ent faz a add do user
							linha += ", " + userToAdd;
							foundAndOwner = true;
						}
					}
					writer.println(linha);
				}
				writer.close();

				if(foundAndOwner){
					linha = "OK";
				} else{
					linha = "NOWS";
				}		
				scanner.close();
				if (linha.equals("OK")) {
					if(wsFile.delete()){
						tempFile.renameTo(wsFile);
					} else {
						System.out.println("Something went wrong! 311");
					}
					
				}
				
				return linha;
			}
		}

		private void escreveLinhaNovaDoWsFile(String workspaceName, String user) throws IOException{
			File wsfile = new File("workspaces.txt");
			StringBuilder strBldr = new StringBuilder();
			strBldr.append(workspaceName).append(":").append(user)
											.append(">").append(user)
											.append(System.lineSeparator());
			try (FileWriter writer = new FileWriter(wsfile, true)) {
				writer.write(strBldr.toString());
			}
		}
	}
}