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

  exports hamster.comm.communication;
  exports hamster.comm.communication.sockopts;

  exports hamster.comm.internalchannel;



  exports hamster.comm.itf.controller;
  exports hamster.comm.itf.listener;

  exports hamster.comm.logging;

  exports hamster.comm.server;
  exports hamster.comm.server.exception;
  exports hamster.comm.server.listener;


  exports hamster.comm.wakeupschedule;

}