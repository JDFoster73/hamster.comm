package test.hamster.emul.netchannel;

import java.security.SecureRandom;
import java.util.ResourceBundle;

import org.slf4j.Logger;

import hamster.comm.buffer.block.SequentialMessageBlockReader;
import hamster.comm.buffer.block.SequentialMessageBlockWriter;
import hamster.comm.buffer.block.itf.IncomingDataListener;
import hamster.comm.buffer.block.itf.ReadableCompletionBlockHandler;
import hamster.comm.buffer.block.itf.WriteBlockHandler;

/**
 * <p>An instance of this class acts as the producer and consumer of one endpoint of a bi-directional connection.  Test messages can be generated using
 * the {@link TestCommPacketSimple#writeMessageBlock(SequentialMessageBlockWriter)} and sent to the other endpoint.  The other endpoint is expecting
 * test packets in sequence which it can detect by reading the appropriate field in the test packet.
 * <p>The other endpoint will also have a {@link TestCommPacketSimple} instance which it will use to generate test packets.  When this instance receives
 * test packets then it can check the given test packet is correct by calling the {@link TestCommPacketSimple#readMessageBlock(SequentialBufferReader, int)} 
 * method.  A runtime exception will be thrown if the contents are invalid.  It expects these packets to be sent in sequence.</p>
 * 
 * @author jdf19
 *
 */
public class TestCommPacketSimple implements IncomingDataListener, WriteBlockHandler, ReadableCompletionBlockHandler
{
  /**
   * <p>Check the packet count field of incoming test packets.  They should start from 0 and increment by 1.  This count checker increments
   * on each test packet reception.  It is compared with the packet count field of the incoming packets and must match.
   */
  private int checkPacketCounter = 0;
  
  /**
   * <p>Maximum length that test packets will be.
   */
  private final int maxLen;
  
  /**
   * <p>Minimum length that test packets will be.
   */
  private final int minLen;
  
  /**
   * <p>Count will be included in every packet.  Used to detect out-of-order delivery and undelivered (missing) packets.
   * It is a 2-byte unsigned integer and will count up 0 ... 64999 and then wrap around back to 0.</p>
   */
  private int packetCounter = 0;
  
  /**
   * <p>Logger to log data.
   */
  private final Logger logger;
  
  /**
   * <p>Session identifier for logging.
   */
  private final String sessionID;
  
  /**
   * <p>Last generated packet counter id, convenience to avoid calculating wraparound.
   */
  private int lastSentPacketID;
  
  public TestCommPacketSimple(String sessionID, int minLen, int maxLen, Logger logger)
  {
    if(maxLen <= minLen) throw new IllegalArgumentException();
    
    this.maxLen = Math.min(Math.max(maxLen, 16), 30000);
    this.minLen = Math.max(minLen, 12);
    
    this.logger = logger;
    
    this.sessionID = sessionID;
  }

  public boolean uptoDate()
  {
    return lastSentPacketID == checkPacketCounter;
  }
  
  @Override
  public void writeMessageBlock(SequentialMessageBlockWriter writer)
  {
    //Put the packet counter in the writer.
    int pc = lastSentPacketID = packetCounter;
    writer.produceChar((char)pc);

    //Detect counter wraparound required.
    if(packetCounter++ >= 65000) packetCounter = 0;
    
    //Log outgoing packet.
    if(logger.isInfoEnabled()) logger.info(ResourceBundle.getBundle("test.hamster.emul.netchannel.strings").getString("tcps.send.1"), sessionID, pc);
  }

  /**
   * <p>Check the contents of the message.  The length should be correct, the packet counter should be in sequence and the content
   * should follow the addition value.</p>
   * 
   * @param rdr
   * @param packetBytes
   */
  @Override
  public void handleIncomingData(SequentialMessageBlockReader reader)
  {
    check(reader);
  }

  /**
   * <p>Check it.
   */
  @Override
  public boolean readMessageBlock(SequentialMessageBlockReader reader)
  {
    check(reader);
    return true;  //Always handle this message.
  }

  /**
   * <p>Read the byte count field which is at the message start.
   */
  @Override
  public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
  {
    //Have we got enough to read the len?
    if(dataReader.bytesInBlock() < 2) return -1;
    
    //Get the byte count.
    return 2;//(dataReader.consumeChar() * 2) + 6;
  }
  
  private void check(SequentialMessageBlockReader reader)
  {
    //Packet counter.  Must match next expected.
    int pc;
    if( (pc = reader.consumeChar()) != checkPacketCounter) throw new IllegalArgumentException("CHECK TEST PACKET FAIL: COUNTER (" + checkPacketCounter + ") DOES NOT MATCH MESSAGE FIELD (" + pc + ")");
    
    //Still here - move packet counter to next expected value.
    checkPacketCounter++;
    if(checkPacketCounter >= 65000) checkPacketCounter = 0;
    
    //Check that no further data exist - the message must have been fully consumed.
    if(reader.bytesRemainingInBlock() > 0) throw new IllegalArgumentException("CHECK TEST PACKET FAIL: DATA REMAINING AFTER PROCESSING MESSAGE = " + reader.bytesRemainingInBlock() + " BYTES");

    //Log outgoing packet.
    if(logger.isInfoEnabled()) logger.info(ResourceBundle.getBundle("test.hamster.emul.netchannel.strings").getString("tcps.recv.1"), sessionID, pc);
  }
  
  /**
   * <p>
   * Generate test values.
   * 
   * @return
   */
  public static int[] getTestVals(int minFields, int range)
  {
    // Random number of fields.
    int numFields = ((int) (Math.random() * range));
    // Number of fields accounting for the requested minimum.
    numFields = Math.max(numFields, minFields);

    // Create some message data.
    int[] vals = new int[numFields];
    SecureRandom sr = new SecureRandom();
    for (int i = 0; i < vals.length; i++)
    {
      vals[i] = sr.nextInt();
    }

    return vals;
  }

  /**
   * <p>
   * Generate test values.
   * 
   * @return
   */
  public static int[] getBadTestVals(int[] testVals)
  {
    // Random number of fields.
    int numFields = testVals.length + 1;
    int[] badVals = new int[numFields];

    System.arraycopy(testVals, 0, badVals, 0, testVals.length);
    badVals[numFields - 1] = 1234;

    return badVals;
  }
}
