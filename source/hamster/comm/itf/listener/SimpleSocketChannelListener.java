package hamster.comm.itf.listener;

import hamster.comm.itf.controller.SocketChannelController;

/**
 * Default implementation of the {@link SocketChannelListener} interface.  only the {@link #handleDataRead()} and
 * {@link #initController(SocketChannelController)} are required to provide a functioning communication channel.
 * The other methods can be implemented as required.
 */
public abstract class SimpleSocketChannelListener implements SocketChannelListener
{
  @Override
  public abstract void initController(SocketChannelController controller);

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
  public void handleChannelWriteContinue()
  {
    //No op.  The superclass only implements this if required.
  }

  @Override
  public void handleServerLoopEnd()
  {
    //No op.  The superclass only implements this if required.
  }
}
