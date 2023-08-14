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
	public boolean registrazioneUtente(String username, String password) throws RemoteException {
		// Implementa la logica per la registrazione dell'utente qui
		// salva il nome utente e la password in un database UTENTEMAP
		if (UtenteMap.get(username) == null) {
			ConcurrentLinkedQueue<String> paroleGiocate = new ConcurrentLinkedQueue<>();
			ConcurrentLinkedQueue<String> paroleNonGiocate = new ConcurrentLinkedQueue<>();
			ConcurrentHashMap<String, Integer> MapTentativiUtente = new ConcurrentHashMap<String, Integer>();
			UtenteMap.putIfAbsent(username, new Utente(username, password, paroleGiocate, paroleIndovinate,
					partiteVinte, partiteGiocate, tentativoUtente, wordGuessed,MapTentativiUtente,paroleNonGiocate,tentativiPartiteVinte));
			
			ServerMain.salvataggioUtenteMap();
			System.out.println("salvataggio RMI andato a buon fine");
			return true; // registrazione andata a buon fine
		} else {
			return false; // registrazione fallita
		}
	}
}