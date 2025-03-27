import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class privateFunctions {
    
    //Funcao que recebe um ficheiro pela inStream dada, se for dado workspace mete o ficheiro no workspace como pasta,
    // se workspace == null, apenas mete o ficheiro na diretoria atual.
    public static void receiveFile(ObjectInputStream inStream, String pathname, String workspace){
        try {
            receiveFilePriv(inStream, pathname, workspace);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }


    //Funcao que envia pelo outStream um ficheiro cujo filepath é enviado por argumento
    public static void sendFile(ObjectOutputStream outStream, String filePath){
        try {
            sendFilePriv(outStream, filePath);
        } catch (Exception e) {
            return;
        }
    }


    public static boolean isFileInWorkspace(String filePathName, String workspacePath){
        return isFileInWorkspacePriv(filePathName, workspacePath);
    }
		private static void receiveFilePriv(ObjectInputStream inStream, String pathname, String workspace) throws ClassNotFoundException, IOException{
			int fileSize = (int) inStream.readObject();
            
			byte[] buffer = new byte[1024];

            //Se workspace for null ele mete so na pasta em que esta
            if(workspace != null){
				File workspaceFolder = new File("workspacesFolder" + File.separator + workspace);
				
                pathname = "workspacesFolder"+ File.separator + workspace + File.separator + pathname;
            } 
            //Criar o ficheiro
			File file = new File(pathname);
			file.createNewFile();

			FileOutputStream fileOutputStream = new FileOutputStream(file);
			BufferedOutputStream buffOutputStream = new BufferedOutputStream(fileOutputStream);
			int bytesRead = 0;
			int totalBytesRead = 0;
			while (totalBytesRead < fileSize && (bytesRead = inStream.read(buffer, 0, Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
				buffOutputStream.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
			}
			buffOutputStream.flush();
			fileOutputStream.close();
			buffOutputStream.close();

            return;
		}
    
    
		private static void sendFilePriv(ObjectOutputStream outStream, String filePath) throws IOException{
			File ficheiro = new File(filePath);
            int sizeFile = (int) ficheiro.length();

			outStream.writeObject(sizeFile);

			byte[] buffer = new byte[1024];
			FileInputStream fileInputStream = new FileInputStream(ficheiro);
			BufferedInputStream buffInputStream = new BufferedInputStream(fileInputStream);
			int bytes = 0;
			while((bytes = buffInputStream.read(buffer)) != -1) {
				outStream.write(buffer,0,bytes);
			}
			outStream.flush();
			fileInputStream.close();
			buffInputStream.close();

            return;
		}

        private static boolean isFileInWorkspacePriv(String filePathName, String workspacePath) {
			File worskspaceFile = new File("workspacesFolder" + File.separator + workspacePath);
			String[] filesInWs = worskspaceFile.list();
			if(filesInWs != null && Arrays.asList(filesInWs).contains(filePathName)){
				return true;
			}
			return false;
		}
}
