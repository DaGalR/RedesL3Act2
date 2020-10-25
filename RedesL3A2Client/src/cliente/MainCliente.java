package cliente;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;

public class MainCliente {
	
	public static int PUERTO;
	public static String SERVIDOR;
	public static String DIR_DESCARGA = "data/descargas/";
	public static int TAM_BUFFER = 1024;	
	
	public static void main(String[] args) throws IOException{
		
		//COMIENZA EJECUCION EN MAQUINA CLIENTE
		System.out.println("Cliente...");
		
		//PREPARACION DE SOCKET Y DE CONSOLA PARA ENTRADA DE USUARIO
		Socket socket = null;
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		
		try {
			//PETICION DE DATOS PARA COMENZAR INTENTO DE CONEXION
			//System.out.println("Ingresa la dirección IP del servidor");
			//SERVIDOR = br.readLine();
			System.out.println("Ingresa el puerto para recibir datos");
			PUERTO = Integer.parseInt(br.readLine());
			
			//CREACION DE SOCKET CON SERVIDOR
			DatagramSocket dsoc = new DatagramSocket(PUERTO);
	        InetAddress ip = InetAddress.getLocalHost(); 

			if(!dsoc.isClosed()) {
				
				//CONEXION CON SERVIDOR Y NOTIFICACION DE ESTADO DE CONEXION
				System.out.println("Estado de conexión: " + !dsoc.isClosed() + " Esperando ID y nombre de archivo...");
			}
			
			//CREACION DE CANAL ENTRADA Y SALIDA EN SOCKET

			int id;
			String nombreArchivo;
			DatagramPacket dpReceive = null; 
	        byte[] receive = new byte[TAM_BUFFER]; 

			//PROTOCOLO PENSADO: CUANDO SERVIDOR CREA SOCKET, ENVÍA "0", LUEGO EL ID DEL CLIENTE Y LUEGO EL NOMBRE DE ARCHIVO QUE ENVIA
            dpReceive = new DatagramPacket(receive, receive.length); 
            dsoc.receive(dpReceive); 
            String res = data(receive).toString();
            System.out.println("Client:-" + res);
            
			if(res.equals("0")) {
				dpReceive = new DatagramPacket(receive, receive.length); 
				dsoc.receive(dpReceive);
				res = data(receive).toString();
				id = Integer.parseInt(res) ;
				dpReceive = new DatagramPacket(receive, receive.length); 
				dsoc.receive(dpReceive);
				res = data(receive).toString();
				nombreArchivo = res;
				System.out.println("El archivo a recibir es "+ nombreArchivo + " y su ID es " + id );
				
				//ENVIAR NOTIFICACION DE PREPARADO PARA RECIBIR ARCHIVO
				System.out.println("Notificando al servidor que se está esperando el archivo...");
				String nuevo = "OK";
				byte[] buf = nuevo.getBytes();
				DatagramPacket dpSend = new DatagramPacket(buf, buf.length, ip, PUERTO); 
				
				//LLAMADO A METODO PARA RECIBIR ARCHIVO
				descargar(dsoc, id, nombreArchivo, receive);
				System.out.println("terminé descarga :D");
			}
			else {
				System.out.println("Falla de servidor");
			}
			
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		
		br.close();
		socket.close();
	}
	public static StringBuilder data(byte[] a) 
    { 
        if (a == null) 
            return null; 
        StringBuilder ret = new StringBuilder(); 
        int i = 0; 
        while (a[i] != 0) 
        { 
            ret.append((char) a[i]); 
            i++; 
        } 
        return ret; 
    } 
	/**
	 * Método que recibe archivo del servidor
	 * @param s socket creado con servidor
	 * @param id id del cliente asignado por servidor
	 * @param arch nombre de archivo a descargar
	 */
	public static void descargar(DatagramSocket s, int id, String arch, byte[] receive) {
		
		try {
			
			//CANAL DE LECTURA DE ARCHIVO 
			FileOutputStream fos = new FileOutputStream(DIR_DESCARGA + arch);
			BufferedOutputStream bos = new BufferedOutputStream(fos);		
			DatagramPacket dpReceive = null;

			//RECEPCION DE HASH DEL SERVIDOR
			String res = null;
			
			dpReceive = new DatagramPacket(receive, receive.length); 
			s.receive(dpReceive);
			res = data(receive).toString();
			String hashServidor = res;
			
			dpReceive = new DatagramPacket(receive, receive.length); 
			s.receive(dpReceive);
			res = data(receive).toString();
			byte[] buffer = new byte[Integer.parseInt(res)];
			
			//System.out.println("hash recibido server " + hashServidor);
			for(int i = 0; i <= buffer.length - (buffer.length % TAM_BUFFER); i +=TAM_BUFFER ) {
				
				if(i == buffer.length - (buffer.length % TAM_BUFFER)) {
					dpReceive = new DatagramPacket(receive, receive.length); 
					s.receive(dpReceive);
					for(int j = i; j<=i+TAM_BUFFER;j++) {
						buffer[j] = receive[j];
					}
				}
				else {
					dpReceive = new DatagramPacket(receive, receive.length); 
					s.receive(dpReceive);
					for(int j = i; j<=buffer.length;j++) {
						buffer[j] = receive[j];
					}
					
				}
			}
			//ESCRIBE ARCHIVO
			bos.write(buffer, 0, buffer.length);
					
			bos.flush();
			
			//NOTIFICACION DE FINALIZACION AL USUARIO Y A SERVIDOR
			System.out.println("Archivo " + arch + " descargado en " + DIR_DESCARGA + " (" + buffer.length + " Bytes leídos).");
			
			//VERIFICACION DE INTEGRIDAD DE ARCHIVO
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			String hashCliente = checksum(DIR_DESCARGA+arch, sha);
			//System.out.println("Hexa cliente " +  hashCliente);
			System.out.println("Comenzando a verificar integridad de archivo descargado...");
			
			if(hashCliente.equals(hashServidor)) {
			//if(true) {
				//CONFIRMA RECEPCION DE ARCHIVO
				String num = "1";
				byte[] buf = num.getBytes();

				DatagramPacket dpSend = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(), PUERTO); 
				
				System.out.println("El archivo recibido ha sido examinado y no ha sido manipulado.");
			}
			else {
				System.out.println("¡¡ADVERTENCIA!! El archivo descargado no es igual al original");
			}
			
			bos.close();
			fos.close();
			s.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static String checksum(String ruta, MessageDigest md) throws IOException{
		try (DigestInputStream dis = new DigestInputStream(new FileInputStream(ruta), md)) {
            while (dis.read() != -1) ; //empty loop to clear the data
            md = dis.getMessageDigest();
        }

        // bytes to hex
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
	}
}
