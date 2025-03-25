package hamster.comm.itf.listener;

import hamster.comm.itf.controller.SocketChannelController;

/**
 * Default implementation of the {@link SocketChannelListener} interface.  only the {@link #handleDataRead()} and
 * {@link #initController(SocketChannelController)} are required to provide a functioning communication channel.
 * The other methods can be implemented as required.
 */
public abstract class SimpleSocketChannelListener implements SocketChannelListener
{
  protected SocketChannelController channelController;

  @Override
  public void initController(SocketChannelController controller)
  {
    this.channelController = controller;
  }

  @Override
  public void isClosing()
  {
    //No op.  The superclass only implements this if required.
  }

  @Override
  public void hasShut()
  {
    //No op.  The superclass only implements this if required.
  }

  @Override
  public void handleReadStop()
  {
    //No op.  The superclass only implements this if required.
  }

  @Override
  public abstract void handleDataRead();

  @Override
  public abstract void handleChannelWriteContinue();

  @Override
  public void handleServerLoopEnd()
  {
    //No op.  The superclass only implements this if required.
  }
}
