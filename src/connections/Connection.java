package connections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import controller.MyP2P;
import view.*;

/**
 * En general, esta clase es responsable de enviar y recibir marcos en la red P2P.
 * Ofrece métodos para enviar y recibir marcos a través de la conexión,
 * y también proporciona funciones para manejar marcos específicos,
 * como marcos de ping o marcos de mensaje.
 * Además, también proporciona algunos métodos para controlar el
 * estado de la conexión y actualizar el tiempo de la última conexión.
 */

public class Connection {

	private final MyP2P myP2P;
	private final String clientIp;
	private Socket socket;
	private HealthCareConnection hcc;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private long lastTimeReceivedMessage;
	private Thread thread;
	public boolean runState;
	
	public Connection(MyP2P myP2P, String clientIp) {
		this.myP2P = myP2P;
		this.clientIp = clientIp;
		thread = new Thread(this::run);
	}

	//Metodos de inicializacion y control

	/**
	 * Detiene el hilo secundario de la conexión.
	 */
	public void stopConnection() {
		System.err.println("Connection: Stopping");
		killSocket();
	}

	/**
	 * Elimina un socket de la conexión.
	 * Borra el puerto adscrito, pero no cambia la targetIp para que este objeto esté vinculado a ella.
	 */
	public void killSocket() {
		try {
			runState = false;
			if(hcc!=null)
				hcc.stopHCC();
			if(socket != null && !socket.isClosed())
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			in = null;
			out = null;
			socket = null;
			System.err.println("Connection: Matando el socket de " + clientIp);
		}
	}

	/**
	 *  Informa si se tiene un socket abierto.
	 * @return True si tiene socket, False en caso contrario
	 */
	public boolean isOk() {
		return socket!=null && !socket.isClosed();
	}

	/**
	 * Devuelve la ip del cliente del peer conectado.
	 * @return Una cadena con la targetIp del peer, null en caso de no haber conectado
	 */
	public String getClientIp() {
		return clientIp;
	}

	/**
	 * Añade un socket a la conexión
	 * @param socket Socket por el que hará la conexión
	 */
	public void setSocket(Socket socket) {
		if(!isOk() && clientIp.equals(socket.getInetAddress().getHostAddress())) {
			this.socket = socket;
			try {
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());
				updateTimeReceivedMessage();
				runState = true;
				if(!thread.isAlive()) {
					thread = new Thread(this::run);
					thread.start();
				}
				hcc = new HealthCareConnection(this, 1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void run() {
		while(runState) {
			if(isOk()) {
				receive();
			}
		}
	}

	/**
	 * Devuelve el tiempo en milisegundos de la última conexión
	 * @return Un dato tipo long con el tiempo de la última conexión
	 */
	public long getTimeReceivedMessage() {
		return lastTimeReceivedMessage;
	}

	//Metodo de envio de mensajes
	/**
	 * Envía una carga útil a través de la conexión.
	 * @param packageInfo
	 */
	public void send(String packageInfo) {
		if(isOk()) {
			send(socket.getInetAddress().getHostAddress(), packageInfo);
		}
	}

	/**
	 * Envía una carga útil a una ip de cliente de destino a través de la conexión.
	 * @param destinationIp
	 * @param packageInfo
	 */
	public void send(String destinationIp, String packageInfo) {
		if(destinationIp != null && !MyP2P.isValidIp(destinationIp))
			return;
		if(destinationIp == null) {
			destinationIp = "*";
		}
		if(isOk()) {
			Frame frame = new Frame();
			frame.setFrameType(Frame.FrameType.MESSAGE);
			frame.setHeader(2, socket.getLocalAddress().getHostAddress(), destinationIp);
			frame.setPayload(packageInfo);
			sendFrame(frame);
		}
	}

	/**
	 * Envía un marco a través de la conexión.
	 */
	public void sendFrame(Frame frame) {
		if(isOk()) {
			try {
				out.writeObject(frame);
			} catch (IOException e) {
				e.printStackTrace();
				killSocket();
			}
		}
	}

	//Metodo de recepcion de mensajes

	/**
	 * Recibe un marco a través de la conexión.
	 */
	private void receive() {
		if(isOk()) {
			try {
				Frame frame = (Frame)in.readObject();
				updateTimeReceivedMessage();
				handleFrame(frame);
			} catch (Exception e) {
				System.err.println("Connection: Error in the receive");
				killSocket();
			}
		}
	}

	//Metodo de manejo de mensajes

	/**
	 * Maneja el marco recibido.
	 * @param frame
	 */
	private void handleFrame(Frame frame) {
		String myIp = socket.getLocalAddress().getHostAddress();
		System.err.flush();
		switch (frame.getFrameType()) {
			case MESSAGE -> {
				// El paquete es nuestro. Lo matamos
				if (frame.getSourceIP().equals(myIp)) return;
				// El paquete va dirigido a todos o a nosotros. Enviar el payload y la ip de origen al controlador para tratarlo.
				if (frame.getTargetIP().equals(myIp) || frame.getTargetIP().equals("*")) {
					myP2P.pushMessage(frame.getSourceIP(), frame.getPayload());
				}
				// Reenviarlo solo en caso de que el paquete no sea para nostros y su ttl no sea 0
				else if (!frame.getTargetIP().equals(myIp) && !frame.decrementTTL()) {
					myP2P.resend(clientIp, frame);
				}
			}
			case PING -> {
				// Consideramos viene directamente de este peer
				Frame response = new Frame();
				response.setFrameType(Frame.FrameType.PING_ACK);
				response.setHeader(1, myIp, clientIp);
				System.out.println("Connection: Enviando PingAck a " + clientIp);
				sendFrame(response);
			}
			case PING_ACK -> System.out.println("Connection: Recibido PingAck: " + socket.getInetAddress().getHostAddress());
		}
	}


	/**
	 * Lanza un ping al destino de la conexion
	 */
	void doPing() {
		if(isOk()) {
			Frame ping = new Frame();
			ping.setFrameType(Frame.FrameType.PING);
			ping.setHeader(1, socket.getLocalAddress().getHostAddress(), clientIp);
			sendFrame(ping);
		}
	}

	//Metodo de actualización de estado

	/**
	 * Actualiza el tiempo de la última conexión.
	 */
	public void updateTimeReceivedMessage() {
		lastTimeReceivedMessage = System.currentTimeMillis();
	}
}
