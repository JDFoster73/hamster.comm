package test.hamster.emul.netchannel;

import org.slf4j.Logger;

import hamster.comm.buffer.block.SequentialMessageBlockReader;
import hamster.comm.buffer.block.SequentialMessageBlockWriter;

/**
 * <p>An instance of this class acts as the producer and consumer of one endpoint of a bi-directional connection.  Test messages can be generated using
 * the {@link TestCommPacketSequential#writeMessageBlock(SequentialMessageBlockWriter)} and sent to the other endpoint.  The other endpoint is expecting
 * test packets in sequence which it can detect by reading the appropriate field in the test packet.
 * <p>The other endpoint will also have a {@link TestCommPacketSequential} instance which it will use to generate test packets.  When this instance receives
 * test packets then it can check the given test packet is correct by calling the {@link TestCommPacketSequential#readMessageBlock(SequentialBufferReader, int)} 
 * method.  A runtime exception will be thrown if the contents are invalid.  It expects these packets to be sent in sequence.</p>
 * 
 * @author jdf19
 *
 */
public class TestCommPacketSequential extends TestCommPacketBase
{
  public TestCommPacketSequential(String sessionID, int minLen, int maxLen, Logger logger)
  {
  super(sessionID, minLen, maxLen, logger);
  }
  
  @Override
  protected void produceTestData(SequentialMessageBlockWriter writer, int numFields)
  {
    //Add the test fields.
    for(int i = 0; i < numFields - 1; i++)
    {
      int vl = i + 1;
      char val = (char) (vl);
      writer.produceChar(val); 
    }

    //Last val is 0xff.
    writer.produceChar((char) 0xff);
  }

  @Override
  protected void checkFields(SequentialMessageBlockReader reader, int numFields, int packetCounter)
  {
    //Check the contents.
    int fld;
    for(int i = 0; i < numFields - 1; i++)
    {
      //Get the data field.
      fld = reader.consumeChar();
      
      //Check it.
      if(fld != (i + 1)) 
      {
        //Spit out buffer.
        reader.rewindToBlockStartPosition();
        //System.out.println(reader.toString());
        throw new IllegalArgumentException("CHECK TEST PACKET FAIL: CONTENT [" + i +"] (" + fld + ") DOES NOT MATCH EXPECTED (" + (i) + ")");
      }
    }

    //Check last.
    if((fld = reader.consumeChar()) != 0xff) throw new IllegalArgumentException("CHECK TEST PACKET FAIL: CONTENT [" + (numFields - 1) +"] (" + fld + ") DOES NOT MATCH EXPECTED (" + (0xff) + ")");
    
    //Still here - trace.
    if(logger.isTraceEnabled()) logger.trace(sessionID + " has processed packet " + packetCounter);
  }

  @Override
  protected int implementationHeaderBytes()
  {
    // TODO Auto-generated method stub
    return 0;
  }

}
