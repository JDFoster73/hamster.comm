package hamster.comm.internalchannel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>The internal channel server creates socket channels which are to be used in internal bidirectional communications.
 * It runs an internal thread which creates internal socket channel instances.  It caches a number of socket channels which 
 * can be retrieved by a simple (synchronised) method call.  
 * <p>As channels are retrieved, more will be created by the internal thread.
 * <p>The socket channels can be used in pipe-like applications.  The Java Pipe object is actually implemented as two socket
 * channels, one only for reading and one only for writing.  It seems a waste.
 * 
 * @author jdf19
 *
 */
public class InternalChannelServer
{
  private static InternalChannelServer singleton;
  
  /**
   * <p>Get an internal channel server for creating internal channels that can be used for inter-process communications.
   * 
   * @return internal channel server instance.
   */
  public static synchronized InternalChannelServer getServer()
  {
    //Create the singleton if it doesn't exist.
    if(singleton == null)
    {
      singleton = new InternalChannelServer();
    }
    //Return the singleton.
    return singleton;
  }

  /**
   * The worker thread which creates internal channel objects.
   */
  private final Thread worker;
  
  /**
   * <p>Internal list holding {@link InternalChannelConnection} instances.
   */
  private final List<InternalChannelConnection> channelCache = new ArrayList<>();
  
  /**
   * <p>The internal channel server will aim to keep this number of cached connections ready to be used.
   */
  private final int CACHE_SIZE = 5;
  
  /**
   * <p>Get an internal channel connection from the cache.
   *   
   * @return {@link InternalChannelConnection} instance; two socket channels connected to each other.
   */
  public synchronized InternalChannelConnection getInternalConnection()
  {
    //Make sure we've got connections.
    return waitForCachedConnections();
  }
  
  /**
   * <p>Create the singleton channel server.  Create the worker thread which will create the first cached channels to be used.
   */
  private InternalChannelServer()
  {
    worker = new Thread(new Worker());
    worker.setDaemon(true);               //Automatically close on system exit.
    worker.setName(getClass().getSimpleName().toUpperCase() + "_WORKER");
    worker.start();
  }
  
  /**
   * <p>Wait for at least one cached connection and return one.  This is the only method which removes internal connection
   * objects from the cache.
   * 
   * @return internal connection.
   */
  private synchronized InternalChannelConnection waitForCachedConnections()
  {
    //Wait until there's at least one in the cache.  The cache is protected by synchronized methods so 
    //there will be no update to the cache until this method is finished.
    while(channelCache.size() == 0)
    {
      try
      {
        //If the cache is not available immediately then it may be that the OS is resetting the network
        //or something.
        //TODO if waiting for more than a couple of seconds, start logging warnings.
        wait();
      }
      catch (InterruptedException e)
      {
        //We don't interrupt this.
      }
    }
    
    //Notify.
    notifyAll();
    
    //Return first on queue.
    return channelCache.remove(0);
  }

  /**
   * <p>The worker thread will sit in this method until there is something to do, i.e. create internal
   * channels.  Don't start creating until there's only one left.
   */
  private synchronized void waitRequireMoreCachedConnections()
  {
    while(channelCache.size() > 1)
    {
      try
      {
        wait();
      }
      catch (InterruptedException e)
      {
        //We don't interrupt this.
      }
    }
  }
  
  /**
   * <p>Synchronised access for the cache, to determine if more connections are required.  The worker
   * thread will create connections up until the CACHE_SIZE limit.
   * 
   * @return true if more cached internal connections are required.
   */
  private synchronized boolean moreCachedConnectionsRequired()
  {
    return channelCache.size() < CACHE_SIZE;
  }
  
  /**
   * <p>Add created internal channels to the cache and notify other threads waiting on the lock.
   * 
   * @param aEnds the 'A' ends.
   * @param bEnds the 'B' ends.
   */
  private synchronized void addCachedChannels(SocketChannel aEnds, SocketChannel bEnds)
  {
    //Add the connected ends to the channel cache.
    channelCache.add(new InternalChannelConnection(aEnds, bEnds));
    
    //Adding channels.
    //System.out.println("Adding cached channel pair. " + System.currentTimeMillis());
    
    //Notify thread waiting for a connection.
    notifyAll();
  }
  
  /**
   * <p>Worker daemon thread.  It is the job of this thread to keep topping the connection
   * cache up when required.
   * 
   * @author jdf19
   *
   */
  class Worker implements Runnable
  {
    /**
     * <p>Authentication buffer.  Will be used to send and receive authentication data between the newly-created channels.
     */
    private final ByteBuffer authenticationBuffer = ByteBuffer.allocate(Long.BYTES);
    
    /**
     * <p>Secure random authentication data generator.
     */
    private final SecureRandom authRandom = new SecureRandom();
    
    /**
     * <p>Thread run method.
     */
    @Override
    public void run()
    {
      // TODO Auto-generated method stub
      while(true)
      {
        try
        {
          //Wait for connections required in the cache.
          waitRequireMoreCachedConnections();
          
          //Open a ServerSocketChannel to create the internal connections.
          ServerSocketChannel ssc = ServerSocketChannel.open();
          
          //Create address to bind server socket to - loopback adapter address and ephemeral port.
          InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
          
          //Bind to the server address.
          ssc.bind(inetSocketAddress);
          
          //Get the loopback address plus port number that the server channel has picked up.
          InetSocketAddress localServerAddress = (InetSocketAddress) ssc.getLocalAddress();

          //Create cached connections.
          while(moreCachedConnectionsRequired())
          {
            //Create socket end 1.
            SocketChannel aEnds = SocketChannel.open(localServerAddress);
            
            //Get the authenticator.
            long auth = authRandom.nextLong();
            
            //Write authentication data to aEnds.
            authenticationBuffer.clear();
            authenticationBuffer.putLong(auth);
            authenticationBuffer.flip();
            
            while(authenticationBuffer.hasRemaining())
            {
              aEnds.write(authenticationBuffer);
            }
            
            //Accept the other end.
            SocketChannel bEnds = ssc.accept();
            //Check authentication.
            authenticationBuffer.clear();
            while(authenticationBuffer.hasRemaining())
            {
              bEnds.read(authenticationBuffer);
            }
            
            //Check auth.
            authenticationBuffer.flip();
            if(auth == authenticationBuffer.getLong())
            {
              //Channels connected and authenticated.  Add to the map.
              addCachedChannels(aEnds, bEnds);
            }
            else
            {
              //PROBLEM!!  Authentiation failed??
            }
            
          }
          
        }
        catch (IOException e)
        {
          //What do do???????
          //TODO LOGGIT.
          e.printStackTrace();
        }
      }
    }
    
  }
}
