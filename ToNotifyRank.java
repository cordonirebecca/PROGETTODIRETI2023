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
	 
   
    public ToNotifyRank() throws RemoteException {
        super();
        listeners = new ConcurrentHashMap<>();
    }

    
    //metodo per aggiungere gli utenti alla lista degli utenti per le callback
    public void registerListener(InterfaceToRankClient listener, String nome) {
        if (!listeners.contains(listener)) {
            listeners.putIfAbsent(nome, listener);
            System.out.println("iscrizione alle callback effettuata con successo" );
        }

    }

    
    //cancellazione per callback
    public void unregisterListener(InterfaceToRankClient listener, String nome) {
        listeners.remove(nome,listener);
        System.out.println("cancellazione alle callback effettuata con successo" );
    }

    
    //metodo per avvisare sulla modifica della classifica
    public void notifyRankUpdate( String nome, List<Map.Entry<String, Double>> list) {
    	
    	if(listeners.get(nome) != null) {
            try{
                InterfaceToRankClient client = listeners.get(nome);
                client.rankUpdated(list);
            }catch (NullPointerException e){
                System.out.println("Cliente non loggato al momento dello share");
            } catch (RemoteException e) {
               e.printStackTrace();
            }
        }
        System.out.println("Servizio Callbacks completato ");
    }
    

}