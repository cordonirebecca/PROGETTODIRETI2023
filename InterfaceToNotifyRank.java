import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface InterfaceToNotifyRank extends Remote {

	void notifyRankUpdate(String nome, List<Map.Entry<String, Double>> list) throws RemoteException;

	public void registerListener(InterfaceToRankClient listener, String nome) throws RemoteException;

	public void unregisterListener(InterfaceToRankClient listener, String nome) throws RemoteException;

}