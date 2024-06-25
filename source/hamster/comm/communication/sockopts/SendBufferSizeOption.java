/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.communication.sockopts;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.NetworkChannel;

/**
 * Network option to set - send buffer size.  Used in a call to SocketChannel.setOption(...) (for example)
 * to pass option requirements.
 * 
 * @author Jim Foster &lt;jdfoster73@gmail.com&gt;
 */
public class SendBufferSizeOption implements SocketChannelOptionAccessor
{
  private int bufferSize;

  /**
   * Create the option with the given send buffer size in bytes. 
   * 
   * @param bufferSizeOptionValue buffer size
   */
  public SendBufferSizeOption(int bufferSizeOptionValue)
  {
    this.bufferSize = bufferSizeOptionValue;
  }  

  /**
   * <p>Query the buffer size, for when this instance has been used to get a buffer size option.
   * 
   * @return the send buffer size.
   */
  public int queryBufferSize()
  {
    return bufferSize;
  }
  
  /**
   * Set the outgoing buffer size of the given socket channel.
   * 
   * @param sc the channel to set the outgoing buffer size of.
   */
  @Override
  public void setOption(NetworkChannel sc) throws IOException
  {
    //sc.setOption(StandardSocketOptions.SO_SNDBUF, bufferSize);
    sc.setOption(StandardSocketOptions.SO_SNDBUF, bufferSize);
  }

  /**
   * Get the incoming buffer size of the given socket channel.
   * 
   * @param sc the channel to set the incoming buffer size of.
   */
  @Override
  public void getOption(NetworkChannel sc) throws IOException
  {
    //bufferSize = sc.getOption(StandardSocketOptions.SO_RCVBUF);
    bufferSize = sc.getOption(StandardSocketOptions.SO_SNDBUF);
  }

  /**
   * <p>Set the buffer size; allow reuse of this object.
   * @param i the buffer size.
   */
  public void setBufferSize(int i)
  {
    bufferSize = i;
  }
}
