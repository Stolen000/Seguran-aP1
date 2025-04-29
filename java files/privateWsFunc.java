import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class privateWsFunc {
    
    public static boolean doesUserHavePermsForWS(ObjectOutputStream outStream, String ws, String user) throws IOException {
        return ws.contains(":" + user) || ws.contains(", " + user);
    }

    		//formatar resultado depois para a LW
    public static List<String> ListOfAssociatedWS(String user) throws FileNotFoundException {
        //Pesquisar ws e ficar com os que tem o user la associado, meter no sb no formato {<ws1>, <ws2>}
        List <String> userWs = new ArrayList<>();
        File file = new File("workspaces.txt");
        Scanner scanner = new Scanner(file);
        String linha;

        //System.out.println("User is " + user + "|");
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

    public static String findWorkspace(String workspaceToFind) throws FileNotFoundException{
        File file = new File("workspaces.txt");
        Scanner scanner = new Scanner(file);
        String linha;
        while (scanner.hasNextLine()) {
            linha = scanner.nextLine();
            //Encontrou um workspace com esse nome
            if ((linha.toUpperCase()).startsWith((workspaceToFind.toUpperCase()) + ":")) {
                //Encontrou ja o ws
                scanner.close();
                return linha;
            }
        }
        scanner.close();
        return "-1";
    }

    //Metodo do ADD, retorna o output para mandar ao servidor
		//NOPERM se user nao for owner do ws ou se userToAdd ja estah no ws
		//NOWS se nao existir o ws 
		//Retorna OK se sucesso
		public static String addUserToWS(String workspace, String userToAdd, String user) throws IOException{
			File wsFile = new File("workspaces.txt");
			Scanner scanner = new Scanner(wsFile);
			File tempFile = new File("temp.txt");
			try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
				String linha;
                //se user a dar Add eh owner do ws
                boolean ownerValid = false;
                //se user a adicionar nao pertence ja ao ws
                boolean userToAddValid = true;

                //se encontra @param workspace em workspaces.txt
                boolean foundWs = false;

				while (scanner.hasNextLine()) {
					linha = scanner.nextLine();
					//Encontrou um workspace com esse nome
					if (linha.startsWith(workspace + ":")) {
                        foundWs = true;

						//formato wsName:owner>user/owner, user1, user2
                        //separar linha em: wsName:owner>owner || user1 || user2
                        String[] usersInWs = linha.split(", ");

                        //verificar o owner
                        String[] isOwner = usersInWs[0].split(">");
                        //wsName:owner>owner dividido em wsName:owner> || owner
                        ownerValid = isOwner[1].equals(user);

                        //verificar users
                        for(String userInWs : usersInWs){
                            if(userInWs.equals(userToAdd)){
                                userToAddValid = false;
                            }
                        }
                        if(userToAddValid){
                            linha += ", " + userToAdd;
                        }

					}
					writer.println(linha);
				}
				writer.close();
                linha = "";
				if(!foundWs){
                    linha = "NOWS";
				} else if(!ownerValid || !userToAddValid){
                    linha = "NOPERM";
				}
                else{
                    linha = "OK";
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

        public static void create_new_ws(String username){
			StringBuilder sb = new StringBuilder();
			//System.out.println("Estou aqui a criar uma nova workspace para o: " + username);
			try{
				File workspaceFile = new File("workspaces.txt");
				if (!workspaceFile.exists()) {
					workspaceFile.createNewFile();
				}
				if(!mySharingServer.verifyFileMac("workspaces")){
					System.out.println("No autentication ficheiro MAC user estava corrompido");
					System.out.println("Tenho pena mas vou fechar");
					System.exit(1); 
				}
				sb.append("AutoWorkspace-").append(username);
				escreveLinhaNovaDoWsFile(sb.toString(),username);
                
			}catch(IOException e){
				e.printStackTrace();
			}
		}

        public static boolean removeFile(String ws, String fileName){
            boolean suces = false;
            StringBuilder sb = new StringBuilder("workspacesFolder").append(File.separator)
                                        .append(ws).append(File.separator).append(fileName);
            String filepath = sb.toString();
            //System.out.println("o file a deletar tem filepath = " + filepath);
            File toRemove = new File(filepath);
            if(toRemove.exists()){
                toRemove.delete();
                suces = true;
            }
    
            return suces;
        }
    
    
        public static void escreveLinhaNovaDoWsFile(String workspaceName, String user) throws IOException{
            
            File wsfile = new File("workspaces.txt");
            //nao devia ser criada aqui, mas so para assegurar
            File wsPath = new File("workspacesFolder" + File.separator + workspaceName);
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

        public static void createCifFile(String username, String ws, byte[] data) throws IOException {
            String filename = "workspacesFolder" + File.separator + ws + File.separator + ws + ".key." + username;
            File file = new File(filename);
            if(!file.exists()){
                file.createNewFile();
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
                fos.flush();
            }
        }


}
