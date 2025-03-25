/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.server.listener;

/**
 *
 * @author jdf19
 */
public interface MainCommunicationController extends EstablishedChannelCommunicationController, BiDirectionalCommunicationController, DatagramChannelCommunicationController
{
}
