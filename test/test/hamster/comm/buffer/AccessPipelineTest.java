package test.hamster.comm.buffer;

import hamster.comm.buffer.AccessBuffer;
import hamster.comm.buffer.BufferFactory;
import hamster.comm.buffer.PipelineBuffer;
import org.junit.Assert;
import org.junit.Test;

public class AccessPipelineTest
{
  @Test
  public void doTest1()
  {
    PipelineBuffer pb = new PipelineBuffer(BufferFactory.getDefaultBufferFactory());
    AccessBuffer ab = new AccessBuffer(BufferFactory.getDefaultBufferFactory());

    //Create some message data in the pipeline.
    pb.startWriteMessage();
    pb.produceChar((char) 0); //Length
    pb.produceInt(0x12341234);
    pb.produceByte((byte) 0x12);
    pb.produceLong(0x1234123455443322L);

    //Message data finished - set the length.
    pb.produceCharAt(0, (char) pb.messageBlockWritePosition());

    //Finish the message.
    pb.completeWriteMessage();

    //Read the message back, strip the length header and then transfer to the access buffer.
    int i = pb.peekChar(0);
    if(pb.tryStartReadMessage(i))
    {
      //Strip the length.
      pb.consumeChar();

      //Transfer to the access buffer.
      ab.transferFrom(pb);

      //Complete the message.
      pb.completeReadMessage();
    }
    else
    {
      Assert.fail();
    }

    //
    int f1 = ab.readInt(0);
    int f2 = ab.readByte(4);
    long f3 = ab.readLong(5);

    int lst = 0;

    //Init a new message.
    ab.init(10);
  }
}
