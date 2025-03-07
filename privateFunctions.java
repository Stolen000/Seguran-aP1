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
    //Retorna "1" se recebeu o ficheiro corretamente, "0" caso contrario e path se o file for invalido
    public static String receiveFile(ObjectInputStream inStream, String workspace){
        try {
            return receiveFilePriv(inStream, workspace);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "0";
    }


    //Funcao que envia pelo outStream um ficheiro cujo filepath é enviado por argumento
    //Retorna 1 se o envio foi completo, -1 se ficheiro foi invalido (path = "-1") e 0 os restantes casos
    public static String sendFile(ObjectOutputStream outStream, String filePath, boolean isValid){
        try {
            return sendFilePriv(outStream, filePath, isValid);
        } catch (Exception e) {
            return "0";
        }
    }

		private static String receiveFilePriv(ObjectInputStream inStream, String workspace) throws ClassNotFoundException, IOException{
            //Receber tamanho do ficheiro a receber
			int fileSize = (int) inStream.readObject();
			System.out.println("File size = " + fileSize);

            //Receber titulo do ficheiro
            String fileTitle = (String) inStream.readObject();
            System.out.println("File title: " + fileTitle);

            if(fileSize == -1){
                return fileTitle;
            }

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
                return "0";
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

            return "1";
		}
    
    
		private static String sendFilePriv(ObjectOutputStream outStream, String filePath, boolean isValid) throws IOException{
            //ficheiro a mandar
			File ficheiro = new File(filePath);

            //se for invalido size = -1
            int sizeFile = isValid ? (int) ficheiro.length() : -1;

            //Se for invalido manda -1 como size, e o server so recebe depois o path
            //Manda o size do ficheiro a mandar ao servidor
			sizeFile = (int) ficheiro.length();
			outStream.writeObject(sizeFile);

            //Manda o filepath do ficheiro a mandar ao servidor
            outStream.writeObject(filePath);

            // If the file is not valid, exit early
            if (!isValid) {
                return "-1";
            }
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

            return "1";
		}
}
