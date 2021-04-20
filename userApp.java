import java.io.File;  																													//Library for handling files
import java.io.FileWriter;																											//Library for writing to files
import java.io.IOException;																											//Library for handling exceptions
import java.io.FileOutputStream;																								//Library for handling file output streams, in order to create images
import java.util.List;																													//Library for Lists
import java.util.ArrayList; 																										//Library for ArrayLists

public class userApp {

	//Request codes that change in each 2-hour session
	public static String echoRequestCode =  "E1018";
	public static String imageRequestCode = "M5879";
	public static String imageRequestCodeError = "G3088";
	public static String gpsRequestCode = "P4530";
	public static String ackCode = "Q9289";
	public static String nackCode = "R6344";

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

	//Receives a lot of echo packets and calculates each packet's response time in ms. Writes the results in a txt file.
	public void makeEchoPacketsList(Modem modem, int packetsNum){
		long time1;
		long timePassed;
		createFile("echopackets.txt");
		writeToFile("echopackets.txt", "Echo packets received: \n\n");
		for(int i=0; i<packetsNum; i++){
			time1 = System.currentTimeMillis();
			String rxmessage = sendAndListen(modem, echoRequestCode, "PSTOP", false);	//Echo packets don't have an \r character after PSTOP for some reason.
			timePassed = System.currentTimeMillis() - time1;
			writeToFile("echopackets.txt", rxmessage+"\t"+timePassed+" ms\r\n");
		}
	}

	public void receiveImage(Modem modem, String requestCode){
		//We take the requestCode as an argument because it could either be an error free imageRequestCode or an imageRequestCodeError one.
		ArrayList<Integer> intList = new ArrayList<Integer>();
		ArrayList<Byte> byteList = new ArrayList<Byte>();
		int k;
		String rxmessage="";
		boolean foundEndDelimiter = false;
		int lastValue = 0;

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
				rxmessage = rxmessage + (char)k;
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

		//Now intArr stores the image in an int array form. We will now write that int array into a txt file.
		createFile("imagepacket.txt");
		writeToFile("imagepacket.txt", "Int array of image:/n/n");
		for(int i=0; i<intList.size();i++){
			writeToFile("imagepacket.txt", intList.get(i)+"/r/n");
		}

		//We will write the int array into a jpg file using FileOutputStream.
		//Create the jpeg file.
		try {
			File imgFile = new File("imgFile.jpeg");
		  if (imgFile.createNewFile()) {
		  	System.out.println("File created: " + imgFile.getName());
		  } else {
		    System.out.println("File already exists.");
		  }
		} catch (IOException e) {
		  System.out.println("An error occurred.");
		  e.printStackTrace();
		}

		//Convert intArr to byteArr
		//int intArr[] = intList.toArray();
		byte byteArr[] = new byte[byteList.size()];
		for(int i=0; i<byteList.size();i++){
			byteArr[i] = byteList.get(i);
		}

		//Use FileOutputStream to copy byteArr into jpg file.
		try{
			FileOutputStream fos = new FileOutputStream("imgFile.jpeg");
			fos.write(byteArr);
			fos.close();
		} catch(Exception e){
			System.out.println(e);
		}
	}

	public void demo() {
		//Initialize modem.
		Modem modem = initModem(80000, 2000);																				//For text, speed=1000. For images, speed=80000.

		//Listen for welcome message from Ithaki.
		String welcomeMessage = listen(modem, "\r\n\n\n", true);

		//Send a test message and listen for answer.
		String testMessage = sendAndListen(modem, "test", "PSTOP\r\n", true);

		//Send echoRequestCodes and listen for answers. Write them to echopackets.txt file, along with the response time for each packet.
		//The number of packets changes to 600-650 when we want to make the G1 graph. For now we keep it to a low number for simplicity.
		//makeEchoPacketsList(modem, 2);

		receiveImage(modem, imageRequestCode);

		//Receive an image
		// createFile("imagepacket.txt");
		// writeToFile("imagepacket.txt", "Image packet received: \n\n");
		// try {
		// 	File imgFile = new File("imgFile.jpeg");
    //   if (imgFile.createNewFile()) {
    //   	System.out.println("File created: " + imgFile.getName());
    //   } else {
    //     System.out.println("File already exists.");
    //   }
    // } catch (IOException e) {
    //   System.out.println("An error occurred.");
    //   e.printStackTrace();
    // }
		//
		// int k;
		// String rxmessage = "";
		// String txmessageToSend = imageRequestCode+"/r";
		// modem.write(txmessageToSend.getBytes());
		// System.out.println("Sent request for message.");
		//
		// //Endless loop for listening for message
		// for (;;) {
		// 	try {
		// 		k=modem.read();																													//Read a byte from the modem
		// 		if (k==-1){
		// 			System.out.println("Connection closed.");
		// 			break;
		// 		}
		// 		System.out.println(k);
		// 		writeToFile("imagepacket.txt", k+"\r\n");
		//
		// 		//Break endless loop by catching break sequence.
		// 		if (rxmessage.indexOf("PSTOP")>-1){																		//If the break sequence is found break
		// 			System.out.println("End of listen message.");
		// 			break;
		// 		}
		// 	} catch (Exception x) {
		// 		break;
		// 	}
		// }


		//This piece of code wrote a binary file to txt that starts with -1 -40 and ends with -1 -39, but the image won't show.
		// String rxmessage = sendAndListen(modem, imageRequestCode, "PSTOP", false);
		// try{
		// 	FileOutputStream fos = new FileOutputStream("imgFile.jpeg");
		// 	byte byteArr[] = rxmessage.getBytes();																		//Convert string to byte array
		// 	fos.write(byteArr);
		// 	fos.close();
		// } catch(Exception e){
		// 	System.out.println(e);
		// }
		// for(int i=0;i<rxmessage.length();i++){
		// 	writeToFile("imagepacket.txt", (byte)rxmessage.charAt(i)+"\r\n");
		// }
		//end




		//Create text files to store packets we receive
		//createFile("gpspackets.txt");
		//writeToFile("gpspackets.txt", "GPS packets received: \n\n");
		//Send gpsRequestCode and listen for answer. Write it to gpspacket.txt file
		//String gpsMessage = sendAndListen(modem, gpsRequestCode, "STOP ITHAKI GPS TRACKING\r", false);
		//writeToFile("gpspackets.txt", gpsMessage+"\r\n");

		//Close modem
		modem.close();
	}
}

//TO-DO
//1. makeEchoPacketsList: write results in excel file, not txt
//2. write .m script to make G1 graph out of makeEchoPacketsList
//3.


//NOTES
// NOTE : Stop program execution when "NO CARRIER" is detected.
// NOTE : A time-out option will enhance program behavior.
