/*
 * 
 * This program takes as input the host name and query type and resolves its IP address.
 * It emulates the DIG tool up to a certain extent.
 */

/** @author Aditya Godambe */
//package fcn;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Date;

import org.xbill.DNS.*;


public class MyDig {

	/**
	 * Queries the list of root servers until a successful response is received
	 * @param rootServers  List of 13 root servers
	 * @param hostname  The name whose resolution is requested
	 * @param type  Type of the query (A, MX, NS)
	 */
	static Message resolveDNSRoot(String[] rootServers, Message queryMsg) {

		Message rootResultMsg = new Message();
		InetAddress ip;
		int rootServerCount = 0;

		try {

			/*
			 * Create a simple resolver that will query each of the root servers
			 * Stops querying when a response is received
			 */

			while(rootResultMsg.numBytes() == 0) {

				//System.out.println("Accessing root server number: " +rootServerCount);
				ip = InetAddress.getByName(rootServers[rootServerCount]);

				SimpleResolver resolver = new SimpleResolver();
				resolver.setAddress(ip);

				//System.out.println("Sending the request message now...");
				//System.out.println("Query to be sent:\n" +queryMsg);
				rootResultMsg = resolver.send(queryMsg);

				rootServerCount++;
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//result = tldNSResolver(queryMsg, rootResultMsg);
		return rootResultMsg;

		//return result;
	}

	//TODO Check for CNAME records
	/**
	 * Queries all the name servers for CNAME types
	 * @param queryMsg  Original query message
	 * @param rootServers  List of root servers
	 * @return  message from the name servers for CNAME types
	 */
	static Message cnameResolver(Message queryMsg, String[] rootServers) {

		Message rootResult = new Message();
		Message tldResultMsg = new Message();
		Message authResultMsg = new Message();

		rootResult = resolveDNSRoot(rootServers, queryMsg);
		tldResultMsg = nsResolver(queryMsg, rootResult);
		authResultMsg = nsResolver(queryMsg, tldResultMsg);

		return authResultMsg;
	}


	/**
	 * 
	 * @param queryMsg  Query to be answered
	 * @param upperLevelMsg  Message received from an upper level in the hierarchy
	 * @return  Message returned by the current level of Name server
	 */
	static Message nsResolver(Message queryMsg, Message upperLevelMsg) {
		Message resultMsg = new Message();
		InetAddress ip;
		int serverCount = 0;
		SimpleResolver resolver;

		Record[] serverList = upperLevelMsg.getSectionArray(2);

		try {
			while(resultMsg.numBytes() == 0) {
				if(serverList.length != 0) {
					ip = InetAddress.getByName(serverList[serverCount].rdataToString());

					resolver = new SimpleResolver();
					resolver.setAddress(ip);

					resultMsg = resolver.send(queryMsg);

					serverCount++;
				}
				else
					break;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultMsg;
	}

	static void displayContents(Message queryResult, long queryTime) {

		System.out.println("QUESTION SECTION:\n" +queryResult.getSectionArray(0)[0]);

		System.out.println("\nANSWER SECTION:");
		for(int i = 0; i < queryResult.getSectionArray(1).length; i++)
			System.out.println(queryResult.getSectionArray(1)[i]);

		System.out.println("\nQuery time: " +queryTime+ " msec");
		System.out.println("WHEN: " +new Date());
		System.out.println("MSG SIZE recd: " +queryResult.numBytes());

	}

	public static void main(String[] args) throws TextParseException {

		if(args.length != 2)
			System.err.print("INVALID INPUT! Correct usage: java MyDig <hostname> <type>");

		final ArrayList<String> types = new ArrayList<String>();
		types.add("A");
		types.add("MX");
		types.add("NS");
		types.add("CNAME");

		if(!types.contains(args[1]))
			System.err.println("INVALID QUERY TYPE! Choose from 'A', 'MX' or 'NS'");


		//List of root servers
		String[] rootServers = {"198.41.0.4", "199.9.14.201", "192.33.4.12", 
				"199.7.91.13", "192.203.230.10", "192.5.5.241", 
				"192.112.36.4", "198.97.190.53", "192.36.148.17", 
				"192.58.128.30", "193.0.14.129", "199.7.83.42", "202.12.27.33"};

		Name host = null;
		Record request = null;

		Message queryMsg = null;
		Message rootResultMsg = new Message();
		Message tldResultMsg = new Message();
		Message authResultMsg = new Message();
		Message cnameResultMsg = new Message();
		Message queryResult = new Message();

		int queryType = Type.value(args[1]);

		long startTime, endTime, queryTime;

		//long totalTime = 0;

		//System.out.println("\n" +args[0]);

		//for(int i = 0; i < 10; i++) {


		host = Name.fromString(args[0], Name.root);
		request = Record.newRecord(host, queryType, DClass.IN);

		queryMsg = Message.newQuery(request);

		rootResultMsg = resolveDNSRoot(rootServers, queryMsg);
		//System.out.println("\n---Contents of rootResult----\n\n" +rootResultMsg);

		tldResultMsg = nsResolver(queryMsg, rootResultMsg);	
		//System.out.println("\n---Contents of tldResult----\n\n" +tldResultMsg);

		authResultMsg = nsResolver(queryMsg, tldResultMsg);
		//System.out.println("\n---Contents of authResult----\n" +authResultMsg);


		// CNAME entry
		if(authResultMsg.getSectionArray(1)[0].getType() == 5) {

			/*
			 * If a CNAME is returned, send the response to the rootServers for 
			 * resolution from scratch
			 */


			cnameResultMsg = nsResolver(queryMsg, authResultMsg);
			//System.out.println("\n---Contents of cnameResult----\n" +cnameResultMsg);

			// Create a new query from cnameMsg
			Record cname = authResultMsg.getSectionArray(1)[0];
			String cHostName = cname.rdataToString();
			//System.out.println(cHostName);

			Name cnameHost = Name.fromString(cHostName, Name.root);
			Record cRequest = Record.newRecord(cnameHost, 1, DClass.IN);
			Message cnameQueryMsg = Message.newQuery(cRequest); 
			Message cnameQueryResult = cnameResultMsg;

			//Append answer section of cname result to original result


			startTime = System.currentTimeMillis();
			queryResult = cnameResolver(queryMsg, rootServers);
			cnameQueryResult = cnameResolver(cnameQueryMsg, rootServers);
			//System.out.println(cnameQueryResult);

			Record answer = cnameQueryResult.getSectionArray(1)[0];
			queryResult.addRecord(answer, 1);
			endTime = System.currentTimeMillis();

			queryTime = endTime - startTime;
		}
		else {
			startTime = System.currentTimeMillis();
			queryResult = nsResolver(queryMsg, tldResultMsg);
			endTime = System.currentTimeMillis();

			queryTime = endTime - startTime;
		}

		//totalTime = totalTime + queryTime;
		//System.out.println(queryTime);
		//System.out.println("\n\n----Displaying results------\n");

		displayContents(queryResult, queryTime);
		//}

		//System.out.println("Total time = " +totalTime);

	}

}
