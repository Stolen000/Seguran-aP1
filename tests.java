import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class tests {
    


    private String addUserToWS(String workspace, String userToAdd, String user) throws IOException{
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
				if(!ownerValid || !userToAddValid){
                    linha = "NOPERM";
				} else if(!foundWs){
					linha = "NOWS";
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
}



private String addUserToWS(String workspace, String userToAdd, String user) throws IOException{
    boolean foundAndOwner = false;
    boolean invitedAlreadyIn = false;
    File wsFile = new File("workspaces.txt");
    Scanner scanner = new Scanner(wsFile);
    File tempFile = new File("temp.txt");
    try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
        String linha;

        //podia percorrer linha do workspace em questao e fazer varredura de users, 
        //primeiro elemento tem de ser o owner, procurar se user em questao estah nas restantes

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
