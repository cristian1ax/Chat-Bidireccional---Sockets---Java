/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.net.Socket;
import java.net.ServerSocket;

public class Server {
    
// Socket para el servidor.
	private static ServerSocket serverSocket = null;
	
	// Socket para el cliente.
	private static Socket clientSocket = null;
	
	public static ArrayList<clientThread> clients = new ArrayList<clientThread>();
	
	public static void main(String args[]) {
            
		// Puerto por defecto y/o asignado por el usuario
		int portNumber = 80;
		
		if (args.length < 1) {
			System.out.println("El servidor usara el puerto asignado por defecto: " + portNumber);
		} 
		
		System.out.println("Esperando conexion entrante en el puerto " + portNumber + "...");

		/*
		 * Abre un socket de servidor en el puerto asignado
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println("No se puede crear el socket de servidor");
		}
		/*
		 * Por cada conexion, se crea un socket de cliente y se maneja como un nuevo hilo
         * Hilo: Linea por la cual se establece la conexion
		 */
		
		int clientNum = 1; //variable que emnumera cada cliente conectado
		
		while (true) {
			try {
				clientSocket = serverSocket.accept();
				clientThread curr_client =  new clientThread(clientSocket, clients);
				clients.add(curr_client);
				curr_client.start();
				
				System.out.println("El Poli "  + clientNum + " se ha conectado!");
				clientNum++;

			} catch (IOException e) {
				System.out.println("No se puede establecer conexion con el cliente");
			}
		}
	}
}

/* Esta clase, gestiona clientes individualmente en sus respectivos hilos 
 * Abre flujos separados de entrada y salida. 
 */

class clientThread extends Thread {

	private String clientName = null; 
	private ObjectInputStream is = null; 
	private ObjectOutputStream os = null; 
	private Socket clientSocket = null; 
	private final ArrayList<clientThread> clients;
	
	public clientThread(Socket clientSocket, ArrayList<clientThread> clients) {
		this.clientSocket = clientSocket;
		this.clients = clients;
	}
	
	public void run() {
		
		ArrayList<clientThread> clients = this.clients;
		
		try {
			// Creacion de los flujos de entrada y salida de informacion del cliente.
			is = new ObjectInputStream(clientSocket.getInputStream());
			os = new ObjectOutputStream(clientSocket.getOutputStream());

			String name; //variable que alamcena el nombre del cliente
            int u;
            
			while (true) {
				synchronized(this){
					
                    //Una vez esta iniciado el proceso de conexion, el servidor solicita el nombre al cliente.
					this.os.writeObject("Por favor, ingrese su nombre :");
					this.os.flush();
					name = ((String) this.is.readObject()).trim();

					if ((name.indexOf('@') == -1) || (name.indexOf('!') == -1)) {
						break;
					} else {
						this.os.writeObject("el nombre no debe tener los siguientes caracteres: '@' o '!' ");
						this.os.flush();
					}
				}
			}
			
			//Bienvenida al nuevo cliente
			
			System.out.println("El nombre del usuario poli es: " + name); //Imprime en consola el nombre del usuario

			this.os.writeObject("Bienvenido/a " + name + " al chat. Para desconectarse ingrese #chao\n"); //Da la bienvenidad al cliente conectado
			this.os.flush();

			this.os.writeObject("Se ha conectado correctamente\n");
			this.os.flush();
			
			synchronized(this){
				
				for (clientThread curr_client : clients) {
					if (curr_client != null && curr_client == this) {
						clientName = "@" + name;
						break;
					}
				}
				
				for (clientThread curr_client : clients) {
					if (curr_client != null && curr_client != this) {
						curr_client.os.writeObject(name + " se ha unido");
						curr_client.os.flush();
					}
				}
			}
			
		/* Inicio de la conversacion. */
		while (true) {

			this.os.writeObject("Escriba su mensaje:");
			this.os.flush();

			String line = (String) is.readObject();
            
            //Permite que el cliente al ingresar #chao, se desconecte del servidor
			if (line.startsWith("#chao")) {
				break;
			}
			
			// Si el mensaje es privado, se envia al cliente especificado @ + nombre del cliente + : + mensaje
			if (line.startsWith("@")) {  // Ejemplo: @Cristian: mensaje
				unicast(line,name);        	
			}
			else // Si es un mensaje para todos, no se especifica el nombre, solo se ingresa el mensaje que se desea enviar.
			{
				broadcast(line,name);
			}
		}
		
		/* Termina la sesion para el usuario especificado */
		this.os.writeObject("Hasta pronto " + name + " \n");
		this.os.flush();
		System.out.println(name + " desconectado.");
		clients.remove(this);

		synchronized(this) {
			if (!clients.isEmpty()) {
				for (clientThread curr_client : clients) {
					if (curr_client != null && curr_client != this && curr_client.clientName != null) {
						curr_client.os.writeObject("El usuario " + name + " se ha desconectado ");
						curr_client.os.flush();
					}
				}
			}
		}
		this.is.close();
		this.os.close();
		clientSocket.close();

		} catch (IOException e) {
			System.out.println("\nHa terminado la sesion del usuario");

		} catch (ClassNotFoundException e) {
			System.out.println("Class Not Found");
		}
	}

    /* Esta funcion transmite mensajes a todos los usuarios conectados al servidor */
	void broadcast(String line, String name) throws IOException, ClassNotFoundException {
		
		/* Enviar mensaje a todos los usuarios (Broadcast)*/
		synchronized(this){
			for (clientThread curr_client : clients) {
				if (curr_client != null && curr_client.clientName != null && curr_client.clientName!=this.clientName) 
				{
					curr_client.os.writeObject("<" + name + "> " + line);
					curr_client.os.flush();
				}
			}
            
		//registro de envio a todos los usuarios en la consola. Se captura el nombre de quien envió el mensaje a todos
		this.os.writeObject("Mensaje enviado a todos los usuarios.");
		this.os.flush();
		System.out.println("Un mensaje fue enviado a todos los usuarios por " + this.clientName.substring(1));
		
		}
	}
	
	/* Esta funcion transmite mensajes a un usuario en epecifico, establecido por el cliente (unicast) */	
	void unicast(String line, String name) throws IOException, ClassNotFoundException {
		
		String[] words = line.split(":", 2); 
		/* Enviando mensaje a un unico cliente*/
		if (words.length > 1 && words[1] != null) {
			words[1] = words[1].trim();
			
			if (!words[1].isEmpty()) {
				for (clientThread curr_client : clients) {
					if (curr_client != null && curr_client != this && curr_client.clientName != null && curr_client.clientName.equals(words[0])) {
						curr_client.os.writeObject("<" + name + "> " + words[1]);
						curr_client.os.flush();

						System.out.println(this.clientName.substring(1) + " envio mensaje a "+ curr_client.clientName.substring(1));

						/* Mensaje que le confirma al cliente remitente que el mensaje se envió al cliente destinatario*/
						this.os.writeObject("Mensaje enviado a: " + curr_client.clientName.substring(1));
						this.os.flush();
						break;
					}
				}
			}
		}
	}
}
