import java.io.File;  // Import the File class
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors

public class userApp {

	public static String echoRequestCode =  "E0223";
	public static String imageRequestCode = "M8880";
	public static String imageRequestCodeError = "G6145";
	public static String gpsRequestCode = "P7268";
	public static String ackCode = "Q9717";
	public static String nackCode = "R6418";

	public static void main(String[] param) {
		(new userApp()).demo();
	}

	public Modem initModem(int speed, int timeout){
		Modem modem;
		modem=new Modem();
		modem.setSpeed(speed);
		modem.setTimeout(timeout);
		modem.open("ithaki");
		return modem;
	}

	public String listen(Modem modem, String delimiter, boolean printRx){
		int k;
		String rxmessage ="";
		System.out.println("Sent request for message.");

		//Endless loop for listening for message
		for (;;) {
			try {
				k=modem.read();																													//Read a byte from the modem
				if (k==-1){
					System.out.println("Connection closed.");
					break;
				}
				//System.out.println((char)k);
				//System.out.print((char)k+" <"+k+"> ");
				rxmessage = rxmessage + (char)k;
				//System.out.println(rxmessage);
				//Break endless loop by catching break sequence.
				if (rxmessage.indexOf(delimiter)>-1){																		//If the break sequence is found break
					if(printRx==true){System.out.println(rxmessage);}
					System.out.println("End of listen message.");
					break;
				}
			} catch (Exception x) {
				break;
			}
		}
		return rxmessage;
	}

	public String sendAndListen(Modem modem, String txmessage, String delimiter, boolean printRx){
		int k;
		String rxmessage = "";
		//We create a different variable for the message we will send to Ithaki. The reason for differentiating is because for some reason the terminal couldn't print the
		//txmessage if we sent it along with the txmessage.
		String txmessageToSend = txmessage + "\r";
		modem.write(txmessageToSend.getBytes());
		String toPrint = "Sent " + txmessage + " request code.";
		System.out.println(toPrint);
		rxmessage = listen(modem, delimiter, printRx);
		return rxmessage;
	}

	public void createFile(String fileName){
		try {
			File myObj = new File(fileName);
      if (myObj.createNewFile()) {
      	System.out.println("File created: " + myObj.getName());
      } else {
        System.out.println("File already exists.");
      }
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
	}

	public void writeToFile(String fileName, String toWrite){
		try {
			FileWriter myWriter = new FileWriter(fileName, true);											//The true argument is needed in order to append to the file and not overwrite it.
			myWriter.write(toWrite);
			myWriter.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	public void demo() {
		//Initialize modem
		Modem modem = initModem(1000, 2000);

		//Listen for welcome message
		String welcomeMessage = listen(modem, "\r\n\n\n", true);

		//Send a test message and listen for answer
		String testMessage = sendAndListen(modem, "test", "PSTOP\r\n", true);

		//Create text files to store packets we receive
		createFile("echopackets.txt");
		//createFile("imagepacket.txt");
		createFile("gpspackets.txt");
		writeToFile("echopackets.txt", "Echo packets received: \n\n");
		//writeToFile("imagepacket.txt", "Image packet received: \n\n");
		writeToFile("gpspackets.txt", "GPS packets received: \n\n");

		long time1 = System.currentTimeMillis();
		//Send echoRequestCodes and listen for answers. Write them to echopackets.txt file
		for(int i=0; i<10; i++){
			String rxmessage = sendAndListen(modem, echoRequestCode, "PSTOP", false);
			writeToFile("echopackets.txt", rxmessage+"\r\n");
		}
		long timePassed = System.currentTimeMillis() - time1;
		System.out.println("It took "+timePassed + " ms to receive the echo packets.");
		writeToFile("echopackets.txt", "It took "+timePassed + " ms to receive the echo packets.\r\n");

		//Send imageRequestCode and listen for answer. Write it to imagepacket.txt file
		// String rxmessage = sendAndListen(modem, imageRequestCode, "PSTOP", false);
		// writeToFile("imagepacket.txt", rxmessage+"\r\n");

		//Send gpsRequestCode and listen for answer. Write it to gpspacket.txt file
		String gpsMessage = sendAndListen(modem, gpsRequestCode, "STOP ITHAKI GPS TRACKING\r", false);
		writeToFile("gpspackets.txt", gpsMessage+"\r\n");


		//Close modem
		modem.close();
	}
}


//NOTES
// NOTE : Stop program execution when "NO CARRIER" is detected.
// NOTE : A time-out option will enhance program behavior.
