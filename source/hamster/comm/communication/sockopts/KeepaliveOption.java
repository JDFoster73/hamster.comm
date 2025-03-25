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
 * Network option to set - receive buffer size.  Used in a call to SocketChannel.setOption(...) (for example)
 * to pass option requirements.
 * 
 * @author Jim Foster &lt;jdfoster73@gmail.com&gt;
 */
public class KeepaliveOption implements SocketChannelOptionAccessor
{
  private boolean keepalive;

  /**
   * Create the option with the given receive buffer size in bytes.
   *
   * @param keepalive should keep the connection alive or not.
   */
  public KeepaliveOption(boolean keepalive)
  {
    this.keepalive = keepalive;
  }  

  /**
   * <p>Query the buffer size, for when this instance has been used to get a buffer size option.
   * 
   * @return the receive buffer size.
   */
  public boolean queryKeepaliveState()
  {
    return keepalive;
  }
  
  /**
   * Set the incoming buffer size of the given socket channel.
   * 
   * @param sc the channel to set the incoming buffer size of.
   */
  @Override
  public void setOption(NetworkChannel sc) throws IOException
  {
    sc.setOption(StandardSocketOptions.SO_KEEPALIVE, keepalive);
  }

  /**
   * Get the incoming buffer size of the given socket channel.
   * 
   * @param sc the channel to set the incoming buffer size of.
   */
  @Override
  public void getOption(NetworkChannel sc) throws IOException
  {
    keepalive = sc.getOption(StandardSocketOptions.SO_KEEPALIVE);
  }
}
