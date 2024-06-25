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

public class RetainingProduceDrainPipelineBufferTest
{
  /**
   * <p>
   * Test that by consuming data and locating the read position, that the same data can be consumed again.
   */
  @Test
  public void test_locateReadPosition()
  {
    Logger lg = TestLogUtil.retrieveLogger("TST");
    TestCommPacketRandom tpcr = new TestCommPacketRandom("TST", 80, 100, lg);
    
    
  }
}
