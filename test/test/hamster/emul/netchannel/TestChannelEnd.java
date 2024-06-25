package test.hamster.emul.netchannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.slf4j.Logger;

import hamster.comm.buffer.block.FillableChannelBuffer;
import hamster.comm.server.lst.ReadTransferHandler;

public class TestChannelEnd implements ReadableByteChannel, WritableByteChannel, ReadTransferHandler
{
  private final TestChannelBufferBridge thisToOther;
  
  private final TestChannelBufferBridge otherToThis;
  
  private boolean writableCallback = false;
  
  private final Logger logger;
  
  public TestChannelEnd(TestChannelBufferBridge thisToOther, TestChannelBufferBridge otherToThis, Logger logger)
  {
    super();
    this.thisToOther = thisToOther;
    this.otherToThis = otherToThis;
    
    this.logger = logger;
  }

  @Override
  public boolean isOpen()
  {
    return true;
  }

  @Override
  public void close() throws IOException
  {
  }

  @Override
  public String toString()
  {
    return "S>D" + thisToOther + ", D>S" + otherToThis + ", CB:" + ((writableCallback) ? "ON" : "OFF");
  }

  @Override
  public int read(ByteBuffer dst) throws IOException
  {
    return otherToThis.read(dst);
  }

  @Override
  public int write(ByteBuffer src) throws IOException
  {
    return thisToOther.write(src);
  }

  public boolean has()
  {
    return otherToThis.has();
  }

  public boolean writeCallback()
  {
    return writableCallback;
  }
  
  @Override
  public int transferChannelReadData(FillableChannelBuffer reader)
  {
    try
    {
      return reader.fromChannel(this);
    }
    catch (IOException e)
    {
      //We don't throw this in testing.
      throw new RuntimeException();
    }
  }

  public void setWritableCallback(boolean notificationOn)
  {
    writableCallback = notificationOn;
  }

  @Override
  public int transferChannelReadData(FillableChannelBuffer reader, int maxBytes)
  {
    try
    {
      return reader.fromChannel(this, maxBytes);
    }
    catch (IOException e)
    {
      //We don't throw this in testing.
      throw new RuntimeException();
    }
  }
}
