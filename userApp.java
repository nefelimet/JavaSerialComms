import java.io.File;  // Import the File class
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors

public class userApp {

	public static String echoRequestCode =  "E1092";
	public static String imageRequestCode = "M2490";
	public static String imageRequestCodeError = "G0342";
	public static String gpsRequestCode = "P4642";
	public static String ackCode = "Q2289";
	public static String nackCode = "R8766";

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

	public String listen(Modem modem, String delimiter){
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
					System.out.println(rxmessage);
					System.out.println("End of listen message.");
					break;
				}
			} catch (Exception x) {
				break;
			}
		}
		return rxmessage;
	}

	public String sendAndListen(Modem modem, String txmessage, String delimiter){
		int k;
		String rxmessage = "";
		//We create a different variable for the message we will send to Ithaki. The reason for differentiating is because for some reason the terminal couldn't print the
		//txmessage if we sent it along with the txmessage.
		String txmessageToSend = txmessage + "\r";
		modem.write(txmessageToSend.getBytes());
		String toPrint = "Sent " + txmessage + " request code.";
		System.out.println(toPrint);
		rxmessage = listen(modem, delimiter);
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
		String welcomeMessage = listen(modem, "\r\n\n\n");

		//Send a test message and listen for answer
		String testMessage = sendAndListen(modem, "test", "PSTOP\r\n");

		//Create text file to store echo messages
		createFile("echopackets.txt");
		writeToFile("echopackets.txt", "Echo packets received: \n\n");

		//Send echoRequestCodes and listen for answers. Write them to echopackets.txt file.
		for(int i =0; i<5; i++){
			String rxmessage = sendAndListen(modem, echoRequestCode, "PSTOP");
			writeToFile("echopackets.txt", rxmessage+"\r\n");
		}

		//Close modem
		modem.close();
	}
}


//NOTES
// NOTE : Stop program execution when "NO CARRIER" is detected.
// NOTE : A time-out option will enhance program behavior.
