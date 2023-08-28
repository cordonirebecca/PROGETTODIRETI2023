import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.*;
import java.util.concurrent.*;

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
    public void registerListener(InterfaceToRankClient listener, String nome) {
        if (!listeners.contains(listener)) {
            listeners.put(nome, listener);
            System.out.println("Iscrizione alle callback effettuata con successo: "+ nome );
        }

    }

    //cancellazione per callback
    public void unregisterListener(InterfaceToRankClient listener, String nome) {
        listeners.remove(nome,listener);
        System.out.println("Cancellazione alle callback effettuata con successo: " + nome );
    }

    
    //metodo per avvisare sulla modifica della classifica
    public void notifyRankUpdate( String nome, List<Map.Entry<String, Double>> list) {

    	if(listeners.get(nome) != null) {
            try{
                System.out.println("avviso i seguenti utenti che è cambiata la classifica: " + listeners.get(nome));
                InterfaceToRankClient client = listeners.get(nome);
                client.rankUpdated(list);
            }catch (NullPointerException e){
            } catch (RemoteException e) {
               e.printStackTrace();
            }
        }
    }


    //funzione che ritorna la lista ordinata
    public List<Map.Entry<String, Double>> SendClassifica(){
        return list;
    }
    

}
