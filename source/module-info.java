/**
 * 
 */
/**
 * @author jdf19
 *
 */
module hamster.comm
{
  requires slf4j.api;

  exports hamster.comm;

  exports hamster.comm.buffer;
  exports hamster.comm.buffer.block;
  exports hamster.comm.buffer.block.itf;
  exports hamster.comm.buffer.pipeline;
  exports hamster.comm.buffer.factory;
  exports hamster.comm.buffer.durable;
  exports hamster.comm.buffer.stream;

  exports hamster.comm.communication;
  exports hamster.comm.communication.sockopts;

  exports hamster.comm.internalchannel;

  exports hamster.comm.ipc.blob;
  exports hamster.comm.ipc.blob.impl;

  exports hamster.comm.itf.controller;
  exports hamster.comm.itf.listener;

  exports hamster.comm.logging;

  exports hamster.comm.server;
  exports hamster.comm.server.exception;
  exports hamster.comm.server.listener;

  exports hamster.comm.turbologging;

  exports hamster.comm.wakeupschedule;

}