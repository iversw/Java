import spread.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.print.event.PrintEvent;

class Test {
//linux.ifi.uio.no 4269
  Test(){}

  public static void main(String[] args) throws SpreadException, UnknownHostException, ConnectException {
    try{
      System.out.println("Getting connection..");
      SpreadConnection connection = new SpreadConnection();

      connection.connect(InetAddress.getByName("127.0.0.1"),
      4803, "iver_daemon", false, false);
      System.out.println("Connection made!\n");


      SpreadGroup group = new SpreadGroup();
      group.join(connection, "testgroup");
      System.out.println("Joined group");

      SpreadMessage message = new SpreadMessage();
      System.out.println("Creating message..");
      short type = 1;
      message.setType(type);
      message.digest("Pung");
      message.addGroup("testgroup");
      message.setReliable();

      System.out.println("Sending message..");
      connection.multicast(message);

      // SpreadMessage message2 = new SpreadMessage();
      // message2.setType(type);
      // message2.digest("Pikk");
      // message2.addGroup("testgroup");
      // message2.setReliable();
      // connection.multicast(message2);

      System.out.println("Getting response..");
      SpreadMessage response = connection.receive();
      SpreadMessage response2 = connection.receive();
      if(response.isRegular()){
        System.out.println("New message from " + message.getSender());
        System.out.println(message.getDigest().get(0));
      }
      else{
        System.out.println("New membership message" +
        message.getMembershipInfo().getGroup());
      }
      //System.out.println(response2.getDigest().get(0));

    } catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }

  }
}
