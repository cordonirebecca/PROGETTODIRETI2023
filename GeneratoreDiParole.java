import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Thread.sleep;

public class GeneratoreDiParole implements Runnable {

	// Variabile che indica il tempo durante il quale la parola da indovinare
	// rimarrà in gioco.
	// Idealmente questo tempo dovrebbe essere pari a 24h ma, al fine di simulare il
	// suo funzionamento, la variabile (letta
	// dal file di configurazione) sarà pari a un minuto.
	// In altre parole, ogni minuto cambierà la parola da giocare.
	int TIMEOUT;
	String filePath;

	/**
	 * Metodo costruttore
	 */
	public GeneratoreDiParole(String filePath, int TIMEOUT) {
		this.TIMEOUT = TIMEOUT;
		this.filePath = filePath;
	}

	/**
	 * Metodo che descrive il comportamento del thread
	 */
	public void run() {

		while (true) {
			List<String> words = new ArrayList<>();

			try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String[] lineWords = line.split("\\s+"); // Splitting line into words
					for (String word : lineWords) {
						if (!word.isEmpty()) {
							words.add(word);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();

			}

			Random random = new Random();
			int index = random.nextInt(words.size());

			// Inserisco la parola scelta nella struttura dati 'paroleDatabase'.
			ServerMain.setParoleDatabase(words.get(index));

			// Durata del gioco per la determinata parola scelta.
			try {
				sleep(TIMEOUT);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}