package hamster.comm.itf.controller;

import hamster.comm.server.NonBlockingCommunicationApplicationServer;

/**
 * <p>The base channel controller is the basis of all channel controllers which are provided by the
 *  {@link NonBlockingCommunicationApplicationServer non-blocking communication server} when a channel is registered
 *  to it.
 *  <p>The non-blocking communication server calls the <code>initController(...)</code> method of the user-supplied listener with a sub-interface
 *  of this one corresponding to the type of channel that has been registered.  At that point, the channel is
 *  set up and the controller can be used for writing, pausing, resuming and closing operations depending on
 *  that the type of channel supports.  For example, a read pipe does not support writing data.
 *  
 * @author jdf19
 *
 */
public interface BaseChannelController
{
  /**
   * The user process which manages the channel can call this method to initiate an immediate
   * shutdown.  On a two-way data connection such as a socket channel, this will affect
   * a hard (non-graceful) shutdown. 
   */
  public void shut();
}
