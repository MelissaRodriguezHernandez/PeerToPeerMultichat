package view;


import controller.MyP2P;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;

public class View extends JFrame implements WindowListener {

	private final JTable connections;
	private JTextArea chat;
	private final JTextField inputMessage;
	private JButton sendButton;
	private MyP2P controller;
	private boolean validConnectionsInTable = false;

	private boolean runState;

	public View() {
		setTitle("Chat");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(800, 600));
		addWindowListener(this);
		connections = new JTable(new DefaultTableModel(new String[]{"IP","STATE"}, 0));
		runState = true;
		inputMessage = new JTextField();
		sendButton = new JButton();
		initializeComponents();
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	public void addConnection(String ip) {
		DefaultTableModel model = (DefaultTableModel) connections.getModel();
		Object[] rowData = {ip, ""};
		model.addRow(rowData);
	}

	public void setController(MyP2P controller) {
		this.controller = controller;
		new Thread(this::pullConnection).start();
	}

	private void initializeComponents() {
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		// Area para recibir mensajes
		chat = new JTextArea();
		chat.setLineWrap(true);
		JScrollPane scrollChat = new JScrollPane(chat);
		scrollChat.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		chat.setEditable(false);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.5;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		mainPanel.add(scrollChat, c);

		// Area para mostrar la lista de peers
		initializeAreaPeers();
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0.3;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JScrollPane(connections), c);

		// Area para enviar mensajes
		initializeMessageInputArea();
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		mainPanel.add(inputMessage, c);

		//Area boton
		initializeSendButtonArea();
		c.gridx = 0;
		c.gridy = 2; // Cambia la posición para que esté debajo del campo de mensaje
		c.weightx = 0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		mainPanel.add(sendButton, c);



		add(mainPanel, BorderLayout.CENTER);
	}


	private void initializeMessageInputArea() {
		JPanel inputArea = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;

		inputMessage.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					sendButton.doClick();
				}
			}
		});
		inputArea.add(inputMessage, c);
	}

	private void initializeSendButtonArea() {
		JPanel buttonArea = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;

		sendButton = new JButton("Send");
		sendButton.addActionListener((ev) -> {
			String mensaje = inputMessage.getText();
			inputMessage.setText("");
			inputMessage.requestFocus();
			sendMessage(mensaje);
		});
		buttonArea.add(sendButton, c);
	}


	private void initializeAreaPeers() {
		JPanel area = new JPanel();
		area.setLayout(new BorderLayout());

		JScrollPane scrollPane = new JScrollPane(connections);
		scrollPane.setBorder(BorderFactory.createTitledBorder(""));
		area.add(scrollPane, BorderLayout.CENTER);

		add(area, BorderLayout.EAST);

		connections.getColumnModel().getColumn(0).setPreferredWidth(200);
		connections.getColumnModel().getColumn(1).setPreferredWidth(100);

		// Center text in the columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		connections.setDefaultRenderer(String.class, centerRenderer);

		// Set column headers
		connections.getTableHeader().setReorderingAllowed(false);
		connections.getTableHeader().setResizingAllowed(false);
		connections.getColumnModel().getColumn(0).setHeaderValue("IP");
		connections.getColumnModel().getColumn(1).setHeaderValue("STATE");

		// Set custom cell renderer for "STATE" column
		TableColumn stateColumn = connections.getColumnModel().getColumn(1);
		stateColumn.setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				String state = (String) value;
				if (state.equals("CONNECTED")) {
					setBackground(Color.GREEN);
				} else if (state.equals("DISCONNECTED")) {
					setBackground(Color.RED);
				}

				return this;
			}
		});
	}

	private void sendMessage(String message) {
		chat.append("MESSAGE: " + message + "\n");
		controller.sendMessage("*", message);
	}

	public void pushMessage(String ip, String message) {
		chat.append("FROM ("+ip+") : " + message + "\n");
	}

	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {
		runState = false;
		setVisible(false);
		controller.stopAndQuit();
	}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}

	private void pullConnection() {
		DefaultTableModel tm = (DefaultTableModel) connections.getModel();
		while (runState) {
			boolean validConnections = false;

			for (int i = 0; i < tm.getRowCount(); ++i) {
				String ip = (String) tm.getValueAt(i, 0);
				if (MyP2P.isValidIp(ip)) {
					boolean isConnected = controller.getConnectionStatus(ip);
					validConnections |= isConnected;
					tm.setValueAt(isConnected ? "CONNECTED" : "DISCONNECTED", i, 1);
				} else {
					tm.setValueAt("", i, 1);
				}
			}

			validConnectionsInTable = validConnections;
			sendButton.setEnabled(validConnectionsInTable);
			inputMessage.setEnabled(validConnectionsInTable);

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
