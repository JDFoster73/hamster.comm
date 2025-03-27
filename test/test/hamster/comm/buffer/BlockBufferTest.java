package test.hamster.comm.buffer;

import hamster.comm.buffer.BufferFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class BlockBufferTest
{
  private static BufferFactory bufferFactory;

  @BeforeClass
  public static void beforeClass()
  {
    bufferFactory = BufferFactory.getDefaultBufferFactory();
  }

  @Test
  public void consumerTransferReformMessageTest()
  {
//    PipelineBlockBuffer receive = new PipelineBlockBuffer(PipelineBufferFactory.getBufferFactory());
//    PipelineBlockBuffer send = new PipelineBlockBuffer(PipelineBufferFactory.getBufferFactory());
//
//    //Create a message in the receive buffer.
//    receive.produceData(p -> {
//      //Reserve length.
//      p.produceInt(0);
//      p.produceBytes(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
//
//      //Fill in the length
//      p.produceIntAt(p.getBlockLength(), 0);
//    });
//
//    //Consume the message.
//    receive.toAdhocConsumer(c -> {
//      //Read the length.
//      int i = c.consumeInt();
//      //Check the length is enough.
//      if(i >= c.bytesInBlock())
//      {
//        //Can service the message.
//        send.produceData(p -> {
//          //Length.
//          p.produceInt(i + 4);
//          //ID
//          p.produceInt(1234);
//          //Transfer from consumer to producer.
//          p.transferConsumerContent(c, i - 4);
//        });
//
//        //Return the number of bytes to consume.
//        //return true;
//      }
//
//      //return false;
//    });
//
//    send.toAdhocConsumer(c -> {
//      int len = c.consumeInt();
//      int id = c.consumeInt();
//      byte[] by = new byte[len - 8];
//      c.consumeBytes(by);
//      //return len;
//      //return true;
//    });
  }

  /**
   * Simple test to produce raw byte data from a source, consume to a destination and compare.
   *
   */
  @Test
  public void produceFromBytesTest()
  {
//    byte[] source1 = new byte[17];
//    fillBufferWithRandomByteData(source1);
//    byte[] source2 = new byte[35];
//    fillBufferWithRandomByteData(source2);
//
//    //Create pipeline buffer and produce.
//    PipelineBlockBuffer testSourceBuffer = new PipelineBlockBuffer(bufferFactory);
//    testSourceBuffer.produceFromBytes(source1);
//    testSourceBuffer.produceFromBytes(source2);
//
//    //Read the byte array back and compare.
//    byte[] target = new byte[17 + 35];
//    testSourceBuffer.consumeBytes(target);
//
//    //Compare the arrays.
//    Assert.assertTrue(compareSourceTarget(source1, target, 0));
//    Assert.assertTrue(compareSourceTarget(source2, target, 17));
  }

  /**
   * Produce data from a callback and check it.
   */
  @Test
  public void produceCallbackDataTest()
  {
//    //Create pipeline buffer and produce.
//    PipelineBlockBuffer testSourceBuffer = new PipelineBlockBuffer(bufferFactory);
//
//    //Produce callback data.
//    testSourceBuffer.produceData(p -> {
//      p.produceByte((byte) 0x32);
//      p.produceChar('h');
//      p.produceDouble(234.432432);
//      p.produceFloat(23.434F);
//      p.produceInt(234252);
//      p.produceLong(0x3ff3cc454L);
//      p.produceShort((short) 1234);
//    });
//
//    //Consume these data and check the fields.
//    testSourceBuffer.toAdhocConsumer(c -> {
//      if(c.consumeByte() != (byte) 0x32) throw new IllegalStateException();
//      if(c.consumeChar() != 'h') throw new IllegalStateException();
//      if(c.consumeDouble() != 234.432432) throw new IllegalStateException();
//      if(c.consumeFloat() != 23.434F) throw new IllegalStateException();
//      if(c.consumeInt() != 234252) throw new IllegalStateException();
//      if(c.consumeLong() != 0x3ff3cc454L) throw new IllegalStateException();
//      if(c.consumeShort() != (short) 1234) throw new IllegalStateException();
//
//      int i = c.numberOfConsumedBytes();
//
//      //return 29;
//      //return true;
//    });
//
//    //Check that the buffer is empty and all data have been consumed.
//    Assert.assertTrue(testSourceBuffer.isEmpty());
  }

  @Test
  public void testPipelineTransfer()
  {
//    //Create source buffer and populate.
//    byte[] source1 = new byte[17];
//    fillBufferWithRandomByteData(source1);
//
//    //Create pipeline buffer and produce.
//    PipelineBlockBuffer testSourceBuffer = new PipelineBlockBuffer(bufferFactory);
//    testSourceBuffer.produceFromBytes(source1);
//
//    //Create target buffer.
//    PipelineBlockBuffer testTargetBuffer = new PipelineBlockBuffer(bufferFactory);
//    testTargetBuffer.produceDataFromPipelineBuffer(testSourceBuffer);
//
//    //Consume the transferred bytes and compare.
//    byte[] target1 = new byte[17];
//    testTargetBuffer.consumeBytes(target1);
//
//    //Compare.
//    Assert.assertTrue(compareSourceTarget(source1, target1, 0));
//
//    //Check that the buffers are both in the correct state to execute further operations.
//    byte[] checkSource1 = new byte[37];
//    byte[] checkTarget1 = new byte[37];
//
//    fillBufferWithRandomByteData(checkSource1);
//
//    //Source buffer check.
//    testSourceBuffer.produceFromBytes(checkSource1);
//    testSourceBuffer.consumeBytes(checkTarget1);
//    Assert.assertTrue(compareSourceTarget(checkSource1, checkTarget1, 0));
//
//    //Target buffer check.
//    testTargetBuffer.produceFromBytes(checkSource1);
//    testTargetBuffer.consumeBytes(checkTarget1);
//    Assert.assertTrue(compareSourceTarget(checkSource1, checkTarget1, 0));
  }

  @Test
  public void pipelineBufferTransferTest()
  {
//    //Create source and target buffer.
//    PipelineBlockBuffer testSourceBuffer = new PipelineBlockBuffer(bufferFactory);
//    PipelineBlockBuffer testTargetBuffer = new PipelineBlockBuffer(bufferFactory);
//
//    //Produce some test data.
//    testSourceBuffer.produceData(p -> {
//      p.produceInt(0x12345678);
//    });
//
//    //Produce a message which includes data from the source buffer.
//    testTargetBuffer.produceData(p -> {
//      p.produceInt(8);
//      p.produceFromPipeline(testSourceBuffer);
//    });
//
//    //Consume and check.
//    testTargetBuffer.toAdhocConsumer(c -> {
//      int i1 = c.consumeInt();
//      int i2 = c.consumeInt();
//      //return 8;
//      //return true;
//    });
  }

  @Test
  public void pipelineBufferCipherTest() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException
  {
//    //Create and initialise the cipher with a secret key.
//    Cipher cp = Cipher.getInstance("AES");
//    MessageDigest md = MessageDigest.getInstance("SHA-256");
//    byte[] dig = md.digest("apassword".getBytes());
//    SecretKeySpec sks = new SecretKeySpec(dig, "AES");
//    cp.init(Cipher.ENCRYPT_MODE, sks);
//
//    //Create source and target buffer.
//    CipherAwarePipelineBuffer testSourceBuffer = new CipherAwarePipelineBuffer(bufferFactory, cp);
//    PipelineBlockBuffer testIntermediateBuffer = new PipelineBlockBuffer(bufferFactory);
//    CipherAwarePipelineBuffer testTargetBuffer = new CipherAwarePipelineBuffer(bufferFactory, cp);
//
//    //Produce some test data.
//    testSourceBuffer.produceData(p -> {
//      p.produceInt(0x12345678);
//    });
//
//    //Produce a message which includes data from the source buffer.
//    testIntermediateBuffer.produceData(p -> {
//      p.produceInt(0);
//      p.produceFromPipeline(testSourceBuffer);
//      p.produceIntAt(p.getBlockLength(), 0);
//    });
//
//    //Set the cipher to decrypt.
//    cp.init(Cipher.DECRYPT_MODE, sks);
//
//    //Consume and decrypt the payload to a buffer.
//    testIntermediateBuffer.toAdhocConsumer(c -> {
//      //Get the block length.
//      int blockLen = c.consumeInt();
//      //Decrypt the payload into target buffer.
//      c.consumeToPipeline(testTargetBuffer, blockLen - 4);
//      //Return the block length.
//      //return blockLen;
//      //return true;
//    });
//
//    //Check the decrypted contents.
//    testTargetBuffer.toAdhocConsumer(c -> {
//      int decry = c.consumeInt();
//      //return 4;
//      //return true;
 //   });

//    //Consume and check.
//    testTargetBuffer.consumeData(c -> {
//      int i1 = c.consumeInt();
//      int i2 = c.consumeInt();
//      return 8;
//    });
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  private void fillBufferWithRandomByteData(byte[] source)
  {
    for(int i = 0; i < source.length; i++)
    {
      //Random number.
      source[i] = (byte) (Math.random() * 255);
    }
  }

  private boolean compareSourceTarget(byte[] source, byte[] target, int startTargetIx)
  {
    for(int i = 0; i < source.length; i++)
    {
      //Compare.
      if(source[i] != target[i + startTargetIx])
      {
        return false;
      }
    }

    //Still here, passed.  Return true.
    return true;
  }
}
