package main;

import controller.MyP2P;
import view.View;

import java.util.ArrayList;

public class Main {

	public static void main(String[] args) {
		//Lista de ips
		ArrayList<String> ipList = new ArrayList<>();
		ipList.add("192.168.0.25");

		//Inicializacion de programa
		MyP2P myP2P = new MyP2P(ipList);
		View view = new View();
		view.setController(myP2P);
		myP2P.setView(view);
	}
}
