import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class privateWorkspaceFunctions {

		public static boolean doesUserHavePermsForWS(ObjectOutputStream outStream, String ws, String user) throws IOException {
			return ws.contains(":" + user) || ws.contains(", " + user);
		}

        public static List<String> ListOfAssociatedWS(String user) throws FileNotFoundException {
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

        public static void create_new_ws(String username, String password){
			boolean created = false;
			StringBuilder sb = new StringBuilder();
			System.out.println("Estou aqui a criar uma nova workspace para o: " + username);
			try{
				File workspaceFile = new File("workspaces.txt");
				if (!workspaceFile.exists()) {
					workspaceFile.createNewFile();
				}
				Scanner sc = new Scanner(workspaceFile);
				System.out.println(sc.hasNextLine());
				while(sc.hasNextLine() && !created){
					String nextLine = sc.nextLine();
					if(nextLine.startsWith("workspace" + username + ":")){
						System.out.println("Este user ja tem workspace automatico");
					}else{
						sb.append("workspace").append(username).append(":").append(username)
						.append(">").append(username)
						.append(System.lineSeparator());
						try (FileWriter writer = new FileWriter(workspaceFile)) {
							writer.write(sb.toString());
						}
						created = true;
					}
				}
				if(!created){
					System.out.println("Estou aqui a criar uma nova workspace porque cheguei ao fim do txt");
					sb.append("workspace").append(username).append(":").append(username)
					.append(">").append(username)
					.append(System.lineSeparator());
					try (FileWriter writer = new FileWriter(workspaceFile, true)) {
						writer.write(sb.toString());
					}
				}
				sc.close();
			}catch(IOException e){
				e.printStackTrace();
			}

		}

        public static boolean findUser(String userToFind) throws FileNotFoundException{
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

        public static String findWorkspace(String workspaceToFind) throws FileNotFoundException{
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
		public static String addUserToWS(String workspace, String userToAdd, String user) throws IOException{
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
					
				} else {
					tempFile.delete();
				}
				
				return linha;
			}
		}

		public static void escreveLinhaNovaDoWsFile(String workspaceName, String user) throws IOException{
			File wsfile = new File("workspaces.txt");
			//nao devia ser criada aqui, mas so para assegurar
			File wsPath = new File(workspaceName);
			if(!wsPath.exists()){
				wsPath.mkdir();
			}
			StringBuilder strBldr = new StringBuilder();
			strBldr.append(workspaceName).append(":").append(user)
											.append(">").append(user)
											.append(System.lineSeparator());
			try (FileWriter writer = new FileWriter(wsfile, true)) {
				writer.write(strBldr.toString());
			}
		}

}

        	
