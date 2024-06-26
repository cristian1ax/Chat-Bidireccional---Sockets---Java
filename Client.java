/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client implements Runnable {
    
private static Socket clientSocket = null; 
	private static ObjectOutputStream os = null; 
	private static ObjectInputStream is = null; 
	private static BufferedReader inputLine = null; 
	private static BufferedInputStream bis = null; 
	private static boolean closed = false; 

	public static void main(String[] args) {
            
                    
		// Puerto por defecto para conexion con el servidor.
		int portNumber = 80;
		
		// IP/Numero de host por defecto.
		String host = "127.0.0.1";
		
		if (args.length < 2) {
			System.out.println("Servidor por defecto: " + host + ", Puerto por defecto: " + portNumber);
		}
		
		// Inicia un socket en la IP y puerto especificado
        // Inicia los flujos de entrada y salida
		
		try {
			clientSocket = new Socket(host, portNumber);
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			os = new ObjectOutputStream(clientSocket.getOutputStream());
			is = new ObjectInputStream(clientSocket.getInputStream());
			
		} catch (UnknownHostException e) {
			System.err.println("IP/Host desconocido " + host);
		} catch (IOException e) {
			System.err.println("No se encontró el servidor. Asegurese de ejecutar primero el servidor y despues los clientes.");
		}
		
		/*
		 * Una vez que la conexion se ha inicializado y los flujos se han abierto, iniciamos la transmision
		 * de datos hacia el socket en el cual abrimos una conexion.
		 */
		if (clientSocket != null && os != null && is != null) {
			try {
				/* Se crea un hilo para transmitir al servidor. */
				new Thread(new Client()).start();
				while (!closed) {

					/* Lee lo que el cliente escribió */
					String msg = (String) inputLine.readLine().trim();

					/* Verifica si es un mensaje privado */
					if ((msg.split(":").length > 1))
					{
                     os.writeObject(msg);
                     os.flush();
					}
					/* Verifica si es un mensaje para todos los usuarios */
					else 
					{
                     os.writeObject(msg);
                     os.flush();
					}
				}
				/*
				 * cierra todos los hilos y el socket.
				 */
				os.close();
				is.close();
				clientSocket.close();
			} catch (IOException e) 
			{
				System.err.println("IOException:  " + e);
			}
		}
	}
	
	/* Crea un hilo (o canal) para comunicarse con el servidor. */
	
	public void run() {
		
		/* Bucle que lee el socket constantemente hasta recibir una desconexion del servidor.
		 * Una vez recibida, se finaliza la conexion.
		 */
		
		String responseLine;
		try {
			while ((responseLine = (String) is.readObject()) != null)  {
				/* Condicion para verificar mensajes entrantes */
                {
                  System.out.println(responseLine);
				}
				/* Condicion para cerrar la aplicacion */
				if (responseLine.indexOf("*** #chao") != -1)
				break;
			}
			closed = true;
			System.exit(0);
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Se ha cerrado la conexion con el servidor");
		}
	}
}
