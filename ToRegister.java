import java.io.Serial;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class ToRegister extends RemoteServer implements InterfaceToRegister {

	// Struttura dati contenente gli utenti registrati.
	private final ConcurrentHashMap<String, Utente> UtenteMap;
	private final ConcurrentLinkedQueue<String> paroleIndovinate;
	int partiteGiocate;
	int partiteVinte;
	int tentativoUtente;
	boolean wordGuessed;
	Vector<Integer> tentativiPartiteVinte;

	// costruttore
	public ToRegister(ConcurrentHashMap<String, Utente> UtenteMap, ConcurrentLinkedQueue<String> paroleIndovinate)
			throws RemoteException {
		super();
		this.UtenteMap = UtenteMap;
		this.paroleIndovinate = paroleIndovinate;
	}

	@Override
	public Integer registrazioneUtente(String username, String password) throws RemoteException {
		// Controlla se l'utente esiste già nel UTENTEMAP e se la password corrisponde
		if (UtenteMap.containsKey(username)) {
			Utente existingUser = UtenteMap.get(username);

			// Controlla se la password corrisponde
			if (existingUser.getPassword().equals(password)) {
				return -1; // Utente già registrato, password corretta
			} else {
				return -2; // Utente già registrato, password errata
			}
		} else {
			// Utente non registrato, procedi con la registrazione
			ConcurrentLinkedQueue<String> paroleGiocate = new ConcurrentLinkedQueue<>();
			ConcurrentLinkedQueue<String> paroleNonGiocate = new ConcurrentLinkedQueue<>();
			ConcurrentHashMap<String, Integer> MapTentativiUtente = new ConcurrentHashMap<String, Integer>();
			UtenteMap.putIfAbsent(username, new Utente(username, password, paroleGiocate, paroleIndovinate,
					partiteVinte, partiteGiocate, tentativoUtente, wordGuessed, MapTentativiUtente, paroleNonGiocate, tentativiPartiteVinte));

			ServerMain.salvataggioUtenteMap();
			return 0; // Registrazione andata a buon fine
		}
	}

}