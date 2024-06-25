package test.hamster.comm.channels.channelop;

import hamster.comm.server.CommLoopCloser;
import hamster.comm.server.NonBlockingCommunicationServer;
import hamster.comm.wakeupschedule.ProcessCallbackScheduler;
import hamster.comm.wakeupschedule.WakeupCallback;

public abstract class AbstractChannelTestBedListener<T> implements WakeupCallback
{
  protected final ProcessCallbackScheduler pcs;
  
  private boolean shutdown = false;
  
  private boolean paused = false;
  
  private T liberatedChannel;

  private final CommLoopCloser loopCloser;
  
  /**
   * <p>Construct the listener.  Allow it to shut the NBCS thread down when finished.
   * 
   * @param commLoopCloser allows NBCS thread loop to be exited.
   * @param processCallbackScheduler allows a callback to be scheduled for some time in the future.
   */
  public AbstractChannelTestBedListener(NonBlockingCommunicationServer nbcs)
  {
    this.loopCloser = nbcs.getCommLoopCloser();
    this.pcs = nbcs.getProcessScheduler();
  }
  
  protected final synchronized void shutdown()
  {
    this.shutdown = true;
    
    //Tell the comm loop closer to shut down.
    loopCloser.closeCommLoop();
  }
  
  public final synchronized boolean isShutdown()
  {
    return this.shutdown;  
  }
  
  protected final synchronized void setPaused(boolean pausedState)
  {
    this.paused = pausedState;
  }
  
  public final synchronized boolean isPaused()
  {
    return this.paused;  
  }

  protected final synchronized void liberate(T channel)
  {
    this.liberatedChannel = channel;
  }
  
  public final synchronized T isLiberated()
  {
    return this.liberatedChannel;  
  }

 }