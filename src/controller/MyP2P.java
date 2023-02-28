package controller;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import connections.ClientConnector;
import connections.Connection;
import connections.ServerConnector;
import view.Frame;
import view.View;

/**
 * La clase MyP2P es un controlador de red para una aplicación de comunicación punto a punto.
 * La clase se encarga de manejar todas las conexiones de red, enviar y recibir mensajes,
 * y mantener una lista de conexiones a los distintos pares de la red.
 */
public class MyP2P {

	private ArrayList<Connection> connectionList;
	private ServerConnector serverConnector;
	private ClientConnector clientConnector;
	private View view;


	//Constructor
	public MyP2P(ArrayList<String> ipList) {
		connectionList = new ArrayList<>();

		try {
			// Afegir ip de peers a la llista
			int serverPort = 1234;
			for (int i = 0; i < ipList.size(); i++) {
				String ip = ipList.get(i);
				if (ip != null && isValidIp(ip)) {
					addEmptyConnection(ip);
				}
			}

			serverConnector = new ServerConnector(this, serverPort);
			clientConnector = new ClientConnector(this, serverPort);

		}catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	//Metodos de gestion de vista
	
	/**
	 * Este método se utiliza para agregar una vista a la aplicación.
	 * La vista es un objeto MainFrame que representa la interfaz gráfica de la aplicación.
	 * El método agrega la vista a la propiedad view de la clase y
	 * agrega todas las conexiones conocidas a la vista.
	 * @param view Vista a añadir
	 */
	public void setView(View view) {
		this.view = view;
		for(Connection c: connectionList) {
			view.addConnection(c.getClientIp());
		}
	}

	/**
	 * Este método se utiliza para mostrar un mensaje recibido en la vista de la aplicación.
	 * El método recibe la dirección IP del remitente y el mensaje,
	 * y agrega el mensaje a la vista correspondiente.
	 * @param message Mensaje recibido a través del socket.
	 */
	public void pushMessage(String ip, String message) {
		if(view != null) {
			view.pushMessage(ip, message);
		}
	}

	//Metodos de Gestion de Conexiones

	/**
	 * Este método se utiliza para crear una nueva conexión de red para la dirección IP dada.
	 * Si la conexión ya existe, se devuelve su índice en la lista de conexiones.
	 * De lo contrario, se crea una nueva conexión y se agrega a la lista de conexiones.
	 * @param ip IP de la conexión que se desea establecer.
	 * @return el índice en la lista de conexiones de la conexión con la ip dada.
	 */
	private int addEmptyConnection(String ip) {
		int index = getConnectionIndex(ip);
		if(index == -1) {
			Connection connection = new Connection(this, ip);
			connectionList.add(connection);
			index = connectionList.size() - 1;
			if(view != null) {
				view.addConnection(ip);
			}
		}
		return index;
	}

	/**
	 * Este método se utiliza para agregar una nueva conexión a la lista de conexiones.
	 * El método recibe un objeto Socket que representa la conexión y
	 * determina la dirección IP del otro extremo de la conexión.
	 * Si la dirección IP es conocida, se establece el socket en la conexión correspondiente
	 * en la lista de conexiones.
	 * @param socket Socket establecido con la nueva conexión.
	 */
	public void addConnection(Socket socket) {
		int index = addEmptyConnection(socket.getInetAddress().getHostAddress());
		connectionList.get(index).setSocket(socket);
	}

	/**
	 * Este método se utiliza para obtener una lista de todas las conexiones de red conocidas.
	 * @return Retorna una lista de peers.
	 */
	public List<Connection> getPeersList() {
		return connectionList;
	}

	/**
	 *  Este método se utiliza para obtener el índice de la conexión de red correspondiente a la dirección IP dada.
	 *  Si no se encuentra ninguna conexión, el método devuelve -1.
	 * @param ip .
	 * @return index.
	 */
	private int getConnectionIndex(String ip) {
		int index = -1;
		for(int i=0; i<connectionList.size(); ++i) {
			Connection connection = connectionList.get(i);
			if(connection.getClientIp() != null && connection.getClientIp().equals(ip)) {
				index = i;
				break;
			}
		}
		return index;
	}

	/**
	 * Este método se utiliza para comprobar el estado de la conexión de red
	 * correspondiente a la dirección IP dada.
	 * Si la conexión es conocida, el método devuelve su estado actual con le metodo isOk().
	 * @param ip IP de la conexión que se quiere comprobar.
	 */
	public boolean getConnectionStatus(String ip) {
		boolean status = false;
		int index = getConnectionIndex(ip);
		if(index >= 0) {
			status = connectionList.get(index).isOk();
		}
		return status;
	}

	//Metodos de envio y reenvio de mensajes

	/**
	 * Este método se utiliza para reenviar un paquete a la dirección IP de destino correspondiente.
	 * Si la dirección IP es de un par conocido, se envía el paquete directamente a ese par.
	 * De lo contrario, el método hace un flood enviando el paquete a todas
	 * las direcciones IP conocidas, excepto a la dirección IP del par que envió el paquete.
	 * @param bannedIp Ip a la que no se debe de retransmitir
	 *                 Para multichat aun no sirve
	 */
	public void resend(String bannedIp, Frame frame) {
		String destinatioIp = frame.getPayload();
		int index = getConnectionIndex(destinatioIp);
		if(index != -1) {
			connectionList.get(index).sendFrame(frame);
		} else {
			for(Connection connection: connectionList) {
				if(!connection.getClientIp().equals(bannedIp)) {
					connection.sendFrame(frame);
				}
			}
		}
	}

	/**
	 * Este método se utiliza para enviar un mensaje a la dirección IP de destino correspondiente.
	 * Si la dirección IP es conocida, el mensaje se envía directamente a ese par.
	 * De lo contrario, el mensaje se envía por broadcast a todas las direcciones IP conocidas.
	 * @param ip IP de la conexión a utilizar para mandar un mensaje.
	 * @param message Mensaje que se desea enviar.
	 */
	public void sendMessage(String ip, String message) {
		if(ip == null) {
			for(Connection conn: connectionList) {
				conn.send(message);
			}
		} else {
			int index = getConnectionIndex(ip);
			if(index != -1) {
				connectionList.get(index).send(message);
			} else {
				for(Connection conn: connectionList) {
					conn.send(message);
				}
			}
		}
	}

	//Metodos de parada y salida

	/**
	 *  Este método se utiliza para detener el servidor, el servicio de reconexión y
	 *  todas las conexiones establecidas.
	 *  Finalmente, el método detiene la ejecución del programa.
	 */
	public void stopAndQuit() {
		// Detiene el servidor
		serverConnector.stopServerConnection();
		// Detiene el reconectar
		clientConnector.stopClientConnection();
		for(Connection connection: connectionList) {
			connection.stopConnection();
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	//Otros metodos

	/**
	 * Establece si una IP dada por una cadena es válida.
	 * @param ip String con una ip.
	 */
	public static boolean isValidIp(String ip) {
		String IPV4_PATTERN =
				"^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$";
		Pattern pattern = Pattern.compile(IPV4_PATTERN);
		return pattern.matcher(ip).matches();
	}
	
}
