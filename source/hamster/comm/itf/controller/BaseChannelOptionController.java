package hamster.comm.itf.controller;

import hamster.comm.communication.sockopts.SocketChannelOptionAccessor;

public interface BaseChannelOptionController
{
  /**
   * <p>Set the given socket channel option on the open channel.
   * 
   * @param optionAccess the option access instance for accessing channel options.
   */
  public void setOption(SocketChannelOptionAccessor optionAccess);

  /**
   * <p>Get the given socket channel option on the open channel.
   * 
   * @param optionAccess the option access instance for accessing channel options.
   */
  public void getOption(SocketChannelOptionAccessor optionAccess);
}
