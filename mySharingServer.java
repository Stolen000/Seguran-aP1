import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;
import java.util.List;
import java.util.Scanner;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;


public class mySharingServer{

	//pass guardada no servidor colocada fora das threads
	private static String serverPass;
	private static Key serverSecretKey;
	private static boolean wantMacUser;
	private static boolean wantMacWs;

	public static void main(String[] args) {

		//colocar aqui afixacao de palavra passe
		//e colocar aqui tambem secretKey?

		wantMacUser = true;
		wantMacWs = true;

		System.out.println("Insira a Palavra passe para o Servidor");
		Scanner sc = new Scanner(System.in);
		serverPass = sc.nextLine();
		serverSecretKey = serverSecretKeyCalc();
		if(serverSecretKey == null){
			System.out.println("Aconteceu peta a gerar a secretKey, bazei");
			sc.close();
			return;
		}
		System.out.println("Tou com uma secret key mesmo ah maneira!");

		//antes disto verificar se existem os ficheiros users.txt e ws.txt
		//senao mesmo que user diga que quer criar macs nao tem peta pra criar
		
		//como workspaces.txt e users.txt sao ficheiros separados fazer a verificaçao separada

		//funcao que cria users.txt
		//funcao que cria workspaces.txt
		//funcao que cria diretorio
		//vars para cada uma delas

		boolean userTxt = initUsersTxt();
		boolean wsTxt = initWsTxt();
		initWsDir();

		//ver cada um dos casos
		//os que ja tinham sido criados e verificar seus macs
		//se algum foi criado agr criar o seu mac

		//fazer distinçao entre nao tenho ficheiros txt nem macs, e tenho ficheiros txt e nao tenho macs
		//para isso usar var booleana na funcao que verifica e cria os txt, se ela criar ent criar logo macs, se nao criar
		//significa que ja existem e entao entrar no txtHasMAcLOgic que distingue entre verificar os macs preexistentes
		//ou no caso de so faltarem macs, perguntar se criamos ou nao

		boolean userVerify;
		boolean wsVerify;
		if(!userTxt){
			userVerify = txtHasMacLogic("users");
		}
		else{
			//criar
			macLogic.createMacFile("users", serverSecretKey);
			userVerify = true;;
		}
		if(!wsTxt){
			wsVerify = txtHasMacLogic("workspaces");
		}
		else{
			//criar
			macLogic.createMacFile("workspaces", serverSecretKey);
			wsVerify = true;;
		}
		if(!userVerify || !wsVerify){
			macLogic.autodestruir();
		}

		System.out.println("Acabei de criar os ficheiros txt e de criar os macs se tiver sido necessario");
		sc.close();
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

	public static boolean verifyFileMac(String filename){
		boolean fileVerify;
		File file = new File(filename + ".mac");
		if (!file.exists()) {
			fileVerify = true;
		}
		else{

			String previousMacFile = macLogic.getMacFromFile(filename);
			
			String calcdMacFile = macLogic.calcularMACBase64(filename, serverSecretKey);


			fileVerify = macLogic.compareMacs(previousMacFile, calcdMacFile);		
		}
		return fileVerify;
	}

	private static boolean verifyMacs(){
		return verifyFileMac("users") && verifyFileMac("workspaces");
	}

	private static boolean txtHasMacLogic(String filename){
		boolean verify = false;
		if(hasMac(filename)){
			//verificar entao se funcionam 
			//verificar se este mac estah fixe

			//ainda tenho de mudar esta
			verify = verifyFileMac(filename);
		}
		else{
			Scanner sc = new Scanner(System.in);
			System.out.println("Nao existe ficheiro MAC para " + filename + ".txt deseja cria lo? (y/n)");
			String answer = sc.nextLine();
			if(answer.equals("y")){
				macLogic.createMacFile(filename, serverSecretKey);
				verify = true;
			}
			//trocar isto
			else{
				if(filename.equals("users")){
					wantMacUser = false;
				}
				else{
					wantMacWs = false;
				}
				System.out.println("Servidor vai ser executado sem ficheiros mac");
			}
		}
		return verify;
	}

	private static boolean hasMac(String filename){
		//so para checar se existem os ficheiros de macs
		File fileMac = new File(filename + ".mac");
		return fileMac.exists();
	}

	//criar o salt a usar no servidor
	//porque?
	//se o salt for aleatorio em todas as iteraçoes do server ent mesmo
	//com a mesma passe vao ser criadas secret keys diferentes sendo assim
	//sempre impossivel validar os macs dos ficheiros
	private static byte[] saltLogic() {
		byte[] systemSalt = new byte[16]; 
		try {
			File saltFile = new File("system_salt.bin");
	
			if (saltFile.exists()) {
				systemSalt = Files.readAllBytes(saltFile.toPath());
			} else {
				SecureRandom random = new SecureRandom();
				random.nextBytes(systemSalt);
				Files.write(saltFile.toPath(), systemSalt);
			}
		} catch (IOException e) {
			System.err.println("Erro ao lidar com o ficheiro de salt: " + e.getMessage());
			System.exit(1);
		}
	
		return systemSalt;
	}
	
	//retorna true se foi criado um novo users.txt
	//falso se ja existia
	private static boolean initUsersTxt(){
		boolean usersCreated = false;
		File db = new File("users.txt");
		if (!db.exists()) {
			try {
				db.createNewFile();
				usersCreated = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return usersCreated;

	}

	//retorna true se foi criado um novo workspaces.txt
	//falso se ja existia
	private static boolean initWsTxt(){
		boolean wsCreated = false;
		File workspaceFile = new File("workspaces.txt");
		if (!workspaceFile.exists()) {
			try {
				workspaceFile.createNewFile();
				wsCreated = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return wsCreated;
	}
	private static void initWsDir(){
		//file dos users com passwords
		File megaFold = new File("workspacesFolder");
		if (!megaFold.exists()) {
			megaFold.mkdir();
		}
	}

	//funçao para criar secretKey com base na pass dada na inicializaçao do server
	private static Key serverSecretKeyCalc(){

		byte[] salt = saltLogic();

		// Create HMAC key from password
		int iterations = 98765;
		int keyLength = 160; // HmacSHA1 uses 160-bit key
		PBEKeySpec keySpec = new PBEKeySpec(serverPass.toCharArray(), salt, iterations, keyLength);
		SecretKeyFactory kf;
		try {
			kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			byte[] keyBytes = kf.generateSecret(keySpec).getEncoded();
			return new SecretKeySpec(keyBytes, "HmacSHA1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
    }

	public void startServer (int port){

		//========================================V
		System.setProperty("javax.net.ssl.keyStore", "keystore.server");
		System.setProperty("javax.net.ssl.keyStorePassword", "keypass");
		//System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
		
		ServerSocketFactory ssf = SSLServerSocketFactory.getDefault( );
		//SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(9096);
		//ss.setNeedClientAuth(true);

		//========================================^
		
		int finalPort = (port != -1) ? port : 12345;
		//ServerSocket sSoc = null; Alterar por socket ssl
		SSLServerSocket sSoc = null;
		try{
			//System.err.println(finalPort);
			//sSoc = new ServerSocket(finalPort);		
			sSoc = (SSLServerSocket) ssf.createServerSocket(finalPort);	
			//sSoc.setNeedClientAuth(true);
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
		private String username;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			//System.out.println("thread do server para cada cliente");
		}

		public void run(){

			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				File db = new File("users.txt");
				File workspaceFile = new File("workspaces.txt");
				String isUserAuth = authentication(outStream, inStream, db);

				//atualizar aqui
				//user e workspace


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
						System.out.println("Cliente:" + username + " fechou ligacao.");
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

							//verificar mac de ws
							if(!verifyFileMac("workspaces")){
								System.out.println("No Create ficheiro workspace MAC estava corrompido");
								System.out.println("Tenho pena mas vou fechar");
								System.exit(1); 
							}

							if(workspaceFile.length() == 0){
								//System.out.println("ws file vazio, adicionando primeiro workspace...");
								privateWsFunc.escreveLinhaNovaDoWsFile(arrayDeArgumentos[1],username);

								
							} else {
								if (!privateWsFunc.findWorkspace(arrayDeArgumentos[1]).equals("-1")){
									//encontrou ws com o nome dado
									outStream.writeObject("NOK");
									break;
								} else if(arrayDeArgumentos[1].startsWith("AutoWorkspace-")){
									//System.out.println("Nao podes criar workspaces com nome generico");
									outStream.writeObject("NOK");
									break;
								}
								privateWsFunc.escreveLinhaNovaDoWsFile(arrayDeArgumentos[1],username);

							}
							if(wantMacWs){
							macLogic.atualizarMAC("workspaces", serverSecretKey);
							}

							//funcao agr de receber passe do user
							//e fazer toda a logica
							outStream.writeObject("OK");
							byte[] ownerCif = privateFunctions.receiveBytes(inStream);
							if(ownerCif != null){
								//criar ent o file
								System.out.print("entrei dentro do if no server CREATE");
								privateWsFunc.createCifFile(username, arrayDeArgumentos[1], ownerCif);
							}
							break;
							//FORMAT for <ws> FILE : <ws>:<owner> ><user>,<user>,...
							
						//ADD <user1> <ws>
						case "ADD":
							//procurar o user
							// Ficheiro user.txt vazio
							// || 
							//User nao existe

							//verificar ficheiro users usado no db.length
							//verificar ficheiro workspace para tudo o resto
							if(!verifyMacs()){
								System.out.println("No autentication ficheiros MAC estavam corrompidos");
								System.out.println("Tenho pena mas vou fechar");
								System.exit(1); 
							}

							if(!findUser(username) || db.length() == 0){
								outStream.writeObject("NOUSER");
								break;
							}

							//procurar o ws e vê se o user é o owner
							//----------------------Checkar se <ws> já existe ou n
							String result = privateWsFunc.addUserToWS(arrayDeArgumentos[2], arrayDeArgumentos[1], username);
							outStream.writeObject(result);
							//-----------------------------

							if(result.equals("OK")){
								if(wantMacWs){
									macLogic.atualizarMAC("workspaces", serverSecretKey);
								}
								
								//enviar data do ficheiro com path do user
								String filepath = "workspacesFolder" + File.separator + arrayDeArgumentos[2] + File.separator + arrayDeArgumentos[2] + ".key." + username;
								privateFunctions.sendFile(outStream, filepath);

								//depois receber data do ficheiro novo
								byte[] wrappedData = privateFunctions.receiveBytes(inStream);
								if(wrappedData != null){
									privateWsFunc.createCifFile(arrayDeArgumentos[1], arrayDeArgumentos[2], wrappedData);
								}
								else{
									System.out.println("Ocorreu um erro no add no server a receber bytes");
									return;
								}
								//assumir que recebemos bem os dados agr eh criar o ficheiro ws.key.userToAdd
								//agarrar na data e fazer ficheiro com path do user a adicionar
							}
							
							//atualizar ficheiro de ws com novo user no ws assigned
							break;
							
						//UP <ws> <file1> ... <filen>
						//Funciona tudo bem mas n funciona caso ja exista ficheiro
						case "UP":
							if(!verifyFileMac("workspaces")){
								System.out.println("No autentication ficheiro workspace MAC estava corrompido");
								System.out.println("Tenho pena mas vou fechar");
								System.exit(1); 
							}

							String workspaceUPPath = arrayDeArgumentos[1];
							String wsUP = privateWsFunc.findWorkspace(workspaceUPPath);
							//verificar Se ws existe, cliente n pertence ao ws NOWS | NOPERM
							if(wsUP.equals("-1")){
								outStream.writeObject("NOWS");
								break;
							}
							if(!privateWsFunc.doesUserHavePermsForWS(outStream, wsUP, username)){
								outStream.writeObject("NOPERM");
								break;
							}
							//Se a ws contem o user como owner ou utilizador
							outStream.writeObject("OK");
							
							//ENVIAR A CHAVE DO WS do USER cifrada
							StringBuilder pathWSPassUserEncrypted = new StringBuilder();
							pathWSPassUserEncrypted.append("workspacesFolder")
											.append(File.separator)
											.append(workspaceUPPath)
											.append(File.separator)
											.append(workspaceUPPath)
											.append(".key.")
											.append(username);
							String pathWSPassUserEncryptedFinal = pathWSPassUserEncrypted.toString();
							privateFunctions.sendFile(outStream, pathWSPassUserEncryptedFinal);
							System.out.println("Apos enviar chave cifrada");

							//Recebe os ficheiros
							receiveFilesAndRespond(outStream, inStream, arrayDeArgumentos, workspaceUPPath);
							break;

						//RM <ws> <file1> ... <filen>
						case "DW":
							if(!verifyFileMac("workspaces")){
								System.out.println("No autentication ficheiro workspace MAC estava corrompido");
								System.out.println("Tenho pena mas vou fechar");
								System.exit(1); 
							}

							String workspaceDWPath = arrayDeArgumentos[1];
							String wsDW = privateWsFunc.findWorkspace(workspaceDWPath);
							//verificar Se ws existe, cliente n pertence ao ws NOWS | NOPERM
							if(wsDW.equals("-1")){
								outStream.writeObject("NOWS");
								break;
							}
							if(!privateWsFunc.doesUserHavePermsForWS(outStream, wsDW, username)){
								outStream.writeObject("NOPERM");
								break;
							}
							//Se a ws contem o user como owner ou utilizador
							outStream.writeObject("OK");

							//Preparar para enviar
							dwSendFiles(inStream,outStream,arrayDeArgumentos);
							break;  
						case "RM":
							if(!verifyFileMac("workspaces")){
								System.out.println("No autentication ficheiro workspace MAC estava corrompido");
								System.out.println("Tenho pena mas vou fechar");
								System.exit(1); 
							}

							if(arrayDeArgumentos.length >= 2){
								String returned = remove(arrayDeArgumentos);
								outStream.writeObject(returned);
							}
							break;
												
						//LW
						case "LW":
							if(!verifyFileMac("workspaces")){
								System.out.println("No autentication ficheiro workspace MAC estava corrompido");
								System.out.println("Tenho pena mas vou fechar");
								System.exit(1); 
							}
							
							//Lista as WS associadas com um user no formato {<ws1>, <ws2>}
							List<String> userWs = privateWsFunc.ListOfAssociatedWS(username);
							String[] lista = userWs.toArray(new String[0]);
							outStream.writeObject(privateFunctions.formatMsg(lista));
							break;

						case "LS":
							if(!verifyFileMac("workspaces")){
								System.out.println("No autentication ficheiro workspace MAC estava corrompido");
								System.out.println("Tenho pena mas vou fechar");
								System.exit(1); 
							}

							File wsFile = new File("workspaces.txt");
							Scanner scanner = new Scanner(wsFile);
							String linha;
							boolean wsExists = false;
							boolean hasPerm = false;
							while (scanner.hasNextLine()) {
								linha = scanner.nextLine();
								//Encontrou um workspace com esse nome
								if (linha.startsWith(arrayDeArgumentos[1] + ":")) {	
									wsExists = true;
									//formato wsName:owner>user/owner, user1, user2
									//separar linha em: wsName:owner>owner || user1 || user2
									String[] usersInWs = linha.split(", ");
									String[] owner = usersInWs[0].split(">");
                                    //wsName:owner>owner dividido em wsName:owner> || owner
                                    usersInWs[0] = owner[1];
			
									//verificar users
									for(String userInWs : usersInWs){
										if(userInWs.equals(username)){
											hasPerm = true;
										}
									}
								}
							}
							if(!wsExists){
                                outStream.writeObject("NOWS");
                            }else if(hasPerm){
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
							//System.out.println("Comando invalido, tente novamente.");
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
			List <String> userWs = privateWsFunc.ListOfAssociatedWS(username);
			StringBuilder sb = new StringBuilder();
			if(privateWsFunc.findWorkspace(arrayDeArgumentos[1]) != "-1"){
				if(userWs.contains(arrayDeArgumentos[1])){
					for(int i = 2; i < arrayDeArgumentos.length; i++){
						boolean removed = privateWsFunc.removeFile(arrayDeArgumentos[1], arrayDeArgumentos[i]);
						String signFilepath = arrayDeArgumentos[i] + ".signed." + username;
						boolean removedSigned = privateWsFunc.removeFile(arrayDeArgumentos[1], signFilepath);
						if(removed && removedSigned){
							sb.append(arrayDeArgumentos[i] + ": APAGADO");
							sb.append(signFilepath + ": APAGADO");
						}
						else{
							sb.append("O ficheiro " + arrayDeArgumentos[i] + " nao existe no workspace indicado");
						}

						if (i < arrayDeArgumentos.length - 1) {
							sb.append(System.lineSeparator());
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

		/**
		 * Funcao que tenta autenticar um dado 'username' e 'password'.
		 * Verifica se o conjunto 'username' e 'password' encontra-se no ficheiro 'users.txt'.
		 * @param outStream canal de envio para o cliente
		 * @param inStream canal de rececao para mensagens vindas do cliente
		 * @param db ficheiro de texto onde guardamos o 'username' e 'password'
		 * @return se o user foi autenticado
		 * @throws ClassNotFoundException
		 */
		private String authentication (ObjectOutputStream outStream, ObjectInputStream inStream, File db) throws ClassNotFoundException{
			boolean encontrouUser = false;
			boolean autentificado = false;
			String inputUsername = "";
			String inputPassword = "";
			while(!autentificado){

				//verificar coerencia dos files macs
				if(!verifyFileMac("users")){
					System.out.println("No autentication ficheiro MAC user estava corrompido");
					System.out.println("Tenho pena mas vou fechar");
					System.exit(1); 
				}
				
				//Verificar credencias
				try{

					inputUsername = (String)inStream.readObject();
					//System.out.print("User:" + user);
					if(inputUsername.equals("CLOSING")){
						return inputUsername;
					}
					inputPassword = (String)inStream.readObject();
					//System.out.print("Pass:" + passwd);
					//System.out.println("thread: depois de receber a password e o user");

					if (inputUsername.length() != 0 && inputPassword.length() != 0){									
						encontrouUser = false;
						Scanner sc = new Scanner(db);

						// Ficheiro user.txt vazio
						if (!sc.hasNextLine()) {
							//System.out.println("Arquivo vazio, adicionando primeira entrada...");
							try (FileWriter writer = new FileWriter(db)) {
								addNewUser(inputUsername,inputPassword,writer); //Usar a funcao para adicionar o user corretamente
								if(wantMacUser){
									macLogic.atualizarMAC("users", serverSecretKey);
								}
							}
							outStream.writeObject("OK-NEW-USER");
							//System.out.println("NOVO USER!!!");;
							privateWsFunc.create_new_ws(inputUsername);

							byte[] ownerCif = privateFunctions.receiveBytes(inStream);
							if(ownerCif != null){
								//criar ent o file
								System.out.println("entrei dentro do if no server authentication a criar para o ws automatico");
								String wsPath = "AutoWorkspace-" + inputUsername;
								privateWsFunc.createCifFile(inputUsername, wsPath, ownerCif);
							}
							if(wantMacWs){
								macLogic.atualizarMAC("workspaces", serverSecretKey);
							}
							autentificado = true;
							
						} else {
							while (sc.hasNextLine() && !encontrouUser) {
								String linha = sc.nextLine();
								if (linha.contains(":")) {
									String[] parts = linha.split(":", 3);
									if (parts.length == 3) {
										String storedUsername = parts[0].trim();
										String storedHash = parts[1].trim();
										String storedSalt = parts[2].trim();
										
										if (storedUsername.equals(inputUsername)) {
											encontrouUser = true;
											if(verifyPassword(inputPassword,storedHash,storedSalt)){
												outStream.writeObject("OK-USER"); //User encontrado
												autentificado = true;
											} else {
												outStream.writeObject("WRONG-PWD"); //Invalido
											}

										}

										if (storedUsername.toUpperCase().equals(inputUsername.toUpperCase()) && !encontrouUser){
											encontrouUser = true;
											//System.out.println("User com caracteres iguais");
											outStream.writeObject("WRONG-PWD");
										}
									}
								}
							}
						}

						if (!encontrouUser && !autentificado) {
							//sb.append(user).append(":").append(passwd).append(System.lineSeparator());
							try (FileWriter writer = new FileWriter(db, true)) {
								addNewUser(inputUsername, inputPassword, writer);
								//writer.write(sb.toString());
							}
							outStream.writeObject("OK-NEW-USER"); // User novo
							//System.out.println("NOVO USER!!! PORQUE NAO ENCONTROU NENHUM");;
							privateWsFunc.create_new_ws(inputUsername);

							byte[] ownerCif = privateFunctions.receiveBytes(inStream);
							if(ownerCif != null){
								//criar ent o file
								System.out.println("entrei dentro do if no server authentication a criar para o ws automatico");
								String wsPath = "AutoWorkspace-" + inputUsername;
								privateWsFunc.createCifFile(inputUsername, wsPath, ownerCif);
							}
							if(wantMacWs){
								macLogic.atualizarMAC("workspaces", serverSecretKey);
							}
							autentificado = true;
						}
						username = inputUsername;
						sc.close();

					} else {
						outStream.writeObject(new String("WRONG-PWD")); // Invalid
					}
				}catch(ClassNotFoundException | IOException e){
					e.printStackTrace();
				}
			}
			if(wantMacUser){
				macLogic.atualizarMAC("users", serverSecretKey);
			}
			return autentificado ? "true" : "false";
		}


		private boolean verifyPassword(String inputPassword, String storedHash, String storedSalt) {
			// Decodificar o salt de Base64 para bytes
			byte[] salt = Base64.getDecoder().decode(storedSalt);
			String inputHash = "";
			try {
				// Combinar inputPassword com o salt
				byte[] passwordBytes = inputPassword.getBytes("UTF-8");
				byte[] combined = new byte[passwordBytes.length + salt.length];
				System.arraycopy(passwordBytes, 0, combined, 0, passwordBytes.length);
				System.arraycopy(salt, 0, combined, passwordBytes.length, salt.length);

				// Calcular o hash com SHA-256
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				byte[] hash = md.digest(combined);

				// Codificar o hash calculado para Base64
				inputHash = Base64.getEncoder().encodeToString(hash);

			} catch (Exception e) {
				System.out.println("Internal error during comparing hashes in User.txt");
			}

			// Comparar com o hash armazenado
			return inputHash.equals(storedHash);
		}

		private void addNewUser(String username, String password, FileWriter fw) {
			// Gerar salt aleatório de 16 bytes
			byte[] salt = new byte[16];
			SecureRandom sr = new SecureRandom();
			sr.nextBytes(salt);

			try {
				// Combinar password + salt
				byte[] passwordBytes = password.getBytes("UTF-8");
				byte[] combined = new byte[passwordBytes.length + salt.length];
				System.arraycopy(passwordBytes, 0, combined, 0, passwordBytes.length);
				System.arraycopy(salt, 0, combined, passwordBytes.length, salt.length);

				// Calcular o hash com SHA-256
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				byte[] hash = md.digest(combined);

				// Codificar hash e salt em Base64 para texto legível
				String hashB64 = Base64.getEncoder().encodeToString(hash);
				String saltB64 = Base64.getEncoder().encodeToString(salt);

				// Escrever para o ficheiro no formato desejado
				//FileWriter fw = new FileWriter("users.txt", true); // append
				fw.write(username + ":" + hashB64 + ":" + saltB64 + "\n");
				fw.close();
			} catch (Exception e) {
				System.out.println("Internal error during writing in User.txt");
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
						privateFunctions.receiveFile(inStream, pathname + ".signed." + username, workspacePath);
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
			String signAtual;
			File ficheiroAtual;
			File filesignAtual;
			boolean readBool;
			StringBuilder pathFicheiroAtualSb;
			StringBuilder pathSigntualSb;

			//ENVIAR A CHAVE DO WS do USER cifrada
			String workspaceUPPath = arrayDeArgumentos[1];
			StringBuilder pathWSPassUserEncrypted = new StringBuilder();
			pathWSPassUserEncrypted.append("workspacesFolder")
							.append(File.separator)
							.append(workspaceUPPath)
							.append(File.separator)
							.append(workspaceUPPath)
							.append(".key.")
							.append(username);
			String pathWSPassUserEncryptedFinal = pathWSPassUserEncrypted.toString();
			privateFunctions.sendFile(outputStream, pathWSPassUserEncryptedFinal);

			File wsFolder = new File("workspacesFolder" + File.separator + arrayDeArgumentos[1]);
			File [] fileArray = wsFolder.listFiles();
			
			
			//Ciclo principal 
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
						for (File file : fileArray) {
							System.out.println(file.toPath().toString());
							if(file.toString().contains(arrayDeArgumentos[i] + ".signed.")){
								System.out.println("Encontrou este: " + file.toString());
								pathSigntualSb = new StringBuilder();
								pathSigntualSb.append("workspacesFolder")
												.append(File.separator)
												.append(arrayDeArgumentos[1])
												.append(File.separator)
												.append(file.toString());
								signAtual = pathSigntualSb.toString();


								privateFunctions.sendBytes(outputStream, file.toString().getBytes());
								//File signFile = new File(signAtual);
								privateFunctions.sendBytes(outputStream, Files.readAllBytes(file.toPath()));
							}
						}
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