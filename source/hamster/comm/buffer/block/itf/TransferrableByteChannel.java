package hamster.comm.buffer.block.itf;

import java.nio.ByteBuffer;

public interface TransferrableByteChannel
{
  public int write(ByteBuffer src);
}
