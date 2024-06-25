package test.hamster.emul.netchannel;

import java.security.SecureRandom;

import org.slf4j.Logger;

import hamster.comm.buffer.block.SequentialMessageBlockReader;
import hamster.comm.buffer.block.SequentialMessageBlockWriter;
import hamster.comm.buffer.block.itf.IncomingDataListener;
import hamster.comm.buffer.block.itf.ReadAllHandler;
import hamster.comm.buffer.block.itf.ReadableCompletionBlockHandler;
import hamster.comm.buffer.block.itf.WriteBlockHandler;

/**
 * <p>An instance of this class acts as the producer and consumer of one endpoint of a bi-directional connection.  Test messages can be generated using
 * the {@link TestCommPacketBase#writeMessageBlock(SequentialMessageBlockWriter)} and sent to the other endpoint.  The other endpoint is expecting
 * test packets in sequence which it can detect by reading the appropriate field in the test packet.
 * <p>The other endpoint will also have a {@link TestCommPacketBase} instance which it will use to generate test packets.  When this instance receives
 * test packets then it can check the given test packet is correct by calling the {@link TestCommPacketBase#readMessageBlock(SequentialBufferReader, int)} 
 * method.  A runtime exception will be thrown if the contents are invalid.  It expects these packets to be sent in sequence.</p>
 * 
 * @author jdf19
 *
 */
public abstract class TestCommPacketBase implements IncomingDataListener, WriteBlockHandler, ReadableCompletionBlockHandler, ReadAllHandler
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
  protected final Logger logger;
  
  /**
   * <p>Session identifier for logging.
   */
  protected final String sessionID;
  
  /**
   * <p>Last generated packet counter id, convenience to avoid calculating wraparound.
   */
  private int lastSentPacketID;
  
  public TestCommPacketBase(String sessionID, int minLen, int maxLen, Logger logger)
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
    //Calculate the length of the packet to send, which is a random number between the min and max len specified.
    double lenRange = maxLen - minLen;
    
    //Get the overall number of fields - at least 2.
    int numFields = ((int) (Math.random() * lenRange)) + minLen;
    
    //Put the length field in the writer.
    writer.produceChar( (char) (numFields) );
    
    //Put the packet counter in the writer.
    int pc = lastSentPacketID = packetCounter;
    writer.produceChar((char)pc);

    //Detect counter wraparound required.
    if(packetCounter++ >= 65000) packetCounter = 0;
    
    //Produce the test data.
    produceTestData(writer, numFields);
    
    //Log outgoing packet.
    //if(logger.isInfoEnabled()) logger.info(ResourceBundle.getBundle("test.hamster.emul.netchannel.strings").getString("tcpm.send.1"), sessionID, pc, numFields);
  }

  /**
   * 
   * @return
   */
  protected abstract int implementationHeaderBytes();
  
  /**
   * <p>Implementation will 
   * @param writer
   */
  protected abstract void produceTestData(SequentialMessageBlockWriter writer, int numFields);
  
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
    check(reader, checkPacketCounter);
    //Roll check counter forward.
    rollforwardCheckCounter();
  }

  /**
   * <p>Check it.
   */
  @Override
  public boolean readMessageBlock(SequentialMessageBlockReader reader)
  {
    check(reader, checkPacketCounter);
    //Roll check counter forward.
    rollforwardCheckCounter();
    return true;
  }

  /**
   * <p>Check it.
   */
  @Override
  public void readMessageAll(SequentialMessageBlockReader reader)
  {
    check(reader, checkPacketCounter);
    //Roll check counter forward.
    rollforwardCheckCounter();
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
    return (dataReader.consumeChar() * 2) + (4 + implementationHeaderBytes());
  }
  
  private void check(SequentialMessageBlockReader reader, int messageID)
  {
    //Read in the packet length.  It must correspond to the packetBytes parameter.
    int fld = reader.consumeChar();
    int cmp = (reader.bytesInBlock() - (4 + implementationHeaderBytes())) / 2;
    if( fld !=  cmp)
    {
      throw new IllegalArgumentException("CHECK TEST PACKET FAIL: PACKET LEN (" + reader.bytesInBlock() + ") DOES NOT MATCH MESSAGE FIELD (" + fld + ")");
    }
    
    //Packet counter.  Must match next expected.
    int pc;
    if( (pc = reader.consumeChar()) != checkPacketCounter)
    {
      throw new IllegalArgumentException("CHECK TEST PACKET FAIL: COUNTER (" + checkPacketCounter + ") DOES NOT MATCH MESSAGE FIELD (" + fld + ")");
    }
    
    //Check implementation
    checkFields(reader, cmp, messageID);
    
    //Check that no further data exist - the message must have been fully consumed.
    if(reader.bytesRemainingInBlock() > 0) throw new IllegalArgumentException("CHECK TEST PACKET FAIL: DATA REMAINING AFTER PROCESSING MESSAGE = " + reader.bytesRemainingInBlock() + " BYTES");

    //Log outgoing packet.
    //if(logger.isInfoEnabled()) logger.info(ResourceBundle.getBundle("test.hamster.emul.netchannel.strings").getString("tcpm.recv.1"), sessionID, pc, cmp);
  }
  
  protected abstract void checkFields(SequentialMessageBlockReader reade, int numFields, int messageCounter);
  
  /**
   * <p>Roll forward the packet counter by 1 position.
   */
  public void rollforwardCheckCounter()
  {
    checkPacketCounter++;
    if(checkPacketCounter >= 65000) checkPacketCounter = 0;
  }
  
  /**
   * <p>Roll back the packet counter by 1 position.
   */
  public void rollbackCheckCounter()
  {
    checkPacketCounter--;
    if(checkPacketCounter < 0) checkPacketCounter = 65000;
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
