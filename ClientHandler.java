import static java.lang.Thread.sleep;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

//legge i dati inviati dal client tramite l'input stream e invia le risposte utilizzando l'output stream.

public class ClientHandler implements Runnable {
	private final Socket clientSocket;
	private static ConcurrentLinkedQueue<String> utentiOnline;
	private final ConcurrentHashMap<String, Utente> UtenteMap;
	String secretWord;
	static int MCASTPORT;
	static String MULTICAST;

	public ClientHandler(Socket clientSocket, ConcurrentLinkedQueue<String> utentiOnline,
						 ConcurrentHashMap<String, Utente> UtenteMap, int MCASTPORT, String MULTICAST) {
		this.clientSocket = clientSocket;
		ClientHandler.utentiOnline = utentiOnline;
		this.UtenteMap = UtenteMap;
		ClientHandler.MCASTPORT = MCASTPORT;
		ClientHandler.MULTICAST = MULTICAST;
	}

	@Override
	public void run() {
		try (InputStream inputStream = clientSocket.getInputStream();
			 OutputStream outputStream = clientSocket.getOutputStream();
			 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			 PrintWriter writer = new PrintWriter(outputStream)) {

			String clientMessage;
			String parolaTradotta;
			Utente utente;

			while ((clientMessage = reader.readLine()) != null) {
				System.out.println("Received message from client: " + clientMessage);

				// Process client message
				String response = "Server response: " + clientMessage;

				if (clientMessage.contains("REGISTRAZIONE")) {
					int startIndex = clientMessage.indexOf(":") + 1; // Calcola l'indice di inizio della parte
					// desiderata

					String desiredPart = clientMessage.substring(startIndex);

					if (desiredPart.equals("stop")) { //già registrato e psw corretta
						response = "utente già registrato,digita 'LOGIN:<nome>/<password>'";
					} else if (desiredPart.equals("stop2")) { //già registrato ma psw sbagliata
						response = "password errata perfavore ritenta 'REGISTRAZIONE:<nome>/<password>'";
					} else{
						response = "utente registrato con successo,digita 'LOGIN:<nome>/<password>'";
					}
				}

				if (clientMessage.contains("LOGIN")) {

					int startIndex = clientMessage.indexOf(":") + 1; // Calcola l'indice di inizio della parte
					// desiderata

					String desiredPart = clientMessage.substring(startIndex);

					//divido nome e psw
					String[] nomePswArray = desiredPart.split(" ");
					String nome = nomePswArray[0]; // nome
					String psw = nomePswArray[1];  // psw

					// Se l'utente non è registrato non può fare login
					if (UtenteMap.containsKey(nome)) { // controllo se l'utente è nella hashmap
						utente = UtenteMap.get(nome);

						// Controllo se la password sia giusta
						if (utente.getPassword().equals(psw)) {
							//caso in cui è ok
							response = "utente registrato, abilitato al login" + "/" + MULTICAST;

							// controllo che non ci sia un altro utente già online con stesso nome
							if (utentiOnline.contains(nome)) {
								response = "utente già collegato! Iniziamo a giocare! digita 'playWORDLE:<nome>'" + "/" + MULTICAST;

							} else {
								utentiOnline.add(nome);
								ServerMain.salvataggioUtentiOnline();
								response = "utente collegato con successo! Iniziamo a giocare! digita 'playWORDLE:<nome>'" + "/" + MULTICAST;
							}
						}
						else{ //psw sbagliata
							response = "password errata, ritenta 'LOGIN:<nome>/<password>'" ;
						}
					} else { // deve fare la registrazione
						response = "utente non registrato, perfavore effettua REGISTRAZIONE:<nome>/<password>";
					}

				}

				if (clientMessage.contains("LOGOUT")) {
					int startIndex = clientMessage.indexOf(":") + 1; // Calcola l'indice di inizio della parte
					// desiderata

					String desiredPart = clientMessage.substring(startIndex);

					// System.out.println(utentiOnline.contains(desiredPart));
					// lo elimino dalla lista degli attivi
					if (utentiOnline.contains(desiredPart)) {
						utentiOnline.remove(desiredPart);
						response = "Utente rimosso correttamente: " + desiredPart;
					} else {
						response = "L'utente risulta non collegato";
					}
				}

				if (clientMessage.contains("playWORDLE")) {

					int startIndex = clientMessage.indexOf(":") + 1;

					// riceve il nome del giocatore
					String username = clientMessage.substring(startIndex);

					if (UtenteMap.containsKey(username)) {
						utente = UtenteMap.get(username);

						// gli passo l'ultima parola estratta
						secretWord = ServerMain.getParolaDatabaseUltima();

						// Estrai l'ultima parola indovinata dall'utente (se presente)
						String ultimaParolaGiocata = utente.getUltimoParoleGiocate();

						if (ultimaParolaGiocata == null) { // prima volta
							response = "digita 'sendWORD :guessed Word' per iniziare a giocare! ";
							utente.addParoleGiocate(secretWord);
							System.out.println(utente.getUltimoParoleGiocate());

						} else if (!utente.getUltimoParoleGiocate().equals(secretWord)) {// casi successivi
							utente.SetWordGuessed(false);
							response = "digita 'sendWORD :guessedWord' per iniziare a giocare! ";
							utente.addParoleGiocate(secretWord);
							System.out.println(utente.getUltimoParoleGiocate());

						} else { // non è ancora uscita la nuova parola
							response = "Hai già partecipato al gioco con la parola: " + ultimaParolaGiocata
									+ ". Attenti la nuova parola !";
						}
					} else {
						response = "Utente non trovato. Effettua prima la REGISTRAZIONE!";
					}
				}

				if (clientMessage.contains("sendWORD")) {
					int attempts = 12;

					// Estrai la parola indovinata da clientMessage
					int startIndex = clientMessage.indexOf(":") + 1;
					String guessedWord = clientMessage.substring(startIndex);

					// prendo la parola da indovinare e il numero del tenativo
					String[] messaggioDiviso = guessedWord.split(",");
					String parolaInviataEnome = messaggioDiviso[0]; // parola inviata e nome
					String numeroTentativo = messaggioDiviso[1];// numero

					// faccio lo split tra la parola e il nome
					String[] messaggioDiviso2 = parolaInviataEnome.split("/");
					String parolaInviata = messaggioDiviso2[0];
					String nome = messaggioDiviso2[1]; // nome utente che invia
					int tentativo = Integer.parseInt(numeroTentativo);
					utente = UtenteMap.get(nome);

					// controllo che la parola inviata sia di 10 caratteri, altrimenti reinserisco
					if (parolaInviata.length() == 10) {

						// se le parole giocate e le parole del server sono uguali allora si gioca
						if (utente.getWordGuessed()) {
							response = "Hai già giocato, digita 'playWORDLE:<nome>' per sapere se è già possibile rigiocare";

						} else {
							while (!utente.getWordGuessed() && attempts > 0 && tentativo < 13) {

								if (parolaInviata.equals(secretWord)) { // ha vinto

									utente.SetWordGuessed(true);

									// aumento il numero di partite vinte
									utente.incrementaPartiteVinte();

									// aumento il numero di partite giocate
									utente.incrementaPartiteGiocate();

									// aumento il numero di tentativi per la partitavinta
									tentativo = utente.incrementoTentativoUtente(tentativo);
									utente.getMapTentativiUtente().put(secretWord, tentativo);

									// associo il numero dei tentativi totali per raggiungere la vittoria
									// per calcolare la guess distribution per ogni partita vinta
									// un vettore con tutti i tentativi di tutte le partite vinte
									utente.incrementoTentativiPartiteVinte(tentativo);

									// questa è la lista di parole indovinate a cui aggiungo la parole in caso di
									// successo
									ConcurrentLinkedQueue<String> paroleIndovinate = utente.getParoleIndovinate();
									paroleIndovinate.add(secretWord);
									parolaTradotta = ServerMain.translateWord(secretWord);
									response = " - Hai indovinato! La parola era: " + secretWord + " .Traduzione: "
											+ parolaTradotta
											+ " .Digita 'playWORDLE:<nome>' per sapere se è già possibile rigiocare";


									ServerMain.SetTabellaClassifica(nome, CalcoloPunteggio(nome));
									break;

								} else {
									response = " - " + compareWords(secretWord, parolaInviata) + " ,tentantivo # "
											+ tentativo;
								}

								response = response;
								attempts--;
							}

							if (!utente.getWordGuessed() && tentativo == 13) { // ha perso
								// aumento il numero di partite giocate
								utente.incrementaPartiteGiocate();
								utente.SetWordGuessed(true);
								utente.getMapTentativiUtente().put(secretWord, tentativo);
								parolaTradotta = ServerMain.translateWord(secretWord);
								response = "Hai esaurito i tentativi. La parola era: " + secretWord + " .Traduzione: "
										+ parolaTradotta
										+ " .Digita 'playWORDLE:<nome>' per sapere se è già possibile rigiocare";

								ServerMain.SetTabellaClassifica(nome, CalcoloPunteggio(nome));
							}
						}

					} else {
						// caso in cui non ha inserito 10 caratteri, perde un tentativo !
						response = " hai inserito un numero di caratteri errato, per favore ritenta! devono essere 10! Tentantivo # " +
								+ tentativo;
					}

				}

				if (clientMessage.contains("sendMeStatistics")) {

					// Estrai la parola indovinata da clientMessage
					int startIndex = clientMessage.indexOf(":") + 1;
					String nome = clientMessage.substring(startIndex);

					CalcoloPunteggio(nome);

					// il server invia:
					// lunghezza dell'ultima sequenza continua di vincite int
					// lunghezzaUltimaSequenzaVittorie
					// lunghezza della massima sequenza continua di vincite int
					// lunghezzaMassimaSequenzaVittorie
					// distribuzione dei tentativi impiegati per arrivare alla soluzione int[]
					// tentativiImpiegati

					int partiteGiocate = 0;// numero partite giocate
					int partiteVinte = 0;
					int massimaSequenzaContinuaDiVittorie = 0;
					int ultimaSequenzaContinuaDiVittorie = 0;
					double guessDistribution = 0;

					// Ottieni le statistiche per l'utente specifico
					utente = UtenteMap.get(nome);

					// numero dell'ultima vittoria consecutiva
					ultimaSequenzaContinuaDiVittorie = calcoloVittorieConsecutive(utente.getParoleGiocate(),
							utente.getParoleIndovinate());

					// numero massimo di vittorie vinte consecutive
					massimaSequenzaContinuaDiVittorie = calcoloMAXVittorieConsecutive(utente.getParoleGiocate(),
							utente.getParoleIndovinate());

					if (utente != null) {
						partiteGiocate = utente.getPartiteGiocate();
						partiteVinte = utente.getPartiteVinte();
					}

					// percentuale partite vinte
					double percentualePartiteVinte = (partiteVinte * 100.0) / partiteGiocate;

					// guess distribution
					if (partiteVinte != 0) { // controllo che abbia vinto almeno una partita sennò mi viene una
						// divisione per 0
						int numeroTentativiUtente = utente
								.sommaTentativiPartiteVinte(utente.getTentativiPartiteVinte());
                        guessDistribution = numeroTentativiUtente / partiteVinte;
					} else {
						guessDistribution = 0;
					}

					// Costruisci la stringa di risposta con le statistiche
					response = "Numero partite giocate: " + partiteGiocate + " Percentuale partite vinte: "
							+ percentualePartiteVinte + " Ultima sequenza di vittorie consecutive: "
							+ ultimaSequenzaContinuaDiVittorie + " Massima sequenza continua di vittorie: "
							+ massimaSequenzaContinuaDiVittorie + " Guess Distribution: " + guessDistribution;

				}

				if (clientMessage.contains("share")) {
					// Estrai il nome dell'utente che desidera condividere il suo punteggio
					int startIndex = clientMessage.indexOf(":") + 1;
					String nome = clientMessage.substring(startIndex);
					double punteggio = 0;

					// ricava il suo punteggio se ha giocato almeno una volta
					if ((punteggio = CalcoloPunteggio(nome)) != -1) {
						// lo invio al gruppo multicast insieme al nome
						sendDataToServer(punteggio, nome);
						response = "Punteggio inviato al gruppo multicast";
					} else {
						response = "Non hai ancora mai giocato ! Devi prima giocare per poter condividere il tuo punteggio";
					}

				}

				// invio risposta al client
				writer.println(response);
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	//funzione per ritornare le parole inviate con i simboli corrispondenti
	private String compareWords(String secretWord, String guessWord) {
		StringBuilder response = new StringBuilder();

		for (int i = 0; i < secretWord.length(); i++) {
			char secretChar = secretWord.charAt(i);
			char guessChar = guessWord.charAt(i);

			if (secretChar == guessChar) { // se è posizione e lettera giusta
				response.append("+");
			} else if (secretWord.indexOf(guessChar) != -1) { // posizione sbagliata ma lettera c'è
				response.append("?");
			} else {
				response.append("X"); // lettera non c'è nella parola
			}
		}

		return response.toString();
	}

	// funzione che calcola il numero di vittorie MAX consecutive
	// confronta le due liste e se manca una parola, azzera il contatore
	public static int calcoloMAXVittorieConsecutive(ConcurrentLinkedQueue<String> partiteGiocate,
													ConcurrentLinkedQueue<String> partiteVinte) {
		int maxVittorieConsecutive = 0;
		int vittorieConsecutiveCorrenti = 0;

		for (String parola : partiteGiocate) {
			if (partiteVinte.contains(parola)) {
				vittorieConsecutiveCorrenti++;
				maxVittorieConsecutive = Math.max(maxVittorieConsecutive, vittorieConsecutiveCorrenti);
			} else {
				vittorieConsecutiveCorrenti = 0;
			}
		}

		return maxVittorieConsecutive;
	}

	// calcola solo il numero dell'ultima seuquenza di vittorie
	public static int calcoloVittorieConsecutive(ConcurrentLinkedQueue<String> partiteGiocate,
												 ConcurrentLinkedQueue<String> partiteVinte) {
		int vittorieConsecutiveCorrenti = 0;

		for (String parola : partiteGiocate) {
			if (partiteVinte.contains(parola)) {
				vittorieConsecutiveCorrenti++;
			} else {
				vittorieConsecutiveCorrenti = 0;
			}
		}

		return vittorieConsecutiveCorrenti;
	}

	//funzione che calcola il punteggio dell'utente
	public double CalcoloPunteggio(String nome) {
		// numero partite vinte * numero medio di tentativi impiegati per raggiungere la
		// soluzione
		Utente utente = UtenteMap.get(nome);
		double punteggio = 0;
		int somma = 0;
		int numeroParoleNonGiocate = 0;
		int partiteVinte = utente.getPartiteVinte();
		// numero medio di tentativi:
		// numero di tutti i tentativi di tutte le partite giocate /numero delle partite giocate
		int partiteGiocate = utente.getPartiteGiocate();

		// calcolo tutti i tentativi effettuati
		for (int valore : utente.getMapTentativiUtente().values()) {
			somma += valore;
		}

		if (partiteGiocate != 0) {
			//ho cambiato il calcolo del punteggio:
			//ho messo il numero totale delle partite vinte * punteggio per la partita vinta che vale 12
			//l'ho sommato al numero dei tentativi massimi - il numero di tutti i tentativi
			punteggio = (partiteVinte * 12 ) + (13 - somma);

		} else {
			// caso in cui non ha mai giocato !
			return -1;
		}

		System.out.println("PUNTEGGIO: " + punteggio + " UTENTE: "+ nome);
		return punteggio;

	}

	//invio dati multicast al server
	public static void sendDataToServer(double value, String str) {
		try (DatagramSocket datagramSocket = new DatagramSocket()) {
			InetAddress group = InetAddress.getByName(MULTICAST);

			// Unisco i due valori con un delimitatore: punteggio e nome
			String data = value + ":" + str;

			// Converte la stringa in array di byte e crea il pacchetto
			byte[] buffer = data.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MCASTPORT);

			// Invia il pacchetto al server di multicast
			datagramSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




}