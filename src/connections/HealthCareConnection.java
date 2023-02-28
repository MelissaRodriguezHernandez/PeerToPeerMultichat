package connections;

/**
 * Esta clase, llamada HealthCareConnection,
 * es una clase que se encarga de monitorear el estado de una conexión y
 * realizar acciones en caso de que se detecte un problema.
 *
 */
public class HealthCareConnection {
	
	private final Connection connection;
	private ConnectionStatus status;
	private final long healthTimeOut;
	public boolean runStateHCC;
	
	public HealthCareConnection(Connection connection, long healthTimeOut) {
		this.connection = connection;
		this.healthTimeOut = healthTimeOut;
		status = ConnectionStatus.OK;
		runStateHCC = true;
		new Thread(this::run).start();
	}

	/**
	 * Es un método público que detiene la ejecución del hilo que se encarga del monitoreo.
	 * Este método simplemente cambia el valor del atributo runState a false.
	 */
	public void stopHCC() {
		System.out.println("HCC: Stopping");
		runStateHCC = false;
	}

	/**
	 * Es el método privado que se encarga de realizar el monitoreo de la conexión.
	 * Este método se ejecuta en un hilo independiente y se encarga de verificar el
	 * estado de la conexión en un ciclo infinito. Si la conexión está funcionando correctamente,
	 * se espera un tiempo y se vuelve a verificar. Si la conexión no responde después de cierto tiempo,
	 * se cambia el estado de la conexión a AWAITING y se intenta enviar un ping.
	 * Si la conexión sigue sin responder después de cierto tiempo,
	 * se cierra el socket de la conexión y se cambia el estado de la conexión a OK.
	 */
	private void run() {
		System.err.println("HealthCareConnection: activado");
		while(runStateHCC) {
			if(connection.isOk()) {
				try {
					Thread.sleep(healthTimeOut/3);
				} catch (InterruptedException e) { }
				long lastTimeConnection = connection.getTimeReceivedMessage();
				long currentTime = System.currentTimeMillis();
				long diffTime = currentTime - lastTimeConnection;
				if(diffTime > healthTimeOut) {
					if(status == ConnectionStatus.OK) {
						status = ConnectionStatus.AWAITING;

						System.out.println("HealthCareConnection: <OK> timeout = " + diffTime + " (max. " + healthTimeOut + "ms)");
						System.out.println("HealthCareConnection: <OK> sending ping");

						connection.doPing();
					} else {  // status == ConnectionStatus.AWAITING_OK
						System.out.println("HealthCareConnection: <AWAITING_OK> timeout = " + diffTime + " (max. " + healthTimeOut + "ms)");

						connection.killSocket();
						status = ConnectionStatus.OK;
					}
				} else {
					status = ConnectionStatus.OK;
				}
			}
		}
		System.err.println("HeHealthCareConnection: detenido.");
	}

}
