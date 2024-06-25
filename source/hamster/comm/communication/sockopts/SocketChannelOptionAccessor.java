/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.communication.sockopts;

import java.io.IOException;
import java.nio.channels.NetworkChannel;

/**
 * Abstract mechanism for setting read-write socket channel options.
 * 
 * @author Jim Foster 
 */
public interface SocketChannelOptionAccessor
{
  /**
   * The solid implementation will set the required socket channel option value upon this method call.
   * 
   * @param sc the socket channel to set the option of.
   * @throws IOException if the operation raised an IOException.
   */
  public void setOption(NetworkChannel sc) throws IOException;
  
  /**
   * The solid implementation will get the required socket channel option value upon this method call.
   * 
   * @param sc the socket channel to fet the option of.
   * @throws IOException if the operation raised an IOException.
   */
  public void getOption(NetworkChannel sc) throws IOException;
}
