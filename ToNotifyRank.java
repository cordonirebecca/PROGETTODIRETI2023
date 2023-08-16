import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ToNotifyRank extends RemoteObject implements InterfaceToNotifyRank {
	
	
	//oggetti remoti dei client registrati per ricevere le callback sugli aggiornamenti della classifica.
	 private final ConcurrentHashMap<String, InterfaceToRankClient> listeners;

    public ConcurrentHashMap<String, Double> tabellaClassifica;
	 
   
    public ToNotifyRank( ConcurrentHashMap<String, Double> tabellaClassifica) throws RemoteException {
        super();
        listeners = new ConcurrentHashMap<>();
        this.tabellaClassifica = tabellaClassifica;
    }

    
    //metodo per aggiungere gli utenti alla lista degli utenti per le callback
    public void registerListener(InterfaceToRankClient listener, String nome) {
        if (!listeners.contains(listener)) {
            listeners.putIfAbsent(nome, listener);
            System.out.println("Iscrizione alle callback effettuata con successo" );
        }

    }

    
    //cancellazione per callback
    public void unregisterListener(InterfaceToRankClient listener, String nome) {
        listeners.remove(nome,listener);
        System.out.println("Cancellazione alle callback effettuata con successo" );
    }

    
    //metodo per avvisare sulla modifica della classifica
    public void notifyRankUpdate( String nome, List<Map.Entry<String, Double>> list) {
    	
    	if(listeners.get(nome) != null) {
            try{
                System.out.println("avviso gli utenti che è cambiata la classifica");
                InterfaceToRankClient client = listeners.get(nome);
                client.rankUpdated(list);
            }catch (NullPointerException e){
            } catch (RemoteException e) {
               e.printStackTrace();
            }
        }
    }


    public ConcurrentHashMap<String, Double> SendClassifica(){

        return tabellaClassifica;
    }
    

}
