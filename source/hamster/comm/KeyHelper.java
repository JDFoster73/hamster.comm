/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm;

import java.nio.channels.SelectionKey;

/**
 * Provide simple utility methods to assist working with {@link java.nio.channels.SelectionKey}s.
 * 
 * @author Jim Foster 
 */
public class KeyHelper
{
  /**
   * Set the OP_WRITE interest in the given key without disturbing the other interest operations that have been set.
   * If the OP_WRITE has already been set then this call will have no effect. 
   * 
   * @param sk the selection key to set OP_WRITE in.
   */
  public static void setWritability(SelectionKey sk)
  {
    sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
  }

  /**
   * Clear the OP_WRITE interest in the given key without disturbing the other interest operations that have been set.
   * If the OP_WRITE has not been set then this call will have no effect. 
   * 
   * @param sk the selection key to clear OP_WRITE in.
   */
  public static void clearWritability(SelectionKey sk)
  {
    sk.interestOps(sk.interestOps() & ~SelectionKey.OP_WRITE);
  }

  /**
   * Set the OP_READ interest in the given key without disturbing the other interest operations that have been set.
   * If the OP_READ has already been set then this call will have no effect. 
   * 
   * @param sk the selection key to set OP_READ in.
   */
  public static void setReadability(SelectionKey sk)
  {
    sk.interestOps(sk.interestOps() | SelectionKey.OP_READ);
  }

  /**
   * Clear the OP_READ interest in the given key without disturbing the other interest operations that have been set.
   * If the OP_READ has not been set then this call will have no effect. 
   * 
   * @param sk the selection key to clear OP_READ in.
   */
  public static void clearReadability(SelectionKey sk)
  {
    sk.interestOps(sk.interestOps() & ~SelectionKey.OP_READ);
  }

  /**
   * Set the OP_ACCEPT interest in the given key without disturbing the other interest operations that have been set.
   * If the OP_ACCEPT has already been set then this call will have no effect. 
   * 
   * @param sk the selection key to set OP_ACCEPT in.
   */
  public static void setAcceptability(SelectionKey sk)
  {
    sk.interestOps(sk.interestOps() | SelectionKey.OP_ACCEPT);
  }

  /**
   * Clear the OP_ACCEPT interest in the given key without disturbing the other interest operations that have been set.
   * If the OP_ACCEPT has not been set then this call will have no effect. 
   * 
   * @param sk the selection key to clear OP_ACCEPT in.
   */
  public static void clearAcceptability(SelectionKey sk)
  {
    sk.interestOps(sk.interestOps() & ~SelectionKey.OP_ACCEPT);
  }

  /**
   * Update the OP_READ interest using the given boolean parameter as the target state.  This will not disturb any of the
   * other interests that have been set in the selection key.
   * 
   * @param key the selection key to clear OP_READ in.
   * @param b the state that the readability of the selection key should have.
   */
  public static void updateRedability(SelectionKey key, boolean b)
  {
    if(b)
    {
      setReadability(key);
    }
    else
    {
      clearReadability(key);
    }
  }

  /**
   * Update the OP_WRITE interest using the given boolean parameter as the target state.  This will not disturb any of the
   * other interests that have been set in the selection key.
   * 
   * @param key the selection key to clear OP_WRITE in.
   * @param b the state that the writability of the selection key should have.
   */
  public static void updateWriteability(SelectionKey key, boolean b)
  {
    if(b)
    {
      setWritability(key);
    }
    else
    {
      clearWritability(key);
    }
  }

  /**
   * Update the OP_ACCEPT interest using the given boolean parameter as the target state.  This will not disturb any of the
   * other interests that have been set in the selection key.
   * 
   * @param key the selection key to clear OP_ACCEPT in.
   * @param b the state that the acceptability of the selection key should have.
   */
  public static void updateAcceptability(SelectionKey key, boolean b)
  {
    if(b)
    {
      setAcceptability(key);
    }
    else
    {
      clearAcceptability(key);
    }
  }
}
