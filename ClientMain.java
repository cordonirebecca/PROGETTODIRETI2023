import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMain {

	private static int TCPPORT;
	private static String MULTICAST;
	private static int MCASTPORT;
	private static int REGPORT;
	private static int CALLPORT;
	private static final String SERVER_HOST = "localhost";
	static Thread multicastThread ;

	private static InterfaceToNotifyRank ToNotifyRank;
	private static InterfaceToRankClient stub;
	private static InterfaceToRankClient ToRankClient;

	private static ConcurrentHashMap<String, Double> datiRicevutiMulticast = new ConcurrentHashMap<String, Double>();

	private static List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>();

	static String dati;

	public static void main(String[] args) {

		// il server legge il suo file di configurazione.txt
		File file = new File("FileConfigurazioneClient.txt");

		int numeroVolteMax = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String st;
			while ((st = br.readLine()) != null) {

				// faccio lo split di ogni riga e controllo che la prima parte sia uguale alla
				// parola
				// se Ã¨ cosÃ¬ assegno il suo valore corrispondente
				String[] parolaDivisa = st.split("=");
				// ignoro # e righe vuote
				if (!st.isBlank() && !st.startsWith("#")) {

					switch (parolaDivisa[0]) {
					case ("TCPPORT") -> {
						TCPPORT = Integer.parseInt(parolaDivisa[1]);
						// System.out.println("TCPPORT " + TCPPORT);
					}
					case ("MULTICAST") -> {
						MULTICAST = parolaDivisa[1];
						// System.out.println("MULTICAST " + MULTICAST);
					}
					case ("MCASTPORT") -> {
						MCASTPORT = Integer.parseInt(parolaDivisa[1]);
						// System.out.println("MCASTPORT " + MCASTPORT);
					}
					case ("REGPORT") -> {
						REGPORT = Integer.parseInt(parolaDivisa[1]);
						// System.out.println("REGPORT " + REGPORT);
					}
					case ("CALLPORT") -> {
						CALLPORT = Integer.parseInt(parolaDivisa[1]);
						// System.out.println("CALLPORT " + CALLPORT);
					}
					}
				}
			}

		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
		}

		//////////////////////
		// connessione socket
		try (Socket socket = new Socket(SERVER_HOST, TCPPORT);
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

			System.out.println();
			System.out.println(
					"-------------------------------BENVENUTO in WORLDE !------------------------------------");
			System.out.println();
			System.out.println("se desideri registrati, digita 'REGISTRAZIONE :<nome>/<password>");
			System.out.println("se desideri effettuare il login, digita 'LOGIN :<nome>/<password>");
			System.out.println();
			System.out.println("per conoscere tutte le statistiche del tuo account digita 'sendMeStatistics:<nome>'");
			System.out.println();
			System.out.println("per conoscere tutti i dati che avete condiviso digita 'showMeSharing:<nome>'");
			System.out.println();
			System.out.println("per vedere la classifica digita 'showMeRanking:<nome>'");
			System.out.println("per uscire dal gioco digita 'LOGOUT : <nome>'");
			System.out.println();
			System.out.println(
					"-----------------------------------------------------------------------------------------");
			System.out.println();

			riferimentiOggettoRemotoCallback();

			// message contiene il messaggio digitato da tastiera
			String message;
			while ((message = reader.readLine()) != null) {
				// diviso il messaggio in due parti: OPERAZIONE, DATI

				String[] messaggioDiviso = message.split(":");
				String operazione = messaggioDiviso[0]; // operazione

				// Verifico che l'array contenga due elementi
				if (messaggioDiviso.length == 2) {
					dati = messaggioDiviso[1];// dati

					switch (operazione) { // parte per analizzare l'OPERAZIONE

					case ("REGISTRAZIONE") -> {

						// isolo il nome e la psw
						String[] messaggioDiviso2 = messaggioDiviso[1].split("/");
						String nome = messaggioDiviso2[0]; // nome

						// controllo che i dati vengano inseriti correttamente
						if (messaggioDiviso2.length == 2) {
							String psw = messaggioDiviso2[1]; // psw

							try {
								Registry registry = LocateRegistry.getRegistry(REGPORT);
								InterfaceToRegister interfacetoregister = (InterfaceToRegister) registry
										.lookup("InterfaceToRegister");

								// Chiamata al metodo remoto per registrare un utente
								boolean registrationResult = interfacetoregister.registrazioneUtente(nome, psw);

								if (registrationResult == false) { // già registrato
									writer.println("REGISTRAZIONE:" + "stop");
									writer.flush();
								} else { // da registrare
									writer.println("REGISTRAZIONE:" + dati);
									writer.flush();
								}

							} catch (Exception e) {
								e.printStackTrace();
							}
							// riceve la risposta dal server
							String serverResponse = serverReader.readLine();
							System.out.println(
									"-----------------------------------------------------------------------------------------");
							System.out.println("Server response: " + serverResponse);

						} else {
							System.out.println(
									"-----------------------------------------------------------------------------------------");
							System.out.println("Devi inserire 'REGISTRAZIONE: nome/password' ");
						}
					}

					case ("LOGIN") -> {

						// isolo il nome e la psw
						String[] messaggioDiviso2 = messaggioDiviso[1].split("/");
						String nome = messaggioDiviso2[0]; // nome

						if (messaggioDiviso2.length == 2) {
							String psw = messaggioDiviso2[1]; // psw

							writer.println("LOGIN:" + nome);
							writer.flush();

							// Avvia il thread per la comunicazione multicast
							multicastThread = new Thread(
									new ClientMulticast(MCASTPORT, MULTICAST, datiRicevutiMulticast));
							multicastThread.start();

							try {
								// registro l'utente alle callback
								ToNotifyRank.registerListener(stub, nome);

							} catch (RemoteException e) {
								e.printStackTrace();
							}
							// riceve la risposta dal server
							String serverResponse = serverReader.readLine();
							System.out.println(
									"-----------------------------------------------------------------------------------------");
							System.out.println("Server response: " + serverResponse);
						} else {
							System.out.println(
									"-----------------------------------------------------------------------------------------");
							System.out.println("Devi inserire 'LOGIN: nome/password' ");
						}
					}

					case ("LOGOUT") -> {

						writer.println("LOGOUT:" + dati);
						writer.flush();

						multicastThread.interrupt();
						// lo elimino dalle callback
						try {
							ToNotifyRank.unregisterListener(stub, dati);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
						// riceve la risposta dal server
						String serverResponse = serverReader.readLine();
						System.out.println(
								"-----------------------------------------------------------------------------------------");
						System.out.println("Server response: " + serverResponse);
					}

					case ("playWORDLE") -> {
						// invio la parola per la prima volta e controllo
						// che l'utente non abbia già giocato con la secret Word
						numeroVolteMax = 0;
						writer.println("playWORDLE:" + dati);
						writer.flush();
						// riceve la risposta dal server
						String serverResponse = serverReader.readLine();
						System.out.println(
								"-----------------------------------------------------------------------------------------");
						System.out.println("Server response: " + serverResponse);
					}

					case ("sendWORD") -> {

						numeroVolteMax++;

						if (numeroVolteMax == 14) {
							// ha finito i tentativi, rinizia la partita, se possibile
							numeroVolteMax = 0;
						}
						if (numeroVolteMax == 13) {
							System.out.println(
									"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
							System.out.println("Hai l'ultima chance");
							System.out.println(
									"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						}
						// Invia il comando di condivisione al server
						writer.println("sendWORD:" + dati + "," + numeroVolteMax);
						writer.flush();
						// riceve la risposta dal server
						String serverResponse = serverReader.readLine();
						System.out.println(
								"-----------------------------------------------------------------------------------------");
						System.out.println("Server response: " + serverResponse);
					}

					case ("sendMeStatistics") -> {
						// invia il comando e il proprio nome
						writer.println("sendMeStatistics:" + dati);
						writer.flush();
						// riceve la risposta dal server
						String serverResponse = serverReader.readLine();
						System.out.println(
								"-----------------------------------------------------------------------------------------");
						System.out.println("Server response: " + serverResponse);
					}

					case ("share") -> {
						// condivisione dell'esito sul gruppo sociale
						// mando nome
						writer.println("share:" + dati);
						writer.flush();
						// riceve la risposta dal server
						String serverResponse = serverReader.readLine();
						System.out.println(
								"-----------------------------------------------------------------------------------------");
						System.out.println("Server response: " + serverResponse);

					}

					case ("showMeSharing") -> {
						// basta che stampo la mia hashmap che contiene i dati del multicast
						System.out.println(
								"-----------------------------------------------------------------------------------------");
						System.out.println("I dati del multicast sono:");
						System.out.println(datiRicevutiMulticast);
					}

					case ("showMeRanking") -> {
						// stampo la classifica
						System.out.println(
								"-----------------------------------------------------------------------------------------");
						System.out.println("La classifica è:");
						System.out.println(list);
					}

					default -> {
						// Operazione non riconosciuta
						System.out.println(
								"-----------------------------------------------------------------------------------------");
						System.out.println("Operazione non valida: " + operazione);

					}

					}
				} else {
					System.out.println(
							"-----------------------------------------------------------------------------------------");
					System.out.println("Errore: messaggio non valido.");
					System.out.println();
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// metodo per gli oggetti remoti per la callback
	private static void riferimentiOggettoRemotoCallback() {
		try {
			Registry registry = LocateRegistry.getRegistry(CALLPORT);
			ToNotifyRank = (InterfaceToNotifyRank) registry.lookup("InterfaceToNotifyRank");
			ToRankClient = new ToRankClient(list); // classifica

			stub = (InterfaceToRankClient) UnicastRemoteObject.exportObject(ToRankClient, 0);
		} catch (RemoteException e) {
			e.printStackTrace();

		} catch (NotBoundException e) {
			throw new RuntimeException(e);
		}

	}

}