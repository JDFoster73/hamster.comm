package test.hamster.comm.buffer;

import hamster.comm.buffer.block.HeaderSpec;
import hamster.comm.buffer.pipeline.PipelineBuffer;
import hamster.comm.buffer.pipeline.PipelineBufferFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

public class BufferTest
{
  private static PipelineBufferFactory bufferFactory;

  @BeforeClass
  public static void beforeClass()
  {
    bufferFactory = PipelineBufferFactory.getBufferFactory();
  }

  /**
   * Simple test to produce raw byte data from a source, consume to a destination and compare.
   *
   */
  @Test
  public void produceFromBytesTest()
  {
    byte[] source1 = new byte[17];
    fillBufferWithRandomByteData(source1);
    byte[] source2 = new byte[35];
    fillBufferWithRandomByteData(source2);

    //Create pipeline buffer and produce.
    PipelineBuffer testSourceBuffer = new PipelineBuffer(bufferFactory);
    testSourceBuffer.produceFromBytes(source1);
    testSourceBuffer.produceFromBytes(source2);

    //Read the byte array back and compare.
    byte[] target = new byte[17 + 35];
    testSourceBuffer.consumeBytes(target);

    //Compare the arrays.
    Assert.assertTrue(compareSourceTarget(source1, target, 0));
    Assert.assertTrue(compareSourceTarget(source2, target, 17));
  }

  /**
   * Produce data from a callback and check it.
   */
  @Test
  public void produceCallbackDataTest()
  {
    //Create pipeline buffer and produce.
    PipelineBuffer testSourceBuffer = new PipelineBuffer(bufferFactory);

    //Produce callback data.
    testSourceBuffer.produceData(p -> {
      p.produceByte((byte) 0x32);
      p.produceChar('h');
      p.produceDouble(234.432432);
      p.produceFloat(23.434F);
      p.produceInt(234252);
      p.produceLong(0x3ff3cc454L);
      p.produceShort((short) 1234);
    });

    //Consume these data and check the fields.
    testSourceBuffer.consumeData(c -> {
      if(c.consumeByte() != (byte) 0x32) throw new IllegalStateException();
      if(c.consumeChar() != 'h') throw new IllegalStateException();
      if(c.consumeDouble() != 234.432432) throw new IllegalStateException();
      if(c.consumeFloat() != 23.434F) throw new IllegalStateException();
      if(c.consumeInt() != 234252) throw new IllegalStateException();
      if(c.consumeLong() != 0x3ff3cc454L) throw new IllegalStateException();
      if(c.consumeShort() != (short) 1234) throw new IllegalStateException();

      int i = c.numberOfConsumedBytes();

      return 29;
    });

    //Check that the buffer is empty and all data have been consumed.
    Assert.assertTrue(testSourceBuffer.isEmpty());
  }

  @Test
  public void testPipelineTransfer()
  {
    //Create source buffer and populate.
    byte[] source1 = new byte[17];
    fillBufferWithRandomByteData(source1);

    //Create pipeline buffer and produce.
    PipelineBuffer testSourceBuffer = new PipelineBuffer(bufferFactory);
    testSourceBuffer.produceFromBytes(source1);

    //Create target buffer.
    PipelineBuffer testTargetBuffer = new PipelineBuffer(bufferFactory);
    testTargetBuffer.produceDataFromPipelineBuffer(testSourceBuffer);

    //Consume the transferred bytes and compare.
    byte[] target1 = new byte[17];
    testTargetBuffer.consumeBytes(target1);

    //Compare.
    Assert.assertTrue(compareSourceTarget(source1, target1, 0));

    //Check that the buffers are both in the correct state to execute further operations.
    byte[] checkSource1 = new byte[37];
    byte[] checkTarget1 = new byte[37];

    fillBufferWithRandomByteData(checkSource1);

    //Source buffer check.
    testSourceBuffer.produceFromBytes(checkSource1);
    testSourceBuffer.consumeBytes(checkTarget1);
    Assert.assertTrue(compareSourceTarget(checkSource1, checkTarget1, 0));

    //Target buffer check.
    testTargetBuffer.produceFromBytes(checkSource1);
    testTargetBuffer.consumeBytes(checkTarget1);
    Assert.assertTrue(compareSourceTarget(checkSource1, checkTarget1, 0));
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  private void fillBufferWithRandomByteData(byte[] source)
  {
    for(int i = 0; i < source.length; i++)
    {
      //Random number.
      source[i] = (byte) (Math.random() * 255);
    }
  }

  private boolean compareSourceTarget(byte[] source, byte[] target, int startTargetIx)
  {
    for(int i = 0; i < source.length; i++)
    {
      //Compare.
      if(source[i] != target[i + startTargetIx])
      {
        return false;
      }
    }

    //Still here, passed.  Return true.
    return true;
  }
}
