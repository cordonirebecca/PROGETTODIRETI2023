import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMulticast implements Runnable {
    private int MCASTPORT;
    private String MULTICAST;
    private static ConcurrentHashMap<String,Double> datiRicevutiMulticast;

    public ClientMulticast(int MCASTPORT, String MULTICAST,ConcurrentHashMap<String,Double> datiRicevutiMulticast) {
        this.MCASTPORT = MCASTPORT;
        this.MULTICAST = MULTICAST;
        this.datiRicevutiMulticast = datiRicevutiMulticast;
    }

    @SuppressWarnings("deprecation")
	@Override
    public void run() {
        try (// Creazione del socket di multicast
		MulticastSocket multicastSocket = new MulticastSocket(MCASTPORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST);

            // Aggiunta del socket al gruppo di multicast
            multicastSocket.joinGroup(group);
          
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.println("Ora sei registrato al gruppo di multicast !");
			System.out.println("Per condividere l'esito delle tue partite digita 'share:nome'");


            while (true) {
                // Ricezione delle notifiche dal gruppo di multicast
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);
                String notification = new String(packet.getData(), 0, packet.getLength());

                
                //memorizzo le notifiche nella map con il nome dell'utente e il punteggio
                String[] parts = notification.split(":");
                if (parts.length == 2) {
                    String nome = parts[1];
                    double valore = Double.parseDouble(parts[0]);
                    datiRicevutiMulticast.put(nome, valore);
                    
                } else {
                    System.out.println("Notifica ricevuta non valida: " + notification);
                }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}