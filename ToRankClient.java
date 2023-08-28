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
    public void rankUpdated(List<Map.Entry<String, Double>> list) throws RemoteException {
        this.list.addAll(list);
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println("PRIME TRE POSIZIONI VARIATE ! ");

        int position = 1;
        for (Map.Entry<String, Double> entry : list) {
            if (position <= 3) {  // Stampa solo le prime tre posizioni
                System.out.println("(" + position + ") " + entry);
            }
            position++;
        }
    }
}
