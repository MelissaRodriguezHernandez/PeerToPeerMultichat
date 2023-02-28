package connections;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import controller.MyP2P;

public class ServerConnector {

	private final int serverPort;
	private final MyP2P myP2P;
	private boolean runStateServerConnection;
	
	public ServerConnector(MyP2P myP2P, int serverPort) {
		this.myP2P = myP2P;
		this.serverPort = serverPort;
		this.runStateServerConnection = true;
		new Thread(this::run).start();
	}

	//Metodos de ejecucion y control

	/**
	 * Método principal de la conexión del servidor.
	 * Ejecuta un ciclo infinito en el que se aceptan conexiones entrantes de peers.
	 * El método utiliza un objeto ServerSocket para escuchar las conexiones entrantes
	 * y acepta cada conexión entrante con el método accept(). Después de aceptar una conexión,
	 * el método agrega la conexión a la lista de conexiones de la clase MyP2P mediante
	 * el método addConnection(Socket). El método se ejecuta en un hilo separado para no
	 * bloquear el hilo principal de la aplicación.
	 */
	private void run() {
		try(ServerSocket serverSocket = new ServerSocket(serverPort)){
			System.out.println("Server: Activating for server port " + serverPort);
			while(runStateServerConnection) {
				if(!serverSocket.isClosed()) {
					try {
						Socket socket = serverSocket.accept();
						System.out.println("Server: Connection established with " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
						myP2P.addConnection(socket);
					} catch(IOException e) {
						System.out.println("Server: outor in server service or connection with client socket");
					}
				}
			}
			System.out.println("ServerConnector: Stopped");
		} catch (IOException e) {
			e.printStackTrace();
			runStateServerConnection = false;
		}
	}

	/**
	 * Detiene la ejecución del hilo principal de la conexión del servidor.
	 */
	public void stopServerConnection() {
		System.out.println("Server: Stopping");
		runStateServerConnection = false;
	}

}
