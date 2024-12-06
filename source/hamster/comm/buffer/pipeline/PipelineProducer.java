package hamster.comm.buffer.pipeline;

public interface PipelineProducer
{
  public void produceToBuffer(SequentialBlockWriter writer);
}
