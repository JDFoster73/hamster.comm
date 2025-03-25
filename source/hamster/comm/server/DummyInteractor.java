package hamster.comm.server;

class DummyInteractor implements CommLoopInteractor
{
  
  @Override
  public boolean shouldWaitForNetworkEvent()
  {
    return true;
  }
  
  @Override
  public void handleLoopStart()
  {
  }
  

}
