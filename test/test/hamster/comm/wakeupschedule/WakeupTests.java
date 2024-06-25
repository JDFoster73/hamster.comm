package test.hamster.comm.wakeupschedule;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import hamster.comm.wakeupschedule.AsyncWakeupScheduler;
import hamster.comm.wakeupschedule.SyncWakeupScheduler;
import hamster.comm.wakeupschedule.WakeupCallback;

public class WakeupTests
{
  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss:S");
  
  //@Test
  public void testSyncSimple() throws InterruptedException
  {
    SyncWakeupScheduler sws = new SyncWakeupScheduler();
    
    WakeHandler wakeHandler1 = new WakeHandler(1);
    WakeHandler wakeHandler2 = new WakeHandler(2);
    WakeHandler wakeHandler3 = new WakeHandler(3);
    sws.requestWakeupCall(1, 1, 1000, wakeHandler1);
    sws.requestWakeupCall(2, 2, 500, wakeHandler2);
    sws.requestWakeupCall(3, 3, 750, wakeHandler3);
    sws.requestWakeupCall(1, 2, 1500, wakeHandler2);
    sws.requestWakeupCall(1, 3, 1250, wakeHandler3);
    
    long endTime = System.currentTimeMillis() + 10000;
    while(endTime > System.currentTimeMillis())
    {
      long serviceObjects = sws.serviceObjects();
      Thread.sleep(serviceObjects);
    }
  }
  
  @Test
  public void testAsyncSimple() throws InterruptedException
  {
    AsyncWakeupScheduler sws = new AsyncWakeupScheduler();
        
    WakeHandler wakeHandler1 = new WakeHandler(1);
    WakeHandler wakeHandler2 = new WakeHandler(2);
    WakeHandler wakeHandler3 = new WakeHandler(3);
    System.out.println("Async start at " + sdf.format(new Date()));
    sws.requestWakeupCall(1, 1, 1000, wakeHandler1);
    sws.requestWakeupCall(2, 2, 500, wakeHandler2);
    sws.requestWakeupCall(3, 3, 750, wakeHandler3);
    sws.requestWakeupCall(1, 2, 1500, wakeHandler2);
    sws.requestWakeupCall(1, 3, 1250, wakeHandler3);
    
    long endTime = System.currentTimeMillis() + 10000;
    int i = 0;
    while(endTime > System.currentTimeMillis())
    {
      switch(i++)
      {
        case 1:
          sws.requestWakeupCall(100, 1, 1800, wakeHandler1);
      }
      long serviceObjects = sws.serviceObjects();
      Thread.sleep(serviceObjects);
    }
  }

  private class WakeHandler implements WakeupCallback
  {
//    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss:S");
    
    private final int callerid;
    
    
    public WakeHandler(int callerid)
    {
      this.callerid = callerid;
    }


    @Override
    public void wakeup(int parameter, long wakeupTime)
    {
      System.out.println("Callback callerid " + callerid + " with param " + parameter + " at : " + sdf.format(new Date()));
    }
    
  }
}
