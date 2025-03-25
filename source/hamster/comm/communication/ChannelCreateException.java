/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.communication;

/**
 * This exception class is a checked exception that is thrown when a request to create a channel
 * has thrown an exception or has a configuration problem.
 * 
 */
@SuppressWarnings("serial")
public class ChannelCreateException extends Exception
{

  /**
   * <p>Raise exception in response to an underlying problem while creating a new channel.
   * 
   * @param string the exception description.
   */
  public ChannelCreateException(String string)
  {
    super(string);
  }

}
