package hamster.comm.buffer.block;

import java.nio.ByteBuffer;

public interface ByteBufferTransferDestinationProvider
{
  public void transferFromSourceBuffer(ByteBuffer bb);

}
