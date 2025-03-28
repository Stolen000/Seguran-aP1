import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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

		ServerThread(Socket inSoc) {
			socket = inSoc;
			//System.out.println("thread do server para cada cliente");
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

				File workspaceFile = new File("workspaces.txt");
				if (!workspaceFile.exists()) {
					workspaceFile.createNewFile();
				}

				//file dos users com passwords
				File megaFold = new File("workspacesFolder");
				if (!megaFold.exists()) {
					megaFold.mkdir();
				}
				String isUserAuth = authentification(outStream, inStream, false, db);
				
			//Servidor tem que manter estruturas de dados com os dados dos users??

			//------------------------------------------------v
				while (true) {
					if(isUserAuth.equals("CLOSING")){
						System.out.println("Cliente: unknown fechou ligacao.");
						socket.close();
						break;
					}
					//Servidor espera pelo commando do cliente
					String comandoDoCliente = (String) inStream.readObject();
					String[] arrayDeArgumentos = comandoDoCliente.trim().split(" ");
					String comando = arrayDeArgumentos[0];


					if(comando != null && "CLOSING".equals(comando)){
						//Cntrl C do cliente
						System.out.println("Cliente:" + user + " fechou ligacao.");
						socket.close();
						//System.out.println("Socket closed.");
						//Sai do while e acaba
						break;
					}
					switch (comando) {
						//CREATE <ws>
						case "CREATE":
							//Checkar se <ws> já existe ou n
							//ficheiro novo/vazio
							if(workspaceFile.length() == 0){
								System.out.println("ws file vazio, adicionando primeiro workspace...");
								privateWsFunc.escreveLinhaNovaDoWsFile(arrayDeArgumentos[1],user);
								outStream.writeObject("OK");
								break;
							} else {
								if (!privateWsFunc.findWorkspace(arrayDeArgumentos[1]).equals("-1")){
									//encontrou ws com o nome dado
									outStream.writeObject("NOK");
									break;
								} else if(arrayDeArgumentos[1].startsWith("AutoWorkspace-")){
									System.out.println("Nao podes criar workspaces com nome generico");
									outStream.writeObject("NOK");
									break;
								}
								privateWsFunc.escreveLinhaNovaDoWsFile(arrayDeArgumentos[1],user);
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
							outStream.writeObject(privateWsFunc.addUserToWS(arrayDeArgumentos[2], arrayDeArgumentos[1], user));
							//-----------------------------
							break;
							
						//UP <ws> <file1> ... <filen>
						//Funciona tudo bem mas n funciona caso ja exista ficheiro
						case "UP":
							String workspaceUPPath = arrayDeArgumentos[1];
							String wsUP = privateWsFunc.findWorkspace(workspaceUPPath);
							//verificar Se ws existe, cliente n pertence ao ws NOWS | NOPERM
							if(wsUP.equals("-1")){
								outStream.writeObject("NOWS");
								break;
							}
							if(!privateWsFunc.doesUserHavePermsForWS(outStream, wsUP, user)){
								outStream.writeObject("NOPERM");
								break;
							}
							//Se a ws contem o user como owner ou utilizador
							outStream.writeObject("OK");
							receiveFilesAndRespond(outStream, inStream, arrayDeArgumentos, workspaceUPPath);
							break;

						//RM <ws> <file1> ... <filen>
						case "DW":
							String workspaceDWPath = arrayDeArgumentos[1];
							String wsDW = privateWsFunc.findWorkspace(workspaceDWPath);
							//verificar Se ws existe, cliente n pertence ao ws NOWS | NOPERM
							if(wsDW.equals("-1")){
								outStream.writeObject("NOWS");
								break;
							}
							if(!privateWsFunc.doesUserHavePermsForWS(outStream, wsDW, user)){
								outStream.writeObject("NOPERM");
								break;
							}
							//Se a ws contem o user como owner ou utilizador
							outStream.writeObject("OK");

							//Preparar para enviar
							dwSendFiles(inStream,outStream,arrayDeArgumentos);
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
							List<String> userWs = privateWsFunc.ListOfAssociatedWS(user);
							String[] lista = userWs.toArray(new String[0]);
							outStream.writeObject(privateFunctions.formatMsg(lista));
							break;

						case "LS":

							File wsFile = new File("workspaces.txt");
							Scanner scanner = new Scanner(wsFile);
							String linha;
							boolean hasPerm = false;
							while (scanner.hasNextLine()) {
								linha = scanner.nextLine();
								//Encontrou um workspace com esse nome
								if (linha.startsWith(arrayDeArgumentos[1] + ":")) {		
									//formato wsName:owner>user/owner, user1, user2
									//separar linha em: wsName:owner>owner || user1 || user2
									String[] usersInWs = linha.split(", ");
			
									//verificar users
									for(String userInWs : usersInWs){
										if(userInWs.equals(user)){
											hasPerm = true;
										}
									}
								}
							}
							if(hasPerm){
								String filepath = "workspacesFolder" + File.separator + arrayDeArgumentos[1];
								File wsFolder = new File(filepath);
								String [] files = wsFolder.list();
								outStream.writeObject(privateFunctions.formatMsg(files));
							}
							else{
								outStream.writeObject("NOPERM");
							}
							break;

						default:
							System.out.println("Comando invalido, tente novamente.");
					}
				}
				outStream.close();
				inStream.close();
				socket.close();
                
            } catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
        }

		private String remove(String[] arrayDeArgumentos) throws FileNotFoundException {
			List <String> userWs = privateWsFunc.ListOfAssociatedWS(user);
			StringBuilder sb = new StringBuilder();
			if(privateWsFunc.findWorkspace(arrayDeArgumentos[1]) != "-1"){
				if(userWs.contains(arrayDeArgumentos[1])){
					for(int i = 2; i < arrayDeArgumentos.length; i++){
						boolean removed = privateWsFunc.removeFile(arrayDeArgumentos[1], arrayDeArgumentos[i]);
						if(removed){
							sb.append(arrayDeArgumentos[i] + ": APAGADO").append(System.lineSeparator());
						}
						else{
							sb.append("O ficheiro " + arrayDeArgumentos[i] + " não existe no workspace indicado");
						}
					}
				}
				else{
					sb.append(("NOPERM"));
				}
			}
			else{
				sb.append(("NOWS"));
			}
			return sb.toString();
		}


		private String authentification(ObjectOutputStream outStream, ObjectInputStream inStream, boolean findUser, File db) throws ClassNotFoundException{
			boolean encontrouUser = findUser;
			boolean autentificado = false;
			while(!autentificado){

				try {
					user = (String)inStream.readObject();
					//System.out.print("User:" + user);
					if(user.equals("CLOSING")){
						return user;
					}
					passwd = (String)inStream.readObject();
					//System.out.print("Pass:" + passwd);

					//System.out.println("thread: depois de receber a password e o user");
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
							privateWsFunc.create_new_ws(user);
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


										if (username.toUpperCase().equals(user.toUpperCase()) && !encontrouUser){
											encontrouUser = true;
											System.out.println("User com caracteres iguais");
											outStream.writeObject("WRONG-PWD");
										}
									}
								}


							}


						}

						if (!encontrouUser && !autentificado) {

									
							sb.append(user).append(":").append(passwd).append(System.lineSeparator());
							try (FileWriter writer = new FileWriter(db, true)) {
								writer.write(sb.toString());
							}
							outStream.writeObject("OK-NEW-USER"); // User novo
							System.out.println("NOVO USER!!! UPI PORQUE NAO ENCONTROU NENHUM");;
							privateWsFunc.create_new_ws(user);
							autentificado = true;
						}
			
						sc.close();

					} else {
						outStream.writeObject(new String("WRONG-PWD")); // Invalid
					}
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			return autentificado ? "true" : "false";
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

		private void receiveFilesAndRespond(ObjectOutputStream outStream, ObjectInputStream inStream,
				String[] arrayDeArgumentos, String workspacePath) throws IOException, ClassNotFoundException {
			String pathname;
			String stringDeResposta;
			boolean isFileNewInThisWS;
			for (int i = 2; i < arrayDeArgumentos.length; i++) {
			    pathname = (String) inStream.readObject();
			    if(!pathname.equals("-1")){
			        //Checkar se existe no ws um com o nome igual
			        //E enviar ao server
					//check se o path do ficheiro ja existe na ws
					isFileNewInThisWS = !privateFunctions.isFileInWorkspace(pathname,workspacePath);
					//Envia ao cliente a validade do seu ficheiro (Se já existe no ws ou n)
					outStream.writeObject(isFileNewInThisWS);
					if(isFileNewInThisWS){
						//Ficheiro é novo logo pode receber
						privateFunctions.receiveFile(inStream, pathname, workspacePath);
						stringDeResposta = "OK";
					} else {
						//Existe um ficheiro com o mesmo nome no servidor
						stringDeResposta = "Existe um ficheiro com o mesmo nome no servidor";
					}
					//Envia resposta ao Cliente ou "OK" ou "Existe um ficheiro com o mesmo nome no servidor"
					outStream.writeObject(stringDeResposta);
					stringDeResposta = "";
			    }
			    //Se for invalido apenas passar em frente, cliente trata do resto
			}
		}

		private void dwSendFiles(ObjectInputStream inputStream, ObjectOutputStream outputStream,
			String[] arrayDeArgumentos) throws IOException, ClassNotFoundException{
					
			String pathFicheiroAtual;
			File ficheiroAtual;
			boolean readBool;
			StringBuilder pathFicheiroAtualSb;
			for (int i = 2; i < arrayDeArgumentos.length; i++) {
				pathFicheiroAtualSb = new StringBuilder();
				pathFicheiroAtualSb.append("workspacesFolder")
								.append(File.separator)
								.append(arrayDeArgumentos[1])
								.append(File.separator)
								.append(arrayDeArgumentos[i]);
				pathFicheiroAtual = pathFicheiroAtualSb.toString();
				ficheiroAtual = new File(pathFicheiroAtual);

				//System.out.println(pathFicheiroAtual);

				if(ficheiroAtual.exists()){
					//Enviamos o pathname
					outputStream.writeObject(arrayDeArgumentos[i]);
					//Recebe validacao do client
					readBool = (boolean) inputStream.readObject();
					if(readBool){
						//Validou entao envia ficheiro
						privateFunctions.sendFile(outputStream, pathFicheiroAtual);
					} 
				} else {
					//envia o "-1" como pathname para simbolizar nao existe no ws ao cliente
					//(para nao ficar á espera)
					outputStream.writeObject("-1");
				}	
			}
		}
	}
}