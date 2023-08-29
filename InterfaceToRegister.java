import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceToRegister extends Remote {

	Integer registrazioneUtente(String username, String password) throws RemoteException;
}
