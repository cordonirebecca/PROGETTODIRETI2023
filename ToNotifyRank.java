import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ToNotifyRank extends RemoteObject implements InterfaceToNotifyRank {
	
	
	//oggetti remoti dei client registrati per ricevere le callback sugli aggiornamenti della classifica.
	 private final ConcurrentHashMap<String, InterfaceToRankClient> listeners;
    public 	List<Map.Entry<String, Double>> list;

    public ToNotifyRank(List<Map.Entry<String, Double>> list) throws RemoteException {
        super();
        listeners = new ConcurrentHashMap<>();
       this.list = list;
    }

    
    //metodo per aggiungere gli utenti alla lista degli utenti per le callback
    public void registerListener(InterfaceToRankClient listener, String nome) throws RemoteException{
        listeners.put(nome, listener);
        System.out.println("Iscrizione alle callback effettuata con successo: "+ nome );
    }

    //cancellazione per callback
    public void unregisterListener(InterfaceToRankClient listener, String nome) throws RemoteException{
        listeners.remove(nome,listener);
        System.out.println("Cancellazione alle callback effettuata con successo: " + nome );
    }

    
    //metodo per avvisare sulla modifica della classifica
    public void notifyRankUpdate(List<Map.Entry<String, Double>> list) throws RemoteException{

        //invio a tutti la notifica della classifica cambiata
        for (Map.Entry<String, InterfaceToRankClient> entry : listeners.entrySet()) {
            try {
                InterfaceToRankClient client = entry.getValue();
                if (client != null) {
                    client.rankUpdated(list);
                }
            } catch (RemoteException e) {
                // Gestione delle eccezioni in caso di errore remoto
                e.printStackTrace();
            }
        }
    }


    //funzione che ritorna la lista ordinata
    public List<Map.Entry<String, Double>> SendClassifica(){
        return list;
    }
    

}
