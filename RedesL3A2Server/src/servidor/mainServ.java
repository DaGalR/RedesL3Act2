package servidor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import servidor.ProtocoloServidor;


public class mainServ {
	private static ServerSocket ss;	
	private static final String MAESTRO = "MAESTRO: ";
	private static final String ARCHIVO_1 = "file-1.bin";
	//private static final String ARCHIVO_1 = "video1.mp4";
	private static final String ARCHIVO_2 = "test2.bin";
	private static final String RUTA_LOG = "data/log/";
	private static final String RUTA_ARCH = "data/archivos/";
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{


		System.out.println(MAESTRO + "Establezca puerto de conexion:");
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		int ip = Integer.parseInt(br.readLine());
		System.out.println(MAESTRO + "Empezando servidor maestro en puerto " + ip);

		System.out.println(MAESTRO + "Escribe el número del archivo a transmitir: 1 para el archivo de 100MiB o 2 para el archio de 250MiB");
		int archivo = Integer.parseInt(br.readLine());

		// Crea el archivo de log

		File archivoLog = null;

		String time = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(Calendar.getInstance().getTime());
		archivoLog = new File(RUTA_LOG + time + ".txt");
		escribirLog(archivoLog, "Fecha y hora de prueba " + time);
		File archivoEnviar;
		if(archivo == 1) {
			archivoEnviar = new File(RUTA_ARCH+ARCHIVO_1);
			escribirLog(archivoLog, "Nombre del archivo a enviar: " + ARCHIVO_1 + " de tamaño " + (int)archivoEnviar.length() + " Bytes");

		}
		else {
			archivoEnviar = new File(RUTA_ARCH+ARCHIVO_2);
			escribirLog(archivoLog, "Nombre del archivo a enviar: " + ARCHIVO_2 + " de tamaño " + (int)archivoEnviar.length() + " Bytes");
		}

		// Crea el socket que escucha en el puerto seleccionado.
		ss = new ServerSocket(ip);
		System.out.println("Por favor introduzca el número máximo de cientes que quiere atender (no más de 25) ");
		int nThreads = Integer.parseInt(br.readLine());

		DatagramSocket[] socketClientes = new DatagramSocket[nThreads];

		System.out.println("Esperando conexiones...");

		int conectados = 0;
		while(conectados < nThreads) {
			try {
				//INICIA PROTOCOLO ACEPTANDO CONEXION
				socketClientes[conectados] = ss.accept();
				DataOutputStream dout = new DataOutputStream(socketClientes[conectados].getOutputStream());
				DataInputStream din = new DataInputStream(socketClientes[conectados].getInputStream());
				System.out.println("Aceptando conexión de cliente numero: " + conectados);

				//TRAS ADMITIR CLIENTE Y CREAR COMUNICACION ENTRADA-SALIDA EN SOCKET SE ENVÍA EL ID AL CLIENTE				
				dout.writeByte(0);

				//SE NOTIFICA EL NOMBRE DEL ARCHIVO QUE SE ENVIARÁ
				if(archivo == 1) {
					dout.writeInt(conectados);
					dout.writeUTF(ARCHIVO_1);
					System.out.println("Escogio archivo 1");
				}
				else {
					dout.writeInt(conectados);
					dout.writeUTF(ARCHIVO_2);
					dout.flush();
					System.out.println("Escogio archivo 2");

				}

				//SE PREPARA RECEPCION DE SIGUIENTE CONEXION Y SE NOTIFICA QUE CLIENTE ESPERA
				//String var = din.readUTF();
				//System.out.println("var " + var);
				if(din.readUTF().contentEquals("OK")) {
					System.out.println("Cliente "+ conectados + " recibió ID y nombre de archivo");
				}
				//SE PREPARA LLEGADA DE SIGUIENTE CLIENTE
				conectados++;

			}
			catch(Exception e) {
				e.printStackTrace();
				System.out.println("Error en servidor! " );
			}
		}
		//ACABA WHILE, ES DECIR, YA LLEGARON CLIENTES ESPERADOS Y SE EMPIEZA ENVÍO DE ARCHIVO
		System.out.println("Comenzando envío de archivo a clientes...");
		//RECORRE N CLIENTES Y ENVÍA ARCHIVO
		for(int cli=0; cli<socketClientes.length;cli++) {
			escribirLog(archivoLog, "Enviando a cliente de ID " + cli);
			ProtocoloServidor ps = new ProtocoloServidor(socketClientes[cli], cli, archivo, archivoLog);

		}


	}
	/*
	 * Generacion del archivo log. 
	 * Nota: 
	 * - Debe conservar el metodo . 
	 * - Es el ÃƒÂºnico metodo permitido para escribir en el log.
	 */
	public static void escribirLog(File fileLog, String pCadena) {

		try {
			FileWriter fw = new FileWriter(fileLog,true);
			fw.append(pCadena + "\n");
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

}

