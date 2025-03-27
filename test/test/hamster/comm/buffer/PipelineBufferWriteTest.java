package test.hamster.comm.buffer;

import hamster.comm.buffer.PipelineBuffer;
import hamster.comm.buffer.BufferFactory;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteOrder;

public class PipelineBufferWriteTest
{
  @Test
  public void simpleTest()
  {
    //Create the buffer instance.
    PipelineBuffer buf = new PipelineBuffer(BufferFactory.getDefaultBufferFactory());
    //Set to little endian.
    buf.setOrder(ByteOrder.LITTLE_ENDIAN);

    //Produce adhoc data to start.
    buf.produceInt(0x66554433);

    //Now start a message.
    buf.startWriteMessage();

    //Blank length field.
    buf.produceChar((char) 0);

    //Message data.
    buf.produceChar((char) 0x1211);
    buf.produceInt(0x21222324);
    buf.produceByte((byte) 0x31);
    buf.produceInt(0x44433231);
    buf.produceLong(0x5857565554535251L);

    //Set the whole message length.
    buf.produceCharAt(0, (char) 21);

    //Complete the message.
    buf.completeWriteMessage();

    int i = 0;
  }

  @Test
  public void partialMessageConsumeTest()
  {
    //Create the buffer instance.
    PipelineBuffer buf = new PipelineBuffer(BufferFactory.getDefaultBufferFactory());
    //Set to little endian.
    buf.setOrder(ByteOrder.LITTLE_ENDIAN);

    //Load with test data.
    //Start
    buf.produceInt(0x66554433);
    //Message data.
    byte[] by = new byte[]{0x11, 0x12, 0x24, 0x23, 0x22, 0x21, 0x31, 0x41, 0x42, 0x43, 0x44, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58};
    buf.produceFromBytes(by);
    //End
    buf.produceInt(0x61514131);

    //Get start.
    int stx = buf.consumeInt();

    //Consume the fields.
    buf.startReadMessage(by.length);
    char f1 = buf.consumeChar();
    int f2 = buf.consumeInt();
    byte f3 = buf.consumeByte();
    int f4 = buf.consumeInt();

    //Complete the message.
    buf.completeReadMessage();

    //Get end.
    int etx = buf.consumeInt();

    int i = 0;
  }

  @Test
  public void illegalMessageBlockOpTest()
  {
    //Create the buffer instance.
    PipelineBuffer buf = new PipelineBuffer(BufferFactory.getDefaultBufferFactory());
    //Set to little endian.
    buf.setOrder(ByteOrder.LITTLE_ENDIAN);

    //Load with test data.
    //Start
    buf.produceInt(0x66554433);

    buf.startReadMessage(4);

    try
    {
      //End
      buf.produceInt(0x61514131);
    }
    catch (IllegalStateException e)
    {
      //Ok - return.
      return;
    }

    //Fail.
    Assert.fail();
  }
}
