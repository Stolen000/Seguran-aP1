import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

public class tests {
    

//"workspacesFolder"
public static String authentification(ObjectOutputStream outStream, ObjectInputStream inStream, boolean findUser, File db, String user, String passwd) throws ClassNotFoundException{
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

				}catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}

				//Verificar credencias
				try{
					if (user.length() != 0 && passwd.length() != 0){									
						encontrouUser = false;
						
						Scanner sc = new Scanner(db);
						// Ficheiro user.txt vazio
						if (!sc.hasNextLine()) {

							firstUser(user, passwd, db);

							outStream.writeObject("OK-NEW-USER");
							privateWsFunc.create_new_ws(user);
							autentificado = true;
						} 
                        else {
							while (sc.hasNextLine() && !encontrouUser) {
								String linha = sc.nextLine();
								authenticationAux(linha, user, passwd, outStream, autentificado, encontrouUser);
							}
						}
						if (!encontrouUser && !autentificado) {
                            addNewUser(user, passwd, db);
							outStream.writeObject("OK-NEW-USER"); // User novo
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

        private void addNewUser(String user, String passwd, File db){
            String newUSerInfo = user + ":" + passwd + System.lineSeparator();
            try (FileWriter writer = new FileWriter(db, true)) {
                writer.write(newUSerInfo);
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
        private void firstUser(String user, String passwd, File db){
            System.out.println("Arquivo vazio, adicionando primeira entrada...");
            String userLine = user + ":" + passwd + System.lineSeparator();
            try (FileWriter writer = new FileWriter(db)) {
                writer.write(userLine);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        private void authenticationAux(String linha, String user, String passwd, ObjectOutputStream outStream, boolean autentificado, boolean encontrouUser){
            if (linha.contains(":")) {
                String[] parts = linha.split(":", 2);
                if (parts.length == 2) {
                    String username = parts[0].trim();
                    String password = parts[1].trim();
                    try{
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
                    catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }

        public static void addNewUser(String user, String passwd, File db){
            String newUSerInfo = user + ":" + passwd + System.lineSeparator();
            try (FileWriter writer = new FileWriter(db, true)) {
                writer.write(newUSerInfo);
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
        public static void firstUser(String user, String passwd, File db){
            System.out.println("Arquivo vazio, adicionando primeira entrada...");
            String userLine = user + ":" + passwd + System.lineSeparator();
            try (FileWriter writer = new FileWriter(db)) {
                writer.write(userLine);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
}



private void authentification(ObjectOutputStream outStream, ObjectInputStream inStream, boolean findUser, File db) throws ClassNotFoundException{
    boolean encontrouUser = findUser;
    boolean autentificado = false;
    while(!autentificado){

        try {
            user = (String)inStream.readObject();
            //System.out.print("User:" + user);
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
                    create_new_ws(user);
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
                    create_new_ws(user);
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

}






