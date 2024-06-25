package test.hamster.comm.buffers.block;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.logging.Level;

import org.junit.Test;
import org.slf4j.Logger;

import hamster.comm.buffer.block.GeneralPipelineBuffer;
import hamster.comm.buffer.block.SequentialMessageBlockReader;
import hamster.comm.buffer.block.SequentialMessageBlockWriter;
import hamster.comm.buffer.block.itf.MessageBlockTransferProvider;
import hamster.comm.buffer.block.itf.ReadableCompletionBlockHandler;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.StandardExpandableBufferFactoryCreator;
import hamster.comm.logging.LogCreator;
import hamster.comm.logging.LogRetriever.LOG_TYPE;
import test.hamster.emul.netchannel.TestChannel;
import test.hamster.emul.netchannel.TestCommPacketBase;
import test.hamster.emul.netchannel.TestCommPacketRandom;
import test.hamster.emul.netchannel.TransferDecision;
import test.hamster.util.log.TestLogUtil;

public class GeneralPipelineBufferTest
{
  /**
   * <p>
   * Simply generate some test data, produce it to the buffer and then consume it.
   */
  @Test
  public void test_simpleProduceConsume()
  {
    // Create pipeline buffer.
    GeneralPipelineBuffer pb = new GeneralPipelineBuffer(
        StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(200), TestLogUtil.retrieveLogger("TEST"));

    // Get some test values.
    int[] vals1 = TestCommPacketBase.getTestVals(2, 20);
    int[] vals2 = TestCommPacketBase.getTestVals(2, 20);

    // Produce the messages.
    pb.produceFromHandler(new DataProd(vals1));
    pb.produceFromHandler(new DataProd(vals2));

    // Consume the message and check the data.
    assertTrue(pb.consumeToHandler(new DataCheck(vals1)));
    assertTrue(pb.consumeToHandler(new DataCheck(vals2)));
  }

  /**
   * <p>
   * Simply generate some test data, transfer it to another buffer and the consume.
   */
  @Test
  public void test_simpleProduceTransferConsume()
  {
    // Create pipeline buffer.
    GeneralPipelineBuffer pbSrc = new GeneralPipelineBuffer(
        StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(200), TestLogUtil.retrieveLogger("TEST"));

    GeneralPipelineBuffer pbDst = new GeneralPipelineBuffer(
        StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(200), TestLogUtil.retrieveLogger("TEST"));

    // Get some test values.
    int[] vals1 = TestCommPacketBase.getTestVals(2, 20);

    // Produce the messages.
    pbSrc.produceFromHandler(new DataProd(vals1));

    //Transfer block from source to dest.
    assertTrue(pbSrc.consumeToHandler(new DataTransfer(pbDst)));
    
    // Consume the message and check the data.
    assertTrue(pbDst.consumeToHandler(new DataCheck(vals1)));
  }

  //16/04/2021 Took this out.  Illegal state generated becuase the operation of the buffer has changed since the test was written.
  //We can't do nested consume-in-produce or produce-in-consume any more; to process blocks-within-blocks, we use the writer/reader
  //block processing methods instead (processReadBlockHandler/processReadAllkHandler/addWritableBlock).
//  /**
//   * <p>
//   * Produce a message. When consuming the message, produce another message. This
//   * behaviour can be seen when handling incoming messages which trigger the
//   * generation of outgoing messages which are written at the end of the same
//   * buffer we're reading from.
//   */
//  @Test
//  public void test_prodThenConsumeProduce()
//  {
//    // Create pipeline buffer.
//    GeneralPipelineBuffer pb = new GeneralPipelineBuffer(
//        StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(200), TestLogUtil.retrieveLogger("TEST"));
//
//    // Get some test values.
//    int[] vals1 = TestCommPacketBase.getTestVals(2, 20);
//    int[] vals2 = TestCommPacketBase.getTestVals(2, 20);
//
//    // Produce the message.
//    pb.produceFromHandler(new DataProd(vals1));
//
//    // Consume the message and check the data.
//    assertTrue(pb.consumeToHandler(new DataCheckAndProduce(vals1, vals2, pb, true)));
//
//    // Consume the produced data.
//    assertTrue(pb.consumeToHandler(new DataCheck(vals2)));
//  }

  //16/04/2021 Took this out.  Illegal state generated becuase the operation of the buffer has changed since the test was written.
  //We can't do nested consume-in-produce or produce-in-consume any more; to process blocks-within-blocks, we use the writer/reader
  //block processing methods instead (processReadBlockHandler/processReadAllkHandler/addWritableBlock).
//  /**
//   * <p>
//   * Test producing nested messages and then using nested consumption to verify
//   * them.
//   */
//  @Test
//  public void test_nestedProduceNestedConsume()
//  {
//    // Create pipeline buffer.
//    GeneralPipelineBuffer pb = new GeneralPipelineBuffer(
//        StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(200), TestLogUtil.retrieveLogger("TEST"));
//
//    // Get some test values.
//    int[] vals1 = TestCommPacketBase.getTestVals(5, 25);
//    int[] vals2 = TestCommPacketBase.getTestVals(10, 30);
//
//    // Produce a nested message.
//    NestProdL1 nestProdL1 = new NestProdL1(pb, new NestedProtocolProducer(vals1));
//    pb.produceFromHandler(nestProdL1);
//
//    // Produce a nested message.
//    NestProdL1 nestProdL1_2 = new NestProdL1(pb, new NestedProtocolProducer(vals2));
//    pb.produceFromHandler(nestProdL1_2);
//
//    // Consume nested message.
//    NestConsL1 nestConsL1 = new NestConsL1(pb, new DataCheck(vals1));
//    assertTrue(pb.consumeToHandler(nestConsL1));
//
//    // Consume nested message.
//    NestConsL1 nestConsL1_2 = new NestConsL1(pb, new DataCheck(vals2));
//    assertTrue(pb.consumeToHandler(nestConsL1_2));
//  }

  /**
   * <p>
   * Test producing nested messages and then using nested consumption to verify
   * them.  Use the simple WriteBlockHandler interface.
   */
  @Test
  public void test_nestedProduceSimpleWriteBlockNestedConsume()
  {
    // Create pipeline buffer.
    GeneralPipelineBuffer pb = new GeneralPipelineBuffer(
        StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(200), TestLogUtil.retrieveLogger("TEST"));

    // Get some test values.
    int[] vals1 = TestCommPacketBase.getTestVals(5, 25);

    // Produce a nested message.
    NestedWriterSimpleBlockCallProtocolProducer nestProdL1 = new NestedWriterSimpleBlockCallProtocolProducer(new NestedWriteBlockProtocolProducer(vals1));
    pb.produceFromHandler(nestProdL1);

    // Consume nested message.
    NestConsL1 nestConsL1 = new NestConsL1(pb, new DataCheck(vals1));
    assertTrue(pb.consumeToHandler(nestConsL1));
  }

  /**
   * <p>
   * Test producing nested messages and then using nested consumption to verify
   * them.  Use the {@link SequentialMessageBlockWriter} to nest the messages, 
   * rather than the {@link GeneralPipelineBuffer#produceFromHandler(WriteBlockHandler)} method
   * in a recursive way.
   */
  @Test
  public void test_writerCallNestedProduceNestedConsume()
  {
    // Create pipeline buffer.
    GeneralPipelineBuffer pb = new GeneralPipelineBuffer(
        StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(200), TestLogUtil.retrieveLogger("TEST"));

    // Get some test values.
    int[] vals1 = TestCommPacketBase.getTestVals(5, 25);
    int[] vals2 = TestCommPacketBase.getTestVals(10, 30);

    // Produce a nested message.
    NestedWriterCallProtocolProducer nestProdL1 = new NestedWriterCallProtocolProducer(new NestedProtocolProducer(vals1));
    pb.produceFromHandler(nestProdL1);

    // Produce a nested message.
    NestedWriterCallProtocolProducer nestProdL1_2 = new NestedWriterCallProtocolProducer(new NestedProtocolProducer(vals2));
    pb.produceFromHandler(nestProdL1_2);

    // Consume nested message.
    NestedReaderCallProtocolConsumer nestConsL1 = new NestedReaderCallProtocolConsumer(new DataCheck(vals1));
    assertTrue(pb.consumeToHandler(nestConsL1));

    // Consume nested message.
    NestedReaderCallProtocolConsumer nestConsL1_2 = new NestedReaderCallProtocolConsumer(new DataCheck(vals2));
    assertTrue(pb.consumeToHandler(nestConsL1_2));
  }

  //16/04/2021 Took this out.  Illegal state generated becuase the operation of the buffer has changed since the test was written.
  //We can't do nested consume-in-produce or produce-in-consume any more; to process blocks-within-blocks, we use the writer/reader
  //block processing methods instead (processReadBlockHandler/processReadAllkHandler/addWritableBlock).
//  /**
//   * <p>
//   * Produce a message. When consuming the message, produce another message. This
//   * behaviour can be seen when handling incoming messages which trigger the
//   * generation of outgoing messages which are written at the end of the same
//   * buffer we're reading from.
//   * <p>
//   * Force a rebuffer operation to make sure everything still works afterwards.
//   */
//  @Test
//  public void test_prodThenConsumeProduceWithRebuffer()
//  {
//    // Create pipeline buffer.
//    GeneralPipelineBuffer pb = new GeneralPipelineBuffer(
//        StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(10), TestLogUtil.retrieveLogger("TEST"));
//
//    // Get some test values.
//    int[] vals1 = TestCommPacketBase.getTestVals(5, 20);
//    int[] vals2 = TestCommPacketBase.getTestVals(5, 20);
//
//    // Produce the message.
//    pb.produceFromHandler(new DataProd(vals1));
//
//    // Consume the message and check the data.
//    assertTrue(pb.consumeToHandler(new DataCheckAndProduce(vals1, vals2, pb, true)));
//
//    // Consume the produced data.
//    assertTrue(pb.consumeToHandler(new DataCheck(vals2)));
//  }

  //16/04/2021 Took this out.  Illegal state generated becuase the operation of the buffer has changed since the test was written.
  //We can't do nested consume-in-produce or produce-in-consume any more; to process blocks-within-blocks, we use the writer/reader
  //block processing methods instead (processReadBlockHandler/processReadAllkHandler/addWritableBlock).
//  /**
//   * <p>
//   * Test for correct framing when the whole message is not consumed. Throw in a
//   * rebuffer.
//   */
//  @Test
//  public void test_testIncompleteConsume()
//  {
//    // Create pipeline buffer.
//    GeneralPipelineBuffer pb = new GeneralPipelineBuffer(
//        StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(10), TestLogUtil.retrieveLogger("TEST"));
//
//    // Create length check and message postprocessor instances.
//    // Get some test values.
//    int[] vals1 = TestCommPacketBase.getTestVals(5, 20);
//    int[] vals2 = TestCommPacketBase.getTestVals(5, 20);
//
//    // Produce the message.
//    pb.produceFromHandler(new DataProd(vals1));
//
//    // Consume the message and check the data.
//    assertTrue(pb.consumeToHandler(new DataCheckAndProduce(vals1, vals2, pb, false)));
//
//    // Consume the produced data.
//    assertTrue(pb.consumeToHandler(new DataCheck(vals2)));
//  }

  /**
   * <p>Test filling from a channel.
   * @throws IOException 
   */
  @Test
  public void test_testFillDrain() throws IOException
  {
    //Create pipeline buffer.
    GeneralPipelineBuffer pbSrc = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(4000), TestLogUtil.retrieveLogger("TEST"));
    GeneralPipelineBuffer pbDst = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(4000), TestLogUtil.retrieveLogger("TEST"));
    
    LogCreator lc = new LogCreator(0);
    
    //Get test vals.
    //int[] testVals = TestCommPacketManager.getTestVals(10, 20);
    Logger retrieveLogger = lc.retrieveLogger(LOG_TYPE.INTERNAL, "test", Level.ALL);
    TestCommPacketBase tcpm = new TestCommPacketRandom("A", 1000, 2000, retrieveLogger);
    //Produce message data.
    pbSrc.produceFromHandler(tcpm);
   
    //Create a test channel.
    TestChannel tc = new TestChannel(200, 100, new AllAvailableDecision(), retrieveLogger);
    
    //Advance the channel.
    while(pbSrc.hasDataToConsume() || tc.inTransit())
    {
      //Drain to channel.
      pbSrc.drainBufferToChannel(tc.getA());
      
      //Transfer data from A to B.
      tc.advance();

      //Fill from channel.
      pbDst.fromChannel(tc.getB());
    }
    
    //Consume and test.
    assertTrue(pbDst.consumeToHandler(tcpm));//pbDst.consumeToHandler(new DataCheck(testVals), new LenCheck()));
  }

  @Test
  public void test_testConsumeDecline() throws IOException
  {
    //Create pipeline buffer.
    GeneralPipelineBuffer pbSrc = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(400), TestLogUtil.retrieveLogger("TEST"));
    
    //Test messages.
    TestCommPacketRandom testData = new TestCommPacketRandom("A>B TESTER", 80, 100, TestLogUtil.retrieveLogger("TEST"));
    
    //Fill buffer with some test data.
    pbSrc.produceFromHandler(testData);
    pbSrc.produceFromHandler(testData);
    pbSrc.produceFromHandler(testData);
    
    //Attempt to consume some data but return false so the message consumption operation is rewound.
    pbSrc.consumeToHandler(new ReadableCompletionBlockHandler()
    {
      
      @Override
      public boolean readMessageBlock(SequentialMessageBlockReader reader)
      {
        // Do the data test to consume the entire message.
        testData.readMessageBlock(reader);
        //Roll back the message counter.
        testData.rollbackCheckCounter();
        
        // Return false - don't process this message.
        return false;
      }
      
      @Override
      public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
      {
        //Return the message length for the next test message.
        return testData.messageBlockCompleteLength(dataReader);
      }
    });

    //Produce another message.
    pbSrc.produceFromHandler(testData);
    
    //We should now have 4 unconsumed messages and should be able to test them all.
    for(int i = 0; i < 4; i++) 
    {
      pbSrc.consumeToHandler(testData);
    }
  }
  
  /**
   * <p>
   * Test draining to a limit to a channel.
   * 
   * @throws IOException
   */
//  @Test
//  public void test_testDrainWithLimit() throws IOException
//  {
//    //Create pipeline buffer.
//    GeneralPipelineBuffer pbSrc = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(100));
//    GeneralPipelineBuffer pbDst = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(100));
//    
//    //Get test vals.
//    int[] testVals1 = getTestVals(10, 20);
//    int[] testVals2 = getTestVals(10, 20);
//    
//    //Produce message data.
//    DataProd dataProd1 = new DataProd(testVals1);
//    DataProd dataProd2 = new DataProd(testVals2);
//    PostComplete postComplete = new PostComplete();
//    pbSrc.produceFromHandler(dataProd1, postComplete);
//    pbSrc.produceFromHandler(dataProd2, postComplete);
//   
//    //Create a test channel.
//    TestChannel tc = new TestChannel(200, new AllAvailableDecision());
//    
//    //Drain to channel.
//    pbSrc.drainBufferToChannel(tc.getA(), dataProd1.messageLen());
//    tc.advance();
//    
//    //Fill from channel.
//    pbDst.fromChannel(tc.getB());
//    
//    //Consume and test.
//    assertTrue(pbDst.consumeToHandler(new DataCheck(testVals1), new LenCheck()));
//
//    //Assert that dest buffer is empty.
//    assertTrue(!pbDst.hasDataToSend());
//    
//    //Drain to channel.
//    pbSrc.drainBufferToChannel(tc.getA(), dataProd2.messageLen());
//    tc.advance();
//    
//    //Fill from channel.
//    pbDst.fromChannel(tc.getB());
//    
//    //Consume and test.
//    assertTrue(pbDst.consumeToHandler(new DataCheck(testVals2), new LenCheck()));
//
//    //Assert that dest buffer is empty.
//    assertTrue(!pbDst.hasDataToSend());    
//  }

  /**
   * <p>
   * Produce some data and consume too much, causing a buffer underflow. Check
   * that we can consume it properly after this, i.e. it is in a coherent state
   * and can be recovered in good order.
   */
  @Test
  public void test_testBufferStateCorrectAfterException()
  {
    // Create a new buffer.
    GeneralPipelineBuffer pbSrc = new GeneralPipelineBuffer(
        StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(100), TestLogUtil.retrieveLogger("TEST"));

    // Produce test data.
    int[] testVals1 = TestCommPacketBase.getTestVals(10, 20);
    int[] badVals1 = TestCommPacketBase.getBadTestVals(testVals1);

    DataProd dataProd1 = new DataProd(testVals1);
    pbSrc.produceFromHandler(dataProd1);

    // Bad consume operation.
    try
    {
      assertTrue(pbSrc.consumeToHandler(new DataCheck(badVals1, false)));
      // Fail if still here - above method MUST throw an exception.
      fail("Should have thrown an exception.");
    }
    catch (Throwable t)
    {
      // Good - exception thrown.
    }

    // Good consume operation.
    assertTrue(pbSrc.consumeToHandler(new DataCheck(testVals1)));
  }

//  /**
//   * <p>Test that data can be drained to a buffer correctly.
//   * @throws IOException 
//   */
//  @Test
//  public void test_drainToBuffer() throws IOException
//  {
//    //Create a new buffers.
//    GeneralPipelineBuffer pbSrc = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(100));
//    GeneralPipelineBuffer pbDest = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(100));
//    
//    //Produce test data.
//    int[] testVals1 = getTestVals(50, 100);
//    
//    //Create a test channel.
//    TestChannel tc = new TestChannel(50, new AllAvailableDecision());
//    
//    //Drain to the channel while there are data to check.
//    int[] checkVals = new int[testVals1.length];
//     
//    boolean finished = false;
//
//    //Write data into testSend.
//    pbSrc.produceFromHandler((r) -> {
//      for(int i : testVals1)
//      {
//        r.produceInt(i);
//      }
//    });
//    
//    //Consume checker.
//    ConsumeChecker cc = new ConsumeChecker(checkVals);
//    
//    int loopCount = 0;
//    while(!finished)
//    {
//      //Drain data to the test channel.
//      pbSrc.drainBufferToChannel(tc.getA());
//      //Advance.
//      tc.advance();
//      
//      //Get B end.  If 0 bytes read then we have a problem.
//      assertTrue(pbDest.fromChannel(tc.getB()) > 0);
//      
//      while(pbDest.consumeToHandler(cc, (r) -> {
//        return r.bytesInBlock() - (r.bytesInBlock() % 4);
//      }));
//
//      finished = cc.getCheckedFields() == testVals1.length;
//      
//      //Advance loop counter.
//      loopCount++;
//      
//      //Don't loop indefinitely if there is a problem.
//      if(loopCount > 20) throw new IllegalStateException();
//    }
//  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // UTILITY METHODS AND CLASSES
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//  private class ConsumeChecker implements ReadableCompletionBlockHandler
//  {
//    private final int[] checkVals;
//
//    private int checkedFields = 0;
//
//    public ConsumeChecker(int[] checkVals)
//    {
//      this.checkVals = checkVals;
//    }
//
//    @Override
//    public void readMessageBlock(SequentialMessageBlockReader reader)
//    {
//      int remaining;
//      while ((remaining = reader.bytesRemainingInBlock()) >= 4)
//      {
//        checkVals[checkedFields++] = reader.consumeInt();
//      }
//    }
//
//    int getCheckedFields()
//    {
//      return checkedFields;
//    }
//
//    @Override
//    public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
//    {
//      // Enough data to check len field?
//      if (dataReader.bytesInBlock() < 2) return -1;
//      // Check len.
//      int len;
//      if ((len = dataReader.consumeChar()) <= dataReader.bytesInBlock()) return len;
//      // Not enough.
//      return -1;
//    }
//  }

//  /**
//   * <p>
//   * Generate test values.
//   * 
//   * @return
//   */
//  private int[] getTestVals(int minFields, int range)
//  {
//    // Random number of fields.
//    int numFields = ((int) (Math.random() * range));
//    // Number of fields accounting for the requested minimum.
//    numFields = Math.max(numFields, minFields);
//
//    // Create some message data.
//    int[] vals = new int[numFields];
//    SecureRandom sr = new SecureRandom();
//    for (int i = 0; i < vals.length; i++)
//    {
//      vals[i] = sr.nextInt();
//    }
//
//    return vals;
//  }
//
//  /**
//   * <p>
//   * Generate test values.
//   * 
//   * @return
//   */
//  private int[] getBadTestVals(int[] testVals)
//  {
//    // Random number of fields.
//    int numFields = testVals.length + 1;
//    int[] badVals = new int[numFields];
//
//    System.arraycopy(testVals, 0, badVals, 0, testVals.length);
//    badVals[numFields - 1] = 1234;
//
//    return badVals;
//  }

  /**
   * <p>
   * Test class will take the given int[] array and produce the values to a
   * message block with a 2-byte unsigned message byte length header.
   * 
   * @author jdf19
   *
   */
  private class DataProd implements WriteBlockHandler
  {
    /**
     * <p>
     * Test values to produce.
     */
    private final int[] vals;

    /**
     * <p>
     * Construct with test values.
     * 
     * @param vals
     */
    public DataProd(int[] vals)
    {
      this.vals = vals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMessageBlock(SequentialMessageBlockWriter w)
    {
      // Len field, filled in by postprocessor.
      w.produceChar((char) 0);
      // Test data.
      for (int i : vals)
        w.produceInt(i);

      w.produceCharAt((char) w.blockMessageBytes(), 0);
    }
  }

  /**
   * <p>
   * Check data value message, with the 2-byte unsigned message length header.
   * 
   * @author jdf19
   *
   */
  private class DataCheck implements ReadableCompletionBlockHandler
  {
    /**
     * <p>
     * Test values to check.
     */
    private final int[] vals;

    /**
     * <p>
     * Check the length field against the amount of message data in the block.
     */
    private boolean checkLenField;

    /**
     * <p>
     * Construct with test values.
     * 
     * @param vals
     */
    public DataCheck(int[] vals)
    {
      this.vals = vals;
      this.checkLenField = true;
    }

    /**
     * <p>
     * Construct with test values.
     * 
     * @param vals
     */
    public DataCheck(int[] vals, boolean checkLenField)
    {
      this.vals = vals;
      this.checkLenField = checkLenField;
    }

    /**
     * Skip length field check.  We may be reading an unbounded block from a SequentialMessageBlockReader.processReadBlockHandler()
     * call, in which case the reader may not contain the same number of bytes in bytesInBlock() as the reported block length.
     * We will have to rely on data integrity and no buffer underflow exception.
     * {@inheritDoc}
     */
    @Override
    public boolean readMessageBlock(SequentialMessageBlockReader r)
    {
      int ln = r.consumeChar();
//      if (checkLenField)
//      {
//        assertTrue(ln == r.bytesInBlock());
//      }

      // Check data.
      for (int i : vals)
      {
        assertTrue(r.consumeInt() == i);
      }

      return true;  //Always handle this message.
    }

    @Override
    public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
    {
      // Enough data to check len field?
      if (dataReader.bytesInBlock() < 2) return -1;
      // Check len.
      int len;
      if ((len = dataReader.consumeChar()) <= dataReader.bytesInBlock()) return len;
      // Not enough.
      return -1;
    }
  }

  /**
   * <p>
   * Transfer data from source reader to dest writer.
   * 
   * @author jdf19
   *
   */
  private class DataTransfer implements ReadableCompletionBlockHandler 
  {
    private MessageBlockTransferProvider txReader;
    
    public DataTransfer(MessageBlockTransferProvider pbDst)
    {
      txReader = pbDst;
    }

    @Override
    public boolean readMessageBlock(SequentialMessageBlockReader reader)
    {
      txReader.transferFromReader(reader);
      return true;  //Always handle this message.
    }

    @Override
    public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
    {
      // Enough data to check len field?
      if (dataReader.bytesInBlock() < 2) return -1;
      // Check len.
      int len;
      if ((len = dataReader.consumeChar()) <= dataReader.bytesInBlock()) return len;
      // Not enough.
      return -1;
    }
  }

  /**
   * <p>
   * Check data value message, with the 2-byte unsigned message length header.
   * 
   * @author jdf19
   *
   */
  private class DataCheckAndProduce extends DataProd implements ReadableCompletionBlockHandler
  {
    /**
     * <p>
     * Test values to check.
     */
    private final int[] checkVals;

    /**
     * <p>
     * Pipeline buffer instance to produce to.
     */
    private final GeneralPipelineBuffer prodBuf;

    /**
     * <p>
     */
    private final boolean completeConsume;

    /**
     * <p>
     * Construct with test values.
     * 
     * @param checkVals
     */
    public DataCheckAndProduce(int[] checkVals, int[] prodVals, GeneralPipelineBuffer pbuf, boolean completeConsume)
    {
      super(prodVals);
      this.checkVals = checkVals;
      this.prodBuf = pbuf;
      this.completeConsume = completeConsume;
    }

    @Override
    public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
    {
      // Enough data to check len field?
      if (dataReader.bytesInBlock() < 2) return -1;
      // Check len.
      int len;
      if ((len = dataReader.consumeChar()) <= dataReader.bytesInBlock()) return len;
      // Not enough.
      return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean readMessageBlock(SequentialMessageBlockReader r)
    {
      // Skip length field.
      int ln = r.consumeChar();

      // Number of fields to consume.
      int fieldsToConsume = ((ln - 2) / 4);

      // Adjust fields to consume if not complete consume flag is set.
      if (!completeConsume)
      {
        fieldsToConsume = (int) (Math.random() * fieldsToConsume);
      }

      // Check data.
      for (int i = 0; i < fieldsToConsume; i++)
      {
        assertTrue(r.consumeInt() == checkVals[i]);
      }

      prodBuf.produceFromHandler(this);
      return true;  //Always handle this message.
    }
  }

  
  /**
   * <p>
   * A {@link TransferDecision} implementation which simply requests all available
   * data to be transferred.
   * 
   * @author jdf19
   *
   */
  private class AllAvailableDecision implements TransferDecision
  {
    /**
     * {@inheritDoc}
     */
    @Override
    public int bytesToTransfer(int advanceWithNoTransfer, int recvBufferSize, int sendBufferSize)
    {
      return Math.min(recvBufferSize, sendBufferSize);
    }

  }

/////////////////////////////////////////////////////////////////////////////////////////////////
//NESTED TEST CLASSES
/////////////////////////////////////////////////////////////////////////////////////////////////
  class NestProdL1 implements WriteBlockHandler
  {
    private final GeneralPipelineBuffer sendBuffer;

    private final NestedProtocolProducer npro;

    public NestProdL1(GeneralPipelineBuffer sendBuffer, NestedProtocolProducer npro)
    {
      this.npro = npro;
      this.sendBuffer = sendBuffer;
    }

    @Override
    public void writeMessageBlock(SequentialMessageBlockWriter writer)
    {
// Reserve length field.
      writer.produceChar((char) 0);

// Write the inner protocol message.
      sendBuffer.produceFromHandler(npro);

   // First field is the length.
      writer.produceCharAt((char) writer.blockMessageBytes(), 0);
    }
  }

  class NestedProtocolProducer implements WriteBlockHandler
  {
    private final int[] vals;

    public NestedProtocolProducer(int[] vals)
    {
      super();
      this.vals = vals;
    }

    @Override
    public void writeMessageBlock(SequentialMessageBlockWriter writer)
    {
      // Write the message data with 2-byte length prefix.
      writer.advance(2);

      // Write the vals.
      for (int i : vals)
        writer.produceInt(i);

      // First field is the length.
      writer.produceCharAt((char) writer.blockMessageBytes(), 0);
    }
  }

  class NestConsL1 implements ReadableCompletionBlockHandler
  {
    private final DataCheck dataCheck;

    private final GeneralPipelineBuffer pb;

    public NestConsL1(GeneralPipelineBuffer pb, DataCheck dc)
    {
      this.dataCheck = dc;
      this.pb = pb;
    }

    @Override
    public boolean readMessageBlock(SequentialMessageBlockReader reader)
    {
      // Read len block.
      reader.skip(2);

      // Consume data check.
      pb.consumeToHandler(dataCheck);

      return true;  //Always handle this message.
    }

    @Override
    public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
    {
      // Check the first 2-byte field.
      if (dataReader.bytesInBlock() < 2) return -1;
      int len;
      return ((len = (dataReader.consumeChar())) <= dataReader.bytesInBlock()) ? len : -1;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  //NESTED WRITER-CALL TEST CLASSES
  /////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * <p>Test operation of nested produce via the {@link SequentialMessageBlockWriter}'s {@link SequentialMessageBlockWriter#addWritableCompletionBlockHandler(WriteBlockHandler)}
   * method.
   * 
   * @author jdf19
   *
   */
  class NestedWriterCallProtocolProducer implements WriteBlockHandler
  {
    private final WriteBlockHandler nestedVal;

    public NestedWriterCallProtocolProducer(WriteBlockHandler nestedVal)
    {
      super();
      this.nestedVal = nestedVal;
    }

    @Override
    public void writeMessageBlock(SequentialMessageBlockWriter writer)
    {
      // Write the message data with 2-byte length prefix.
      writer.advance(2);

      //Write the nested message data.
      writer.addWritableBlock(nestedVal);
      
      // First field is the length.
      writer.produceCharAt((char) writer.blockMessageBytes(), 0);      
    }
  }
  
  class NestedReaderCallProtocolConsumer implements ReadableCompletionBlockHandler
  {
    private final ReadableCompletionBlockHandler dataCheck;

    public NestedReaderCallProtocolConsumer(ReadableCompletionBlockHandler dc)
    {
      this.dataCheck = dc;
    }

    @Override
    public boolean readMessageBlock(SequentialMessageBlockReader reader)
    {
      // Read len block.
      reader.skip(2);

      // Consume data check.
      reader.processReadBlockHandler(dataCheck);

      return true;  //Always handle this message.
    }

    @Override
    public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
    {
      // Check the first 2-byte field.
      if (dataReader.bytesInBlock() < 2) return -1;
      int len;
      return ((len = (dataReader.consumeChar())) <= dataReader.bytesInBlock()) ? len : -1;
    }
  }

  class NestedWriterSimpleBlockCallProtocolProducer implements WriteBlockHandler
  {
    private final WriteBlockHandler nestedVal;

    public NestedWriterSimpleBlockCallProtocolProducer(WriteBlockHandler nestedVal)
    {
      super();
      this.nestedVal = nestedVal;
    }

    @Override
    public void writeMessageBlock(SequentialMessageBlockWriter writer)
    {
      // Write the message data with 2-byte length prefix.
      writer.advance(2);

      //Write the nested message data.
      writer.addWritableBlock(nestedVal);

      // First field is the length.
      writer.produceCharAt((char) writer.blockMessageBytes(), 0);
    }
  }

  class NestedWriteBlockProtocolProducer implements WriteBlockHandler
  {
    private final int[] vals;

    public NestedWriteBlockProtocolProducer(int[] vals)
    {
      super();
      this.vals = vals;
    }

    @Override
    public void writeMessageBlock(SequentialMessageBlockWriter writer)
    {
      // Write the message data with 2-byte length prefix.
      writer.advance(2);

      // Write the vals.
      for (int i : vals)
        writer.produceInt(i);

      // First field is the length.
      writer.produceCharAt((char) writer.blockMessageBytes(), 0);
    }
  }
}
