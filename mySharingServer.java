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
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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

				boolean encontrouUser = authentication(outStream, inStream, false, db);

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
								privateWorkspaceFunctions.escreveLinhaNovaDoWsFile(arrayDeArgumentos[1],user);
								outStream.writeObject("OK");
								break;
							} else {
								if (!privateWorkspaceFunctions.findWorkspace(arrayDeArgumentos[1]).equals("-1")){
									//encontrou ws com o nome dado
									outStream.writeObject("NOK");
									break;
								} 
								privateWorkspaceFunctions.escreveLinhaNovaDoWsFile(arrayDeArgumentos[1],user);
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
							if(!privateWorkspaceFunctions.findUser(user) || db.length() == 0){
								outStream.writeObject("NOUSER");
								break;
							}
							
							//procurar o ws e vê se o user é o owner
							//----------------------Checkar se <ws> já existe ou n
							outStream.writeObject(privateWorkspaceFunctions.addUserToWS(arrayDeArgumentos[2], arrayDeArgumentos[1], user));
							//-----------------------------
							break;
							
						//UP <ws> <file1> ... <filen>
						//Funciona tudo bem mas n funciona caso ja exista ficheiro
						case "UP":
							String workspaceUPPath = arrayDeArgumentos[1];
							String wsUP = privateWorkspaceFunctions.findWorkspace(workspaceUPPath);
							//verificar Se ws existe, cliente n pertence ao ws NOWS | NOPERM
							if(wsUP.equals("-1")){
								outStream.writeObject("NOWS");
								break;
							}
							if(!privateWorkspaceFunctions.doesUserHavePermsForWS(outStream, wsUP, user)){
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
							String wsDW = privateWorkspaceFunctions.findWorkspace(workspaceDWPath);
							//verificar Se ws existe, cliente n pertence ao ws NOWS | NOPERM
							if(wsDW.equals("-1")){
								outStream.writeObject("NOWS");
								break;
							}
							if(!privateWorkspaceFunctions.doesUserHavePermsForWS(outStream, wsDW, user)){
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
							List<String> userWs = privateWorkspaceFunctions.ListOfAssociatedWS(user);
							String[] lista = userWs.toArray(new String[0]);
							outStream.writeObject(formatMsg(lista));
							break;

						case "LS":
							String filepath = arrayDeArgumentos[1] + File.separator;
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
			if(lista != null){
				for(int i = 0; i < lista.length; i++){
					if(i == lista.length - 1){
						sb.append(lista[i]);
					}
					else{
						sb.append(lista[i] + ", ");
					}
				}
			}
			sb.append("}");
			return sb.toString();
		}

		private String remove(String[] arrayDeArgumentos) throws FileNotFoundException {
			System.out.println("entrei no remove function com os argumentos ws e file = " + arrayDeArgumentos[1] + " e " + arrayDeArgumentos[2]);

			List <String> userWs = privateWorkspaceFunctions.ListOfAssociatedWS(user);
			StringBuilder sb = new StringBuilder();
			if(privateWorkspaceFunctions.findWorkspace(arrayDeArgumentos[1]) != "-1"){
				if(userWs.contains(arrayDeArgumentos[1])){
					for(int i = 2; i < arrayDeArgumentos.length; i++){
						boolean removed = removeFile(arrayDeArgumentos[1], arrayDeArgumentos[i]);
						if(removed){
							sb.append(arrayDeArgumentos[i] + ": APAGADO");
						}
						else{
							sb.append("O ficheiro " + arrayDeArgumentos[i] + " não existe no workspace indicado");
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

		private boolean removeFile(String ws, String fileName){
			boolean suces = false;
			String filepath = ws + File.separator + fileName;
			System.out.println("o file a deletar tem filepath = " + filepath);
			File toRemove = new File(filepath);
			if(toRemove.exists()){
				toRemove.delete();
				suces = true;
			}

			return suces;
		}

		private boolean authentication(ObjectOutputStream outStream, ObjectInputStream inStream, boolean encontrouUser, File db) throws ClassNotFoundException {
			boolean autenticado = false;
			try {
				while (!autenticado) {
					try {
						String user = (String) inStream.readObject();
						String passwd = (String) inStream.readObject();
						System.out.println("User: " + user + " Pass: " + passwd);
						
						if (user.isEmpty() || passwd.isEmpty()) {
							outStream.writeObject("WRONG-PWD");
							continue;
						}
						
						encontrouUser = false;
						List<String> linhas = Files.readAllLines(db.toPath());
						
						if (linhas.isEmpty()) {
							registrarNovoUsuario(outStream, db, user, passwd);
							autenticado = true;
						} else {
							for (String linha : linhas) {
								String[] parts = linha.split(":", 2);
								if (parts[0].trim().equals(user)) {
									if (parts[1].trim().equals(passwd)) {
										outStream.writeObject("OK-USER");
										autenticado = true;
									} else {
										outStream.writeObject("WRONG-PWD");
									}
									encontrouUser = true;
									break;
								}
							}
							if (!encontrouUser) {
								registrarNovoUsuario(outStream, db, user, passwd);
								autenticado = true;
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return encontrouUser;
		}

		private void registrarNovoUsuario(ObjectOutputStream outStream, File db, String user, String passwd) throws IOException {
			String entrada = user + ":" + passwd + System.lineSeparator();
			try (FileWriter writer = new FileWriter(db)) {
				writer.write(entrada);
			}			outStream.writeObject("OK-NEW-USER");
			System.out.println("NOVO USER!!! UPI");
			privateWorkspaceFunctions.create_new_ws(user, passwd);
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
			for (int i = 2; i < arrayDeArgumentos.length; i++) {
				pathFicheiroAtual = arrayDeArgumentos[1] + File.separator + arrayDeArgumentos[i];
				ficheiroAtual = new File(pathFicheiroAtual);

				if(ficheiroAtual.exists()){
					
					//Enviamos o pathname
					outputStream.writeObject(arrayDeArgumentos[i]);
					//Recebe validaºão do client
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