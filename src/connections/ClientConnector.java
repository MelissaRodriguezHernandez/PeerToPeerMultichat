package connections;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

import controller.MyP2P;


public class ClientConnector {

	private final int serverPort;
	private final MyP2P myP2P;
	private boolean runStateClientConnection;
	private static final Logger LOGGER = Logger.getLogger(ClientConnector.class.getName());


	//Constructor
	public ClientConnector(MyP2P myP2P, int serverPort) {
		this.myP2P = myP2P;
		this.serverPort = serverPort;
		runStateClientConnection = true;
		new Thread(this::run).start();
	}


	//Metodos de ejecucion y control

	/**
	 * Método principal de la conexión del cliente.
	 * Ejecuta un ciclo infinito en el que se intenta reconectar a los peers conocidos
	 * que están desconectados.
	 * El método utiliza el método getPeersList() de la clase MyP2P para obtener
	 * una lista de peers que están desconectados, intenta conectarse a cada peer de la lista
	 * y agrega la conexión a la lista de conexiones de la clase MyP2P si se establece la conexión.
	 * El método se ejecuta en un hilo separado para no bloquear el hilo principal de la aplicación.
	 */
	private void run() {
		LOGGER.info("Client: Activated successfully");
		while(runStateClientConnection) {
			// Recibe la lista de los antiguos peers que se han desconectado
			List<Connection> connectionList = myP2P.getPeersList().stream().filter((connection) -> !connection.isOk()).toList();
			// Intenta conectar a cada peer de la lista
			for(Connection connection: connectionList) {
				try {
					LOGGER.warning("Client: Try of reconnection " + connection.getClientIp());
					Socket socket = new Socket(connection.getClientIp(), serverPort);
					LOGGER.info("Client: Success in reconnecting with" + connection.getClientIp());
					myP2P.addConnection(socket);
				} catch (IOException e) {
					LOGGER.severe("Client: Fail to connect with " + connection.getClientIp());
				}
			}

			// Espera para la siguente conexion
			try {
				Thread.sleep(500);
			} catch (InterruptedException ignored) {}
		}
		LOGGER.severe("Client: Client stopped");
	}

	/**
	 * Detiene la ejecución del hilo principal de la conexión del cliente.
	 */
	public void stopClientConnection() {
		LOGGER.warning("Stopping " + this.getClass().getSimpleName());
		runStateClientConnection = false;
	}
	


}
