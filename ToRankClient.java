import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ToRankClient extends RemoteException implements InterfaceToRankClient {

	private static  List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>();

    public ToRankClient(List<Map.Entry<String, Double>> list) throws RemoteException {
        super();
        this.list = list;
        
    }

    @Override
    public void rankUpdated( List<Map.Entry<String, Double>> list) throws RemoteException {
    	 this.list.addAll(list);
         System.out.println("     -------------------- CALLBACK -------------------- " +
                 "\n      Prime tre posizioni della classifica aggiornate" +
                 "\n     -------------------------------------------------- ");
         System.out.println(this.list);
    }
}