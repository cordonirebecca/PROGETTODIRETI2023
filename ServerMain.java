import java.io.*;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.*;
import java.net.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.*;


public class ServerMain {

	private static int TCPPORT;
	public static String MULTICAST;
	private static int MCASTPORT;
	private static int REGPORT;
	private static int CALLPORT;
	private static int TIMEOUT;
	static InterfaceToRegister interfacetoregister;
	static InterfaceToRegister stub;
	static Registry registry;
	static ToNotifyRank notifier;

	// lista delle parole che escono
	private static Vector<String> paroleDatabase = new Vector<>();

	// lista utenti Online
	private static ConcurrentLinkedQueue<String> utentiOnline = new ConcurrentLinkedQueue<>();

	// map con gli utenti e i punteggi
	public static ConcurrentHashMap<String, Double> tabellaClassifica = new ConcurrentHashMap<String, Double>();

	// concurrent hashMap che contiene i dati degli utenti:
	// il primo valore rappresenta l'username e il secondo un oggetto di tipo
	// Utente.
	private static ConcurrentHashMap<String, Utente> UtenteMap = new ConcurrentHashMap<String, Utente>();

	// lista con tutti i punteggi della classifica in ordine
	public static List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>();
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// inizio main
	public static void main(String[] args) {

		// lista delle parole indovinate
		ConcurrentLinkedQueue<String> paroleIndovinate = new ConcurrentLinkedQueue<>();

		// il server legge il suo file di configurazione.txt

		File file = new File("FileConfigurazioneServer.txt");

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
						case ("TIMEOUT") -> {
							TIMEOUT = Integer.parseInt(parolaDivisa[1]);
							// System.out.println("TIMEOUT " + TIMEOUT);
						}
					}
				}
			}

		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
		}

		// riattivo tutte le mie strutture dati
		riattivazioneParoleDatabase();
		riattivazioneUtentiOnline();
		riattivazioneTabellaClassifica();
		riattivazioneUtenteMap();

		// funzione che genera in modo casuale la parola del giorno
		// apro il vocabolario e prendo a caso una parola che sarà la Secret Word
		String filePath = "vocabolario.txt";

		// Creiamo e avviamo il thread per generare una nuova secretWord
		try {
			Thread t = new Thread(new GeneratoreDiParole(filePath, TIMEOUT));
			t.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// RMI registro l'utente
		try {
			ToRegister toregister = new ToRegister(UtenteMap, paroleIndovinate);
			InterfaceToRegister stub = (InterfaceToRegister) UnicastRemoteObject.exportObject(toregister, 0);
			LocateRegistry.createRegistry(REGPORT);
			Registry registry = LocateRegistry.getRegistry(REGPORT);
			registry.rebind("InterfaceToRegister", stub);
			System.out.println("Server RMI per registrazione avviato con successo.");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// RMI per le callback
		try {
			notifier = new ToNotifyRank(list);
			InterfaceToNotifyRank stub = (InterfaceToNotifyRank) UnicastRemoteObject.exportObject(notifier, 0);
			// Creazione del registro RMI sulla porta 1099
			Registry registry = LocateRegistry.createRegistry(CALLPORT);

			// Pubblicazione dell'oggetto remoto nel registro
			registry.rebind("InterfaceToNotifyRank", stub);

			System.out.println("Server RMI per callback avviato con successo");
		} catch (RemoteException e) {
			System.err.println("Errore durante l'avvio del server: " + e);

		}

		// Avvia il thread per la comunicazione multicast
		Thread multicastThread = new Thread(new ServerMulticast(MCASTPORT, MULTICAST));
		multicastThread.start();

		/////////////////////
		// connessione TCP
		ExecutorService executorService = Executors.newCachedThreadPool();

		try (ServerSocket serverSocket = new ServerSocket(TCPPORT)) {
			System.out.println("Server started on port " + TCPPORT);

			// il server aspetta dei client
			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("New connection from: " + clientSocket.getInetAddress().getHostAddress());

				// Gestisce la connessione client utilizzando un thread dal pool di thread
				executorService.execute(new ClientHandler(clientSocket, utentiOnline, UtenteMap, MCASTPORT, MULTICAST));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			executorService.shutdown();
		}
	}
// fine main
///////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// estraggo solo la parola tradotta da tutto il path
	private static String extractTranslatedWord(String response) {
		String startTag = "translatedText\":\"";
		String endTag = "\",";

		int startIndex = response.indexOf(startTag);
		if (startIndex != -1) {
			startIndex += startTag.length();
			int endIndex = response.indexOf(endTag, startIndex);
			if (endIndex != -1) {
				return response.substring(startIndex, endIndex);
			}
		}
		return "";
	}

	// metodo per tradurre la secret word con GET HTTP
	public static String translateWord(String word) {
		String translatedWord = "";

		try {
			String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
			String urlStr = "https://mymemory.translated.net/api/get?q=" + encodedWord + "&langpair=en|it";
			URL url = new URL(urlStr);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder response = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					response.append(line);
				}
				reader.close();

				// Parse the JSON response to extract the translated word
				translatedWord = extractTranslatedWord(response.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return translatedWord;
	}

	// metodo per inserire le parole giocate in paroleDatabase
	public static void setParoleDatabase(String parola) {
		paroleDatabase.add(parola);
		salvataggioParoleDatabase();

		System.out.println("Le parole giocate fin ora sono : " + paroleDatabase);
		System.out.println();
	}

	// metodo che ritorna paroleDatabase
	public static Vector<String> getParoleDatabase() {
		return paroleDatabase;
	}

	// metodo che ritorna l'ultimo elemento
	public static String getParolaDatabaseUltima() {
		return paroleDatabase.lastElement();
	}


	// mi serve una funzione che crei la tabella dato il punteggio
	// il puntegggio è calcolato dalla funzione nel clientHandler 'CalcoloPunteggio'
	public static void SetTabellaClassifica(String nome, Double punteggio) {
		// Aggiunta o modifica del punteggio nella tabella
		if (tabellaClassifica.putIfAbsent(nome, punteggio) != null) {
			System.out.println(
					nome + " :il tuo punteggio precedente: " + tabellaClassifica.get(nome) + "\nil tuo nuovo punteggio:" + punteggio);
			tabellaClassifica.replace(nome, tabellaClassifica.get(nome), punteggio);
		}

		// Rimuovi eventuali duplicati dalla lista
		list.removeIf(entry -> entry.getKey().equals(nome));

		// Aggiungi l'entry aggiornata alla lista
		list.add(new AbstractMap.SimpleEntry<>(nome, punteggio));

		// Memorizza i primi tre nomi attuali
		List<String> primiTreNomiAttuali = new ArrayList<>();
		for (int i = 0; i < Math.min(3, list.size()); i++) {
			primiTreNomiAttuali.add(list.get(i).getKey());
		}

		// Ordina la lista in base ai punteggi in modo decrescente
		//chi ha il punteggio più alto sta vincendo quindi sarà più alto
		//in classifica
		list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

		// Estrai i primi tre nomi dalla lista ordinata
		List<String> primiTreNomiNuovi = new ArrayList<>();
		for (int i = 0; i < Math.min(3, list.size()); i++) {
			primiTreNomiNuovi.add(list.get(i).getKey());
		}

		// Confronta i primi tre nomi attuali con i nuovi
		if (!primiTreNomiAttuali.equals(primiTreNomiNuovi)) {
			// Invia notifica che i primi tre nomi sono cambiati
			System.out.println("I primi tre nomi sono cambiati!");
			// invio messaggio rmi
			try{
				notifier.notifyRankUpdate(list);
			}catch (RemoteException e) {
				System.out.println("Errore callaback");
			}

		}

	}


	// metodo per salvare le parole in JSON
	private static void salvataggioParoleDatabase() {
		//creo oggeto gson
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try (BufferedWriter writer = new BufferedWriter(new PrintWriter("Parole_database.json"))) {
			//definisco il tipo di dati che verrà serializzato in JSON
			Type pGiocateType = new TypeToken<Vector<String>>() {
			}.getType();
			String p = gson.toJson(paroleDatabase, pGiocateType);

			if (p != null) {
				writer.write(p);
				//La flush garantisce che i dati siano scritti nel file in quel momento,
				// in modo da evitare problemi di sincronizzazione tra il buffer in memoria e il file su disco.
				writer.flush();

				System.out.println("Salvataggio su Parole_database.json effettuato");
			} else {
				System.out.println("Parole database vuoto");
			}

		} catch (FileNotFoundException f) {

			//se il file non esiste lo creo
			try {
				File fi = new File("Parole_database.json");
				if (fi.createNewFile()) {
					System.out.println("Il file è stato creato correttamente: " + fi);
				} else {
					System.out.println("Errore nella creazione: " + fi);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// metodo per salvare la lista degli utentiOnline in JSON
	public static void salvataggioUtentiOnline() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try (BufferedWriter writer = new BufferedWriter(new PrintWriter("Utenti_online.json"))) {
			//definisco il tipo di dati che verrà serializzato in JSON
			Type pGiocateType = new TypeToken<ConcurrentLinkedQueue<String>>() {
			}.getType();
			String p = gson.toJson(utentiOnline, pGiocateType);

			if (p != null) {
				writer.write(p);
				//La flush garantisce che i dati siano scritti nel file in quel momento,
				// in modo da evitare problemi di sincronizzazione tra il buffer in memoria e il file su disco.
				writer.flush();

				System.out.println("Salvataggio su Utenti_online.json effettuato");
			} else {
				System.out.println("Utenti_online vuoto");
			}

		} catch (FileNotFoundException f) {

			//se il file non esiste lo creo
			try {
				File fi = new File("Utenti_online.json");
				if (fi.createNewFile()) {
					System.out.println("Il file è stato creato correttamente: " + fi);
				} else {
					System.out.println("Errore nella creazione: " + fi);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// metodo per salvare la tabella con la classifica in JSON
	private static void salvataggioTabellaClassifica() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try (BufferedWriter writer = new BufferedWriter(new PrintWriter("Tabella_classifica.json"))) {
			//definisco il tipo di dati che verrà serializzato in JSON
			Type pGiocateType = new TypeToken<ConcurrentHashMap<String, Double>>() {
			}.getType();
			String p = gson.toJson(tabellaClassifica, pGiocateType);

			if (p != null) {
				writer.write(p);
				//La flush garantisce che i dati siano scritti nel file in quel momento,
				// in modo da evitare problemi di sincronizzazione tra il buffer in memoria e il file su disco.
				writer.flush();

				System.out.println("Salvataggio su Tabella_classifica effettuato");
			} else {
				System.out.println("Tabella_classifica vuoto");
			}

		} catch (FileNotFoundException f) {

			//se il file non esiste lo creo
			try {
				File fi = new File("Tabella_classifica.json");
				if (fi.createNewFile()) {
					System.out.println("Il file è stato creato correttamente: " + fi);
				} else {
					System.out.println("Errore nella creazione: " + fi);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// metodo per salvare gli utenti in JSON in JSON
	public static void salvataggioUtenteMap() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try (BufferedWriter writer = new BufferedWriter(new PrintWriter("Utente_map.json"))) {
			//definisco il tipo di dati che verrà serializzato in JSON
			Type pGiocateType = new TypeToken<ConcurrentHashMap<String, Utente>>() {
			}.getType();
			String p = gson.toJson(UtenteMap, pGiocateType);

			if (p != null) {
				writer.write(p);
				//La flush garantisce che i dati siano scritti nel file in quel momento,
				// in modo da evitare problemi di sincronizzazione tra il buffer in memoria e il file su disco.
				writer.flush();

				System.out.println("Salvataggio su Utente_map effettuato");
			} else {
				System.out.println("Utente_map vuoto");
			}

		} catch (FileNotFoundException f) {

			//se il file non esiste lo creo
			try {
				File fi = new File("Utente_map.json");
				if (fi.createNewFile()) {
					System.out.println("Il file è stato creato correttamente: " + fi);
				} else {
					System.out.println("Errore nella creazione: " + fi);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	//ripristino la struttura dati patoledatabase
	private static void riattivazioneParoleDatabase() {
		Gson gson = new Gson();

		//leggo file json
		try (BufferedReader reader = new BufferedReader((new FileReader("Parole_database.json")))) {
			Type vincitoriType = new TypeToken<Vector<String>>() {
			}.getType();
			Vector<String> v = gson.fromJson(reader, vincitoriType);

			//se v non è nulla allora inserisco
			if (v != null) {

				paroleDatabase.addAll(v);
				System.out.println("Riattivazione parole_database.json eseguita corretamente");

			} else {
				System.out.println("Parole_database.json vuoto");
			}

		} catch (FileNotFoundException f) {

			//se il file non esiste lo creo
			try {
				File fi = new File("Parole_database.json");
				if (fi.createNewFile()) {
					System.out.println(fi + " creato correttamente");
				} else {
					System.out.println("Errore nella creazione di " + fi);

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//ripristino la struttura dati UtentiOnline
	private static void riattivazioneUtentiOnline() {
		Gson gson = new Gson();

		//leggo file json
		try (BufferedReader reader = new BufferedReader((new FileReader("Utenti_online.json")))) {
			Type vincitoriType = new TypeToken<ConcurrentLinkedQueue<String>>() {
			}.getType();
			ConcurrentLinkedQueue<String> v = gson.fromJson(reader, vincitoriType);

			//se v non è nulla allora inserisco
			if (v != null) {
				utentiOnline.addAll(v);
				System.out.println("Riattivazione Utenti_online.json eseguita corretamente");

			} else {
				System.out.println("Utenti_online.json vuoto");
			}

		} catch (FileNotFoundException f) {

			//se il file non esiste lo creo
			try {
				File fi = new File("Utenti_online.json");
				if (fi.createNewFile()) {
					System.out.println(fi + " creato correttamente");
				} else {
					System.out.println("Errore nella creazione di " + fi);

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//ripristino la tabellaClassifica
	private static void riattivazioneTabellaClassifica() {
		Gson gson = new Gson();

		//leggo file json
		try (BufferedReader reader = new BufferedReader((new FileReader("Tabella_classifica.json")))) {
			Type vincitoriType = new TypeToken<ConcurrentHashMap<String, Double>>() {
			}.getType();
			ConcurrentHashMap<String, Double> v = gson.fromJson(reader, vincitoriType);

			//se v non è  nulla allora inserisco
			if (v != null) {
				tabellaClassifica.putAll(v);
				System.out.println("Riattivazione Tabella_classifica.json eseguita corretamente");

			} else {
				System.out.println("Tabella_classifica.json vuoto");
			}

		} catch (FileNotFoundException f) {
			//se il file non esiste lo creo
			try {
				File fi = new File("Tabella_classifica.json");
				if (fi.createNewFile()) {
					System.out.println(fi + " creato correttamente");
				} else {
					System.out.println("Errore nella creazione di " + fi);

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//ripristino la utenteMap
	private static void riattivazioneUtenteMap() {
		Gson gson = new Gson();

		//leggo il file json
		try (BufferedReader reader = new BufferedReader((new FileReader("Utente_map.json")))) {
			Type vincitoriType = new TypeToken<ConcurrentHashMap<String, Utente>>() {
			}.getType();
			ConcurrentHashMap<String, Utente> v = gson.fromJson(reader, vincitoriType);

			//se v non è nulla allora inserisco i valori
			if (v != null) {
				UtenteMap.putAll(v);
				System.out.println("Riattivazione Utente_map.json eseguita corretamente");

			} else {
				System.out.println("Utente_map.json vuoto");
			}

		} catch (FileNotFoundException f) {
			//se il file non esiste lo creo
			try {
				File fi = new File("Utente_map.json");
				if (fi.createNewFile()) {
					System.out.println(fi + " creato correttamente");
				} else {
					System.out.println("Errore nella creazione di " + fi);

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}



}