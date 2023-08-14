import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Utente {

	private final String username;
	private final String psw;
	private final ConcurrentLinkedQueue<String> paroleGiocate;
	private final ConcurrentLinkedQueue<String> paroleIndovinate;
	private final ConcurrentLinkedQueue<String> paroleNonGiocate;
	private int partiteGiocate;
	private int partiteVinte;
	private int tentativoUtente;
	boolean wordGuessed;
	private final ConcurrentHashMap<String, Integer> MapTentativiUtente;
	private final Vector<Integer> tentativiPartiteVinte;

	public Utente(String username, String password, ConcurrentLinkedQueue<String> paroleGiocate,
			ConcurrentLinkedQueue<String> paroleIndovinate, int partiteGiocate, int partiteVinte, int tentativoUtente,
			boolean wordGuessed, ConcurrentHashMap<String, Integer> MapTentativiUtente,
			ConcurrentLinkedQueue<String> paroleNonGiocate,Vector<Integer> tentativiPartiteVinte) {

		this.username = username;
		this.psw = password;
		this.paroleGiocate = paroleGiocate;
		this.paroleIndovinate = paroleIndovinate;
		this.partiteGiocate = partiteGiocate;
		this.partiteVinte = partiteVinte;
		this.tentativoUtente = tentativoUtente;
		this.wordGuessed = wordGuessed;
		this.MapTentativiUtente = MapTentativiUtente;
		this.paroleNonGiocate = paroleNonGiocate;
		this.tentativiPartiteVinte =  new Vector<>();

	}
	
	public void incrementoTentativiPartiteVinte (int valore) {
		tentativiPartiteVinte.add(valore);
	}
	
	public Vector<Integer> getTentativiPartiteVinte() {
		return tentativiPartiteVinte;
	}
	
	public int sommaTentativiPartiteVinte(Vector<Integer> vector) {
		int somma = 0;
        for (Integer elemento : vector) {
            somma += elemento;
        }
        return somma;
	}

	public void SetWordGuessed(boolean valore) {
		wordGuessed = valore;
	}

	public boolean getWordGuessed() {
		return wordGuessed;
	}

	public String getPassword() {
		return psw;
	}

	public String getUsername() {
		return username;
	}

	@Override
	public String toString() {
		return username + " " + psw;
	}

	public ConcurrentLinkedQueue<String> getParoleGiocate() {
		return paroleGiocate;
	}

	public void addParoleGiocate(String parola) {
		paroleGiocate.add(parola);
	}

	public String getUltimoParoleGiocate() {
		if (paroleGiocate == null || paroleGiocate.isEmpty()) {
			return null;
		}

		Iterator<String> iterator = paroleGiocate.iterator();
		String ultimoElemento = null;
		while (iterator.hasNext()) {
			ultimoElemento = iterator.next();
		}

		return ultimoElemento;
	}

	public ConcurrentLinkedQueue<String> getParoleNONGiocate() {
		return paroleNonGiocate;
	}

	public void addParoleNONGiocate(String parola) {
		paroleNonGiocate.add(parola);
	}

	public ConcurrentLinkedQueue<String> getParoleIndovinate() {
		return paroleIndovinate;
	}

	public String getUltimoParoleIndovinate() {

		Iterator<String> iterator = paroleIndovinate.iterator();
		String ultimoElemento = null;
		while (iterator.hasNext()) {
			ultimoElemento = iterator.next();
		}

		return ultimoElemento;
	}

	public int getPartiteGiocate() {
		return partiteGiocate;
	}

	public int getPartiteVinte() {
		return partiteVinte;
	}

	public void incrementaPartiteGiocate() {
		this.partiteGiocate++;
	}

	public void incrementaPartiteVinte() {
		this.partiteVinte++;
	}

	public int getTentativoUtente() {
		return tentativoUtente;
	}

	public int incrementoTentativoUtente(int tentativo) {
		return tentativo + tentativoUtente;
	}

	public ConcurrentHashMap<String, Integer> getMapTentativiUtente() {
		return MapTentativiUtente;
	}

	public Integer ottieniNumeroTentativi(String chiave) {
		return MapTentativiUtente.get(chiave);
	}

}