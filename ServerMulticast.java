import java.io.*;
import java.net.*;

public class ServerMulticast implements Runnable {
	private int MCASTPORT;
	private String MULTICAST;

	public ServerMulticast(int MCASTPORT, String MULTICAST) {
		this.MCASTPORT = MCASTPORT;
		this.MULTICAST = MULTICAST;
	}

	@Override
	public void run() {
		try {
			// Creazione del socket di multicast
			MulticastSocket multicastSocket = new MulticastSocket(MCASTPORT);
			InetAddress group = InetAddress.getByName(MULTICAST);

			// Aggiunta del socket al gruppo di multicast
			multicastSocket.joinGroup(group);


			System.out.println("Server in attesa di punteggi...");

			while (true) {
				// Ricezione della richiesta del client
				byte[] buffer = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				multicastSocket.receive(packet);
				String request = new String(packet.getData(), 0, packet.getLength());

				System.out.println("REQUEST: " + request);

				// Invio della notifica al gruppo di multicast
				//request contiene il double:nome
				byte[] data = request.getBytes();
				DatagramPacket notificationPacket = new DatagramPacket(data, data.length, group, MCASTPORT + 1);
				multicastSocket.send(notificationPacket);
				System.out.println("Notifica inviata al gruppo di multicast: " + request);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}