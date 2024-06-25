package test.hamster.emul.netchannel;

public interface TransferDecision
{
  /**
   * <p>The test buffer calls the implementation which will respond with the number of bytes that
   * should be transferrred.
   * 
   * @param advanceWithNoTransferCount the number of test system advances have been made with no transfer.
   * @param recvBufferAvailableBytes the amount of data that can be transferred into the receive buffer.
   * @param sendBufferAvailableBytes the amount of data that can be transferred out of the send buffer.
   * @return
   */
  public int bytesToTransfer(int advanceWithNoTransferCount, int recvBufferAvailableBytes, int sendBufferAvailableBytes);
}
