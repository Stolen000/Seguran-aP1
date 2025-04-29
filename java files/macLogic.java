import javax.crypto.Mac;

import java.io.File;
import java.io.FileNotFoundException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.Scanner;

public class macLogic {

    /*
    B. O servidor deve verificar a integridade dos ficheiros de utilizadores e workspaces. 

        ///Para tal, cada ficheiro deve ser protegido com um MAC. ///
        
        O cálculo deste MAC utiliza uma chave simétrica calculada a partir da password do sistema mySharing 
        que é pedida ao utilizador quando se inicia a execução do servidor.
            -implementar pass do servidor
            -funcao que calcula chave simetrica com base na pass do server

        No início da sua execução, o servidor deve usar o MAC para verificar a integridade dos ficheiros.
            -executar em inicio

        Se o MAC estiver errado, o servidor deve imprimir um aviso e terminar imediatamente a sua
        execução. 

        Se não há MAC para os ficheiros, o servidor deve imprimir um aviso e perguntar ao
        utilizador (administrador do servidor mySharing) se pretende calcular o MAC. 
            -a ter em mente

        O MAC deve ser verificado em todos os acessos aos ficheiros de utilizadores e workspaces, 
        e atualizados caso o(s) ficheiro(s) seja(m) alterado(s).
            -a ter em mente

        Os MACs devem ser guardados em ficheiros próprios apenas para este efeito (um para utilizadores
        e outro para os workspaces).
            -ficheiro para users
            -ficheiro para server


        Criar MAC
        Criar MACclient

        estrutura MACserver
            -file1-MAC
            -file2-MAC
            ...
        

        estrutura MACclient
            -client1

        -o que preciso
            -saber tudo sobre macs
            -chaves simetricas


        o que fazer no server
            -criar atributo pass, passado no inicio do server
                -no momento de ligar o server afixa la(provavelmente vai ser fora da thread, pois nao depende do user)
            -criar atraves da pass a secretkey
                -so precisa ser calculada uma vez?
                -de que forma usar o salt?
            -situacoes de alteraçao de ficheiros user e ws.txt
                -create ws
                -add user to ws
                -inicio de sessao



                //perguntar se deve criar
				//user pode dizer que nao?? o que acontece?



			//caminhos possiveis
				//primeira iteraçao
					//sem macs
					//sem users e ws.txt
						//nao cria macs
					//em futuros acessos, quando forem criados users e ws.txt
						//criar ai os macs
							//essa funcao de criar macs tem de verificar se macs ja existem ou nao
								//senao cria los e seguir
					//e se houver macs e nao txts?
					//apagar MACS?

				//iteraçoes futuras
					//sem macs
					//com ficheiros users.txt e ws.txt
						//perguntar...
							//se nao 
								//gg go next
							//se sim
								//cria los com base nos txt
					
					//em todas as verificaçoes comparar com os macs de ficheiro mac
						//antes de ocorrer a mudança
							//se bom 
								//recalcular o mac e atualizar o ficheiro mac

			//ficheiro users.mac atualizar quando
                //verificar sempre mesmo com ficheiro vazio
                    //se ocorrer incoerencia fechar merdosamente
				//entra novo user, mais nada

			//ficheiro workspaces.mac atualizar quando
				//entra novo user
				//user faz create
				//user faz add
                

            onde coloquei as verificaçoes
                -dentro do authentification depois de receber user e pass do oir    --precisa de atualizar 
                                                                                    --ficheiro users e workspace

                -no case CREATE antes de iniciar qualquer funcao    --precisa de atualizar
                                                                    --ficheiro a atualizar workspace

                -no case ADD antes de iniciar qualquer funcao       --precisa de atualizar
                                                                    --ficheiro a atualizar workspace

            
            cobrir todas as partes
                --authentification  ficheiros a verificar
                                        --users, antes de adicionar novo user
                                            --e depois atualizar
                                                --nos dois sitios que da write no ficheiro atualizar
                                        --ws, antes de atualizar com novo ws
                                            --na funcao create new ws
                                                --utilizando a funcao do mySharingServer
                                                --e depois atualizar
                                                    --na func authentication depois dos dois create_ws

                --Create        ficheiro de workspace a verificar
                                    --ws, antes de iniciar o if dentro do case
                                        --atualizar
                                            --antes do break

                --ADD         ficheiros a verificar
                                --verificar ficheiro users usado no db.length
							    --verificar ficheiro workspace para tudo o resto
                                    --ficheiro ws atualizado depois do escrever_linha_no_ws func

                --RM          ficheiros a verificar
                                --ficheiro ws    

                --DW          ficheiros a verificar
                                --ficheiro ws

                --UP          ficheiros a verificar
                                --ficheiro ws

                --LW          ficheiros a verificar
                                --ficheiro ws

                --LS          ficheiros a verificar
                                --ficheiro ws

        erros de debug
            --se eu fechar o server com valores nos txts e macs
                --iniciar denovo com outra pass
                    --os macs sao alterados para usar a secret key da nova passe
                        --nao pode ser
                            --tem de comparar e se forem diferentes entao assumir que deu merda
                                --ou nao passe ou nos macs
                --ao entrar um cliente o mac dos users nao estaha a atualizar
                --e mais cocó depois ver isso

    */
        //criar os ficheiros MAC de users e workspaces
        //chamados users.mac
        //chamado workspaces.mac

        public static String calcularMACBase64(String filename, Key key){
            try{
                // Initialize MAC
                Mac mac = Mac.getInstance("HmacSHA1");
                mac.init(key);

                //abrir file (macFileName + ".txt")
                    //verificar existencia
                        //retirar conteudo
                        //colocar em string
                    //se gg
                        //mandar msg de erro e terminar
                File file = new File(filename + ".txt");
                if (!file.exists()) {
                    System.err.println("Erro: o ficheiro " + filename + ".txt não existe.");
                    System.exit(1);
                }

                byte[] buf = Files.readAllBytes(file.toPath());

                mac.update(buf);
                byte[] macResult = mac.doFinal();
                return Base64.getEncoder().encodeToString(macResult);
            }catch(Exception e){
                System.err.println("peta a calcular o mac do ficheiro " + e.getMessage());
                System.exit(1);
            }
            return null;
        }

        public static void atualizarMAC(String filename, Key key) {
            try {
                String novoMacBase64 = calcularMACBase64(filename, key);

                if (novoMacBase64 != null) {
                    String macFileName = filename + ".mac";
                    Files.write(Paths.get(macFileName), novoMacBase64.getBytes());
                }

            } catch (Exception e) {
                System.err.println("MAC nao foi atualizado " + e.getMessage());
            }
        }
        

        //para ser utilizada duas vezes
        //uma para users.txt
        //uma para workspaces.txt
        public static void createMacFile(String macFileName, Key key) {
            try {
                String macResult = calcularMACBase64(macFileName, key);
                Path path = Paths.get(macFileName + ".mac");

                Files.write(path, Collections.singletonList(macResult), StandardCharsets.UTF_8);

            } catch (Exception e) {
                System.out.println("Ocorreu um erro ao criar o ficheiro MAC: " + e.getMessage());
            }
        }


        //a ser utilizada duas vezes
        //uma para users
        //outra para workspaces
        public static String getMacFromFile(String filename) {
            //abrir o ficheiro users.mac
            //retornar conteudo em string
            File file = new File(filename + ".mac");
            if (!file.exists()) {
                System.err.println("Erro: o ficheiro users.mac não existe.");
                System.exit(1);
            }
            String mac = null;
            try (Scanner sc = new Scanner(file)) {
                if (sc.hasNextLine()) {
                    String macLine = sc.nextLine();
                    mac = macLine.trim(); //assumindo que o ficheiro mac a ler so tem uma linha corrida
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
            return mac;
        }

    public static boolean compareMacs(String mac1, String mac2) {
        try {
            byte[] mac1Bytes = Base64.getDecoder().decode(mac1);
            byte[] mac2Bytes = Base64.getDecoder().decode(mac2);

            return MessageDigest.isEqual(mac1Bytes, mac2Bytes);
        } catch (IllegalArgumentException e) {
            System.err.println("Ficheiro .mac corrompido.");
            return false;
        }
    }


    public static void autodestruir(){
        try {
            System.out.println("Ficheiros MAC comprometidos!!!");
            System.out.println("Sistema vai entrar em auto-destruição em 10 segundos");
            Thread.sleep(4000); 
            System.out.println("10");

            Thread.sleep(1000); 
            System.out.println("9");

            Thread.sleep(1000); 
            System.out.println("8");

            Thread.sleep(1000); 
            System.out.println("7");

            Thread.sleep(1000); 
            System.out.println("6");

            Thread.sleep(1000); 
            System.out.println("5");

            Thread.sleep(1000); 
            System.out.println("4");

            Thread.sleep(1000); 
            System.out.println("3");

            Thread.sleep(1000); 
            System.out.println("2");

            Thread.sleep(1000); 
            System.out.println("1");
            
            Thread.sleep(1000); 
            System.out.println("BOOOOOOOM!");
            System.exit(1);

        } catch (InterruptedException e) {
            System.err.println("A pausa foi interrompida.");
        }
    }        
}
