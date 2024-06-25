package test.hamster.emul.netchannel;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import hamster.comm.buffer.block.SequentialMessageBlockReader;
import hamster.comm.buffer.block.SequentialMessageBlockWriter;
import hamster.comm.buffer.block.itf.ReadAllHandler;

/**
 * <p>An instance of this class acts as the producer and consumer of one endpoint of a bi-directional connection.  Test messages can be generated using
 * the {@link TestCommPacketRandom#writeMessageBlock(SequentialMessageBlockWriter)} and sent to the other endpoint.  The other endpoint is expecting
 * test packets in sequence which it can detect by reading the appropriate field in the test packet.
 * <p>The other endpoint will also have a {@link TestCommPacketRandom} instance which it will use to generate test packets.  When this instance receives
 * test packets then it can check the given test packet is correct by calling the {@link TestCommPacketRandom#readMessageBlock(SequentialBufferReader, int)} 
 * method.  A runtime exception will be thrown if the contents are invalid.  It expects these packets to be sent in sequence.</p>
 * 
 * @author jdf19
 *
 */
public class TestCommPacketRandom extends TestCommPacketBase
{
  private final List<char[]> randomPacketsList = new ArrayList<>();
  
  public TestCommPacketRandom(String sessionID, int minLen, int maxLen, Logger logger)
  {
  super(sessionID, minLen, maxLen, logger);
  }
  
  @Override
  protected void produceTestData(SequentialMessageBlockWriter writer, int numFields)
  {
    //Create a random packet and add to the list.
    char[] cha = getRandomPacketData(numFields);
    randomPacketsList.add(cha);
    
    //Add the test fields.
    for(int i = 0; i < numFields; i++)
    {
      writer.produceChar(cha[i]); 
    }
  }

  @Override
  protected void checkFields(SequentialMessageBlockReader reader, int numFields, int packetCounter)
  {
    //Get the front of the list.
    char[] cha = randomPacketsList.get(packetCounter);
    
    //Check the contents.
    int fld;
    for(int i = 0; i < numFields; i++)
    {
      //Get the data field.
      fld = reader.consumeChar();
      
      //Check it.
      if(fld != cha[i]) 
      {
        //Buffer output.
        
        //Exception.
        throw new IllegalArgumentException("CHECK TEST PACKET FAIL: CONTENT [" + i +"] (" + fld + ") DOES NOT MATCH EXPECTED (" + (i) + ")");
      }
    }
  }

  @Override
  protected int implementationHeaderBytes()
  {
    return 0;
  }

  private char[] getRandomPacketData(int dataLen)
  {
    //Create the packet.
    char[] cha = new char[dataLen];
    
    //Populate with random data.
    for(int i = 0; i < dataLen; i++)
    {
      double rch = (Math.random() * 65535);
      cha[i] = (char) rch;
    }
    
    //Return the new array.
    return cha;
  }
}
