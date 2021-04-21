import java.io.File;  																													//Library for handling files
import java.io.FileWriter;																											//Library for writing to files
import java.io.IOException;																											//Library for handling exceptions
import java.io.FileOutputStream;																								//Library for handling file output streams, in order to create images
import java.util.List;																													//Library for Lists
import java.util.ArrayList; 																										//Library for ArrayLists

public class userApp {

	//Request codes that change in each 2-hour session
	public static String echoRequestCode =  "E0344";
	public static String imageRequestCode = "M1305";
	public static String imageRequestCodeError = "G5273";
	public static String gpsRequestCode = "P7783";
	public static String ackCode = "Q2584";
	public static String nackCode = "R7462";

	public static void main(String[] param) {
		(new userApp()).demo();
	}

	//Initializes the modem
	public Modem initModem(int speed, int timeout){
		Modem modem;
		modem=new Modem();
		modem.setSpeed(speed);
		modem.setTimeout(timeout);
		modem.open("ithaki");
		return modem;
	}

	//Receives what Ithaki sends without sending a request code. Prints the packet on the terminal if printRx is true. Packet comes as a string.
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

	//Sends a request code and receives the answer. Prints the packet on the terminal if printRx is true.
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

	//Creates a file.
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

	//Writes a string to a file.
	public void writeToFile(String fileName, String toWrite){
		try {
			FileWriter myWriter = new FileWriter(fileName, true);											//The true argument is needed in order to append to the file and not overwrite it.
			myWriter.write(toWrite);
			myWriter.close();
		//	System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	//Appends a string to a file.
	public void appendToFile(String fileName, String toWrite){
		try{
			FileWriter myWriter = new FileWriter(fileName, true);
			myWriter.append(toWrite);
			myWriter.flush();
			myWriter.close();
		} catch(IOException e){
			System.out.println("An error occured.");
			e.printStackTrace();
		}
	}

	//Receives a lot of echo packets and calculates each packet's response time in ms. Writes the results in a txt file.
	public void makeEchoPacketsList(Modem modem, int packetsNum){
		long time1;
		long timePassed;
		createFile("echopackets.txt");
		writeToFile("echopackets.txt", "Echo packets received: \n\n");
		createFile("echopackets.csv");
		appendToFile("echopackets.csv", "Response time (ms)");
		appendToFile("echopackets.csv", "\n");

		for(int i=0; i<packetsNum; i++){
			time1 = System.currentTimeMillis();
			String rxmessage = sendAndListen(modem, echoRequestCode, "PSTOP", false);	//Echo packets don't have an \r character after PSTOP for some reason.
			timePassed = System.currentTimeMillis() - time1;
			writeToFile("echopackets.txt", rxmessage+"\t"+timePassed+" ms\r\n");
			appendToFile("echopackets.csv", String.valueOf(timePassed));
			appendToFile("echopackets.csv", "\n");
		}
	}

	//Receives an image and stores it into a jpeg file. Also stores its info in a txt file for checking purposes.
	public void receiveImage(Modem modem, String requestCode){
		//We take the requestCode as an argument because it could either be an error free imageRequestCode or an imageRequestCodeError one.
		ArrayList<Integer> intList = new ArrayList<Integer>();
		ArrayList<Byte> byteList = new ArrayList<Byte>();
		int k;
		boolean foundEndDelimiter = false;
		int lastValue = 0;

		String jpegFileName = "";
		String txtFileName = "";
		if(requestCode==imageRequestCode){
			jpegFileName = "imageNoError.jpeg";
			txtFileName = "imageNoError.txt";
		}
		if(requestCode==imageRequestCodeError){
			jpegFileName = "imageWithError.jpeg";
			txtFileName = "imageWithError.txt";
		}

		String txmessageToSend = requestCode + "\r";
		modem.write(txmessageToSend.getBytes());
		System.out.println("Sent request for image.");
		String toPrint = "Sent " + requestCode + " request code.";
		System.out.println(toPrint);

		for(;;){
			try{
				k = modem.read();
				if (k==-1){
					System.out.println("Connection closed.");
					break;
				}
				intList.add(k);
				byteList.add((byte)k);
				if(lastValue==255 && k==217){foundEndDelimiter=true;}
				if(foundEndDelimiter){
					System.out.println("End of listen message.");
					break;
				}
				else{
					lastValue = k;
				}
			} catch(Exception x){
				break;
			}
		}

		//Now intList stores the image in an ArrayList<Integer> form. We will now write that into a txt file for checking (optional).
		createFile(txtFileName);
		writeToFile(txtFileName, "Int array of image:/n/n");
		for(int i=0; i<intList.size();i++){
			writeToFile(txtFileName, intList.get(i)+"/r/n");
		}

		//We will write the int array into a jpeg file using FileOutputStream.

		//Create the jpeg file.
		try {
			File imgFile = new File(jpegFileName);
		  if (imgFile.createNewFile()) {
		  	System.out.println("File created: " + imgFile.getName());
		  } else {
		    System.out.println("File already exists.");
		  }
		} catch (IOException e) {
		  System.out.println("An error occurred.");
		  e.printStackTrace();
		}

		//Convert byteList to byteArr
		byte byteArr[] = new byte[byteList.size()];
		for(int i=0; i<byteList.size();i++){
			byteArr[i] = byteList.get(i);
		}

		//Use FileOutputStream to copy byteArr into jpeg file.
		try{
			FileOutputStream fos = new FileOutputStream(jpegFileName);
			fos.write(byteArr);
			fos.close();
		} catch(Exception e){
			System.out.println(e);
		}
	}

	//Receives an image with a GPS trace (or more), according to the gpsRequestCode we pass. Stores it into a jpeg file as well as a txt file.
	public void receiveGPSimage(Modem modem, String requestCode){
		ArrayList<Integer> intList = new ArrayList<Integer>();
		ArrayList<Byte> byteList = new ArrayList<Byte>();
		int k;
		boolean foundEndDelimiter = false;
		int lastValue = 0;

		String jpegFileName = "gpsImage.jpeg";
		String txtFileName = "gpsImage.txt";
		String txmessageToSend = requestCode + "\r";
		modem.write(txmessageToSend.getBytes());
		System.out.println("Sent request for image.");
		String toPrint = "Sent " + requestCode + " request code.";
		System.out.println(toPrint);

		for(;;){
			try{
				k = modem.read();
				if (k==-1){
					System.out.println("Connection closed.");
					break;
				}
				intList.add(k);
				byteList.add((byte)k);
				if(lastValue==255 && k==217){foundEndDelimiter=true;}
				if(foundEndDelimiter){
					System.out.println("End of listen message.");
					break;
				}
				else{
					lastValue = k;
				}
			} catch(Exception x){
				break;
			}
		}

		//Now intList stores the image in an ArrayList<Integer> form. We will now write that into a txt file for checking (optional).
		createFile(txtFileName);
		writeToFile(txtFileName, "Int array of image:/n/n");
		for(int i=0; i<intList.size();i++){
			writeToFile(txtFileName, intList.get(i)+"/r/n");
		}

		//We will write the int array into a jpeg file using FileOutputStream.

		//Create the jpeg file.
		try {
			File imgFile = new File(jpegFileName);
		  if (imgFile.createNewFile()) {
		  	System.out.println("File created: " + imgFile.getName());
		  } else {
		    System.out.println("File already exists.");
		  }
		} catch (IOException e) {
		  System.out.println("An error occurred.");
		  e.printStackTrace();
		}

		//Convert byteList to byteArr
		byte byteArr[] = new byte[byteList.size()];
		for(int i=0; i<byteList.size();i++){
			byteArr[i] = byteList.get(i);
		}

		//Use FileOutputStream to copy byteArr into jpeg file.
		try{
			FileOutputStream fos = new FileOutputStream(jpegFileName);
			fos.write(byteArr);
			fos.close();
		} catch(Exception e){
			System.out.println(e);
		}
	}

	//Takes arqPacket string as argument and finds its FCS by parsing the string.
	public int fcs(String arqPacket){
		String fcsString = "";
		int startIndex = 0;
		for (int i=0; i<arqPacket.length(); i++){
			if(arqPacket.charAt(i) == '>'){
				startIndex = i+2;
				break;
			}
		}
		for(int i=0; i<3; i++){
			fcsString += arqPacket.charAt(startIndex + i);
		}
		int fcs;
		fcs = Integer.parseInt(fcsString);
		return fcs;
	}

	//Takes arqPacket string and finds the XOR result of its characters.
	public int xorResult(String arqPacket){
		String xSeq = "";
		int startIndex = 0;

		//Find the beginning of the X sequence.
		for (int i=0; i<arqPacket.length(); i++){
			if(arqPacket.charAt(i) == '<'){
				startIndex = i+1;
				break;
			}
		}

		//Save the X sequence into xSeq string.
		int j = startIndex;
		while(arqPacket.charAt(j) != '>'){
			xSeq += arqPacket.charAt(j);
			j++;
		}

		//Make each character in xSeq a number (ASCII).
		ArrayList<Integer> xArr = new ArrayList<Integer>();
		for (int i=0; i<xSeq.length(); i++){
			xArr.add((int)(xSeq.charAt(i)));
		}

		//XOR all the X characters and save result into p.
		int p = xArr.get(0) ^ xArr.get(1);
		for(int i=2; i<xSeq.length(); i++){
			p = p ^ xArr.get(i);
		}
		return p;
	}

	//Implements an ARQ mechanism, for duration milliseconds (taken as an argument).
	public void arqMechanism(Modem modem, long duration){
		createFile("arqpackets.txt");
		writeToFile("arqpackets.txt", "ARQ packets received: \n\n");

		createFile("ARQrepetitions.csv");
		appendToFile("ARQrepetitions.csv", "Repetitions");
		appendToFile("ARQrepetitions.csv", ", ");
		appendToFile("ARQrepetitions.csv", "Number of times");
		appendToFile("ARQrepetitions.csv", "\n");

		createFile("ARQresponseTimes.csv");
		appendToFile("ARQresponseTimes.csv", "Response times (ACK)");
		appendToFile("ARQresponseTimes.csv", "\n");

		int ackNum = 1;
		int nackNum = 0;
		String nextCode;
		long startTime = System.currentTimeMillis();
		long responseTime;
		//Send an ACK to begin.
		String rxmessage = sendAndListen(modem, ackCode, "PSTOP", false);
		long timePassed = System.currentTimeMillis() - startTime;
		responseTime = System.currentTimeMillis() - startTime;
		long lastTimeACK = startTime;
		writeToFile("arqpackets.txt", rxmessage+"\n");

		int repetitions = 0;
		//repTimes[0] stores how many times we had to repeat 0 times and so on.
		int repTimes[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

		//Keep sending and receiving as long as time passed < duration.
		do{
			if(fcs(rxmessage) == xorResult(rxmessage)){
				nextCode = ackCode;
				ackNum++;
				repTimes[repetitions] += 1;
				repetitions = 0;
				responseTime = System.currentTimeMillis() - lastTimeACK;
				lastTimeACK = System.currentTimeMillis();
				appendToFile("ARQresponseTimes.csv", String.valueOf(responseTime));
				appendToFile("ARQresponseTimes.csv", "\n");
			}
			else{
				nextCode = nackCode;
				nackNum++;
				repetitions++;
			}
			rxmessage = sendAndListen(modem, nextCode, "PSTOP", false);
			timePassed = System.currentTimeMillis() - startTime;
			writeToFile("arqpackets.txt", rxmessage+"  "+nextCode+"\n");
		}while(timePassed <= duration);

		//Write the results neatly in a txt file for checking purposes.
		writeToFile("arqpackets.txt", "Number of ACK packets: "+ackNum+"\n");
		writeToFile("arqpackets.txt", "Number of NACK packets: "+nackNum+"\n");
		for(int i=0; i<11; i++){
			writeToFile("arqpackets.txt", String.valueOf(i)+" repetitions: "+String.valueOf(repTimes[i])+"\n");
			appendToFile("ARQrepetitions.csv", String.valueOf(i));
			appendToFile("ARQrepetitions.csv", ", ");
			appendToFile("ARQrepetitions.csv", String.valueOf(repTimes[i]));
			appendToFile("ARQrepetitions.csv", "\n");
		}
	}

	public void demo() {
		//Initialize modem.
		Modem modem = initModem(8000, 2000);																				//For text, speed=1000. For images, speed=80000.

		//Listen for welcome message from Ithaki.
		String welcomeMessage = listen(modem, "\r\n\n\n", true);

		//Send a test message and listen for answer.
		String testMessage = sendAndListen(modem, "test", "PSTOP\r\n", true);

		//Send echoRequestCodes and listen for answers. Write them to echopackets.txt file, along with the response time for each packet. Write times to echopackets.csv file.
		//To run for at least 4 minutes, the number of packets must be 600-650 when we want to make the G1 graph (for speed=1000bps).
		//makeEchoPacketsList(modem, 650);

		//Receive an image with no error and one with error.
		//receiveImage(modem, imageRequestCode);
		//receiveImage(modem, imageRequestCodeError);

		//Receive GPS track packets.
		// String tCode1 = "T=403096225969";
		// String tCode2 = "T=403196225969";
		// String tCode3 = "T=403296225969";
		// String tCode4 = "T=403396225969";
		//
		// String rCode = "R=12341";
		// receiveGPSimage(modem, gpsRequestCode+rCode);

		//ARQ mechanism.
		//arqMechanism(modem, 10000);

		//Close modem
		modem.close();
	}
}
