import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class privateFunctions {
    
    //Funcao que recebe um ficheiro pela inStream dada, se for dado workspace mete o ficheiro no workspace como pasta,
    // se workspace == null, apenas mete o ficheiro na diretoria atual.
    //Retorna true se recebeu o ficheiro corretamente, false caso contrario
    public static boolean receiveFile(ObjectInputStream inStream, String workspace){
        try {
            return receiveFilePriv(inStream, workspace);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    //Funcao que envia pelo outStream um ficheiro cujo filepath é enviado por argumento
    //Retorna true se o envio foi completo, falso caso contrario
    public static boolean sendFile(ObjectOutputStream outStream, String filePath){
        try {
            return sendFilePriv(outStream, filePath);
        } catch (Exception e) {
            return false;
        }
    }

		private static boolean receiveFilePriv(ObjectInputStream inStream, String workspace) throws ClassNotFoundException, IOException{
            //Receber tamanho do ficheiro a receber
			int fileSize = (int) inStream.readObject();
			System.out.println("File size = " + fileSize);

            //Receber titulo do ficheiro
            String fileTitle = (String) inStream.readObject();
            System.out.println("File title: " + fileTitle);

            //buffer
			byte[] buffer = new byte[1024];

            //PathCompleto
            if(workspace != null){
                fileTitle = workspace + File.separator + fileTitle;
            } 
            
            System.out.println("Path completo: " + fileTitle);
			File file = new File(fileTitle);
			if(!file.exists()){
                file.createNewFile();
            } else {
                System.out.println("File ja existe");
                return false;
            }
            

			FileOutputStream fileOutputStream = new FileOutputStream(file);
			BufferedOutputStream buffOutputStream = new BufferedOutputStream(fileOutputStream);
			int bytesRead = 0;
			int totalBytesRead = 0;

			// Read until the total bytes read matches the expected file size
			while (totalBytesRead < fileSize && (bytesRead = inStream.read(buffer, 0, Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
				buffOutputStream.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
				//System.out.println("Bytes read in this iteration: " + bytesRead);
				//System.out.println("Total bytes read: " + totalBytesRead + "/" + fileSize);
			}

			buffOutputStream.flush();
			fileOutputStream.close();
			System.out.println("File received. Final size: " + file.length());
			buffOutputStream.close();

            return true;
		}
    
    
		private static boolean sendFilePriv(ObjectOutputStream outStream, String filePath) throws IOException{
            //ficheiro a mandar
			File ficheiro = new File(filePath);

            //checka se ficheiro existe ou nao
            if(!ficheiro.exists()){
                return false;
            }

            //Manda o size do ficheiro a mandar ao servidor
			int sizeFile = (int) ficheiro.length();
			outStream.writeObject(sizeFile);

            //Manda o filepath do ficheiro a mandar ao servidor
            outStream.writeObject(ficheiro);

			byte[] buffer = new byte[1024];

			FileInputStream fileInputStream = new FileInputStream(ficheiro);
			BufferedInputStream buffInputStream = new BufferedInputStream(fileInputStream);
			int bytes = 0;
            //envia bytes do tamanho do buffer
			while((bytes = buffInputStream.read(buffer)) != -1) {
				outStream.write(buffer,0,bytes);
			}
			outStream.flush();
			fileInputStream.close();
			buffInputStream.close();

            return true;
		}
}
