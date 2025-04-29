import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
        } catch (Exception e) {
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

	public static void sendBytes(ObjectOutputStream outStream, byte[] bytes) {
		try{
			sendBytesPriv(outStream, bytes);
		}catch(Exception e){
			System.out.println("Erro a enviar bytes da cifra no sendBytes");
			e.printStackTrace();
		}

    }

	public static byte[] receiveBytes(ObjectInputStream inStream) {
		try{
			return receiveBytesPriv(inStream);
		}catch(Exception e){
			System.out.println("Erro a receber bytes da cifra no sendBytes");
			e.printStackTrace();
		}
		return null;
	}

	private static byte[] receiveBytesPriv(ObjectInputStream inStream) throws ClassNotFoundException, IOException {
		int fileSize = (int) inStream.readObject();  // tamanho dos dados a receber

		//System.out.println("tamanho do data a receber = " + fileSize);

		byte[] buffer = new byte[1024];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		int bytesRead = 0;
		int totalBytesRead = 0;
		while (totalBytesRead < fileSize &&
			(bytesRead = inStream.read(buffer, 0, Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
			baos.write(buffer, 0, bytesRead);
			totalBytesRead += bytesRead;
		}

		return baos.toByteArray();  // devolve o conteúdo como array de bytes
	}


    public static boolean isFileInWorkspace(String filePathName, String workspacePath){
        return isFileInWorkspacePriv(filePathName, workspacePath);
    }
	private static void receiveFilePriv(ObjectInputStream inStream, String pathname, String workspace) throws ClassNotFoundException, IOException{
		int fileSize = (int) inStream.readObject();
		byte[] buffer = new byte[1024];

		//Se workspace for null ele mete so na pasta em que esta
		if(workspace != null){
			//File workspaceFolder = new File("workspacesFolder" + File.separator + workspace);
			
			pathname = "workspacesFolder" + File.separator + workspace + File.separator + pathname;
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
	private static void sendBytesPriv(ObjectOutputStream outStream, byte[] data) throws IOException {
		int size = data.length;

		//System.out.println("tamanho do data a enviar = " + size);

		outStream.writeObject(size);  //envia o tamanho primeiro

		//agora envia os bytes em blocos
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		byte[] buffer = new byte[1024];
		int bytes;
		while ((bytes = bais.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytes);
		}
		outStream.flush();
		bais.close();
	}


	private static boolean isFileInWorkspacePriv(String filePathName, String workspacePath) {
		String dirAtual = "";
		File worskspaceFile;
		String[] filesInWs;
		if(workspacePath == null){
			dirAtual = System.getProperty("user.dir");
			worskspaceFile = new File(dirAtual);
		} else {
			worskspaceFile = new File("workspacesFolder" + File.separator + workspacePath);
		}
		filesInWs = worskspaceFile.list();
		return filesInWs != null && Arrays.asList(filesInWs).contains(filePathName);
	}

	public static String formatMsg(String[] lista){
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
}
