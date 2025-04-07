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
    //Store the controller instance.
    this.channelController = controller;

    //Call the initialised() method.  Subclasses can extend this to get a notification when the
    //controller has been initialised and so can start using the instance to send data for example.
    initialised();
  }

  /**
   *  Empty, default implementation of the initialised() method which is called when the channel controller
   *  instance is received.
   */
  protected void initialised()
  {

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

  //@Override
  //public abstract void handleDataRead();

  @Override
  public abstract void handleChannelWriteContinue();

  @Override
  public void handleServerLoopEnd()
  {
    //No op.  The superclass only implements this if required.
  }
}
