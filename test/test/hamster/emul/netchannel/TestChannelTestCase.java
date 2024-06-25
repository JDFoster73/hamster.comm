package test.hamster.emul.netchannel;

import java.io.IOException;
import java.security.SecureRandom;

import org.junit.Test;

import hamster.comm.buffer.block.GeneralPipelineBuffer;
import hamster.comm.buffer.block.SequentialMessageBlockReader;
import hamster.comm.buffer.block.SequentialMessageBlockWriter;
import hamster.comm.buffer.block.itf.ReadableCompletionBlockHandler;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.StandardExpandableBufferFactoryCreator;
import test.hamster.util.log.TestLogUtil;

public class TestChannelTestCase
{

  @Test
  public void test_TestChannelEnd_largeTransfer() throws IOException
  {
    //Create a TestChannel.
    TestChannel tc = new TestChannel(50, 100, null, TestLogUtil.retrieveLogger("GENERAL"));
    
    //Create a buffer and fill it with 100 bytes.
    SecureRandom sr = new SecureRandom();
    GeneralPipelineBuffer pb = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(100), TestLogUtil.retrieveLogger("TEST"));
    pb.produceFromHandler(new WriteBlockHandler()
    {
      
      @Override
      public void writeMessageBlock(SequentialMessageBlockWriter writer)
      {
        //Fill buffer.
        for(int i = 0; i < 25; i++)
        {
          writer.produceInt(sr.nextInt());
        }
      }
    });
        
//        (r) -> {
//      //Fill buffer.
//      for(int i = 0; i < 25; i++)
//      {
//        r.produceInt(sr.nextInt());
//      }
//    }, (r) -> {//Do nothing
//    });
    
    //Drain.
    boolean finished = false;
    TestConsumer tcs = new TestConsumer();
    while(!finished)
    {
      //Drain buffer to test channel if remaining.
      if(pb.hasDataToConsume())
      {
        pb.drainBufferToChannel(tc.getA());
      }

      //Advance.
      tc.advance();
      
      //Drain.
      pb.consumeToHandler(tcs);
      
      //Finished?
      finished = (tcs.bytesIn == 100);
    }
  }
  
  /**
   * <p>Simple test consumer checks that a certain number of bytes have been consumed.
   * 
   * @author jdf19
   *
   */
  private class TestConsumer implements ReadableCompletionBlockHandler
  {
    public int bytesIn = 0;
    
    public GeneralPipelineBuffer recvBuf = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.nonDirectBufferFactory(100), TestLogUtil.retrieveLogger("TEST"));
    
    @Override
    public boolean readMessageBlock(SequentialMessageBlockReader reader)
    {
      return true;  //Always handle this message.       
    }

    @Override
    public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
    {
      // TODO Auto-generated method stub
      return 0;
    }
  }
}
