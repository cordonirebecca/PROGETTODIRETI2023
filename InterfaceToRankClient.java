import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface InterfaceToRankClient extends Remote {

    void rankUpdated(List<Map.Entry<String, Double>> list) throws RemoteException;
}