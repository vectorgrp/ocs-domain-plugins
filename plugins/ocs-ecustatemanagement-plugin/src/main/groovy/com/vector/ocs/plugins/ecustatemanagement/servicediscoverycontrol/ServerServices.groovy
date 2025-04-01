/**********************************************************************************************************************
 *  COPYRIGHT
 *  -------------------------------------------------------------------------------------------------------------------
 *  \verbatim
 *  Copyright (c) 2025 by Vector Informatik GmbH. MIT license:
 *
 *                Permission is hereby granted, free of charge, to any person obtaining
 *                a copy of this software and associated documentation files (the
 *                "Software"), to deal in the Software without restriction, including
 *                without limitation the rights to use, copy, modify, merge, publish,
 *                distribute, sublicense, and/or sell copies of the Software, and to
 *                permit persons to whom the Software is furnished to do so, subject to
 *                the following conditions:
 *
 *                The above copyright notice and this permission notice shall be
 *                included in all copies or substantial portions of the Software.
 *
 *                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *                EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *                MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *                NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *                LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *                OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *                WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  \endverbatim
 *  -------------------------------------------------------------------------------------------------------------------
 *  FILE DESCRIPTION
 *  -----------------------------------------------------------------------------------------------------------------*/
/*!        \file  ServerServices.groovy
 *        \brief  Class to handle the server services feature.
 *
 *      \details  -
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement.servicediscoverycontrol

import com.vector.ocs.plugins.ecustatemanagement.Feature

/**
 * Defines the attributes which are part of a Server Service feature
 */
class ServerServices extends Feature {
    public Boolean ImmediateRequestProcessing
    public List<Server> Servers

    ServerServices(String name, Boolean isActivated) {
        super(name, isActivated)
        ImmediateRequestProcessing = false
        Servers = new LinkedList<Server>()
    }

    /**
     * This method adds an event handler to the event handlers list.
     * @param EventHandler Event Handler in the configuration
     * @param server Identifier of the server from the configuration
     */
    void addEventHandler(Feature EventHandler, String server) {
        Server foundServer = Servers.find {it.name.contains(server)}
        foundServer.EventHandlers.add(EventHandler)
    }
}
