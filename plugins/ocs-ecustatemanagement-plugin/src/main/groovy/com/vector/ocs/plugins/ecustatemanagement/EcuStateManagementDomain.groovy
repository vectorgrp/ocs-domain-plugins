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
/*!        \file  EcuStateManagementDomain.groovy
 *        \brief  The EcuStateManagementDomain contains abstract methods which are implemented for each domain.
 *                Furthermore it provides methods for activating and deactivating of features and setting of parameter.
 *
 *      \details  -
 *
 *********************************************************************************************************************/

package com.vector.ocs.plugins.ecustatemanagement

import com.vector.cfg.dom.deprecated.modemgt.pai.api.IModeManagementApi
import com.vector.cfg.dom.deprecated.modemgt.pai.bswm.IBswMAutoConfigurationApi
import com.vector.cfg.model.mdf.model.autosar.ecucdescription.MIContainer
import com.vector.ocs.core.api.OcsLogger

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Superclass representing the domains of Mode Management.
 * <ul>
 *     <li>Initializes the domain features and parameters by creating a data model.</li>
 *     <li>Processes the domain features and parameters according to the data model.</li>
 *     <li>Provides common methods to activate/deactivate features and set parameters.</li>
 * </ul>
 */
abstract class EcuStateManagementDomain {

    /**
     * Abstract method to initialize the domain features and parameters by creating a data model.
     * @param model Instance of EcuStateManagementModel
     * @param modeMngt Instance of IModeManagementApi
     * @param bswMCfg bswMConfig container
     */
    abstract void initializeDomainFeatures(EcuStateManagementModel model, IModeManagementApi modeMngt, MIContainer bswMCfg)

    /**
     * Abstract method to process the domain features and parameters according to the data model.
     * @param model Instance of EcuStateManagementModel
     * @param logger Instance of OcsLogger
     * @param modeMngt Instance of IModeManagementApi
     * @param bswMCfg bswMConfig container
     */
    abstract void processDomainFeatures(EcuStateManagementModel model, OcsLogger logger, IModeManagementApi modeMngt, MIContainer bswMCfg)

    /**
     * Initialize data model and process features and parameters of a domain
     * @param model Instance of EcuStateManagementModel
     * @param modeMngt Instance of IModeManagementApi
     * @param bswMCfg bswMConfig container
     * @param logger Instance of OcsLogger
     */
    void initializeAndProcessDomain(EcuStateManagementModel model, IModeManagementApi modeMngt, String domain, MIContainer bswMCfg, OcsLogger logger) {
        try {
            initializeDomainFeatures(model, modeMngt, bswMCfg)
            processDomainFeatures(model, logger, modeMngt, bswMCfg)
        } catch (exception) {
            logger.error("$exception $domain Domain cannot be accessed")
        }
    }

    /**
     * Deactivate feature in Mode Management domain
     * @param autoConfigApi Instance of IBswMAutoConfigurationApi
     * @param featureName Name of the feature
     * @param logger Instance for OCS specific logging
     */
    static void deactivateFeature(IBswMAutoConfigurationApi autoConfigApi, String featureName, OcsLogger logger) {
        try {
            autoConfigApi.deactivate(getIdentifier(featureName))
            logger.info("Deactivating " + getIdentifier(featureName) + ".")
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.error("$exception")
        }
    }

    /**
     * Activate feature in Mode Management domain
     * @param autoConfigApi Instance of IBswMAutoConfigurationApi
     * @param featureName Name of the feature
     * @param OcsLogger Instance for OCS specific logging
     */
    static void activateFeature(IBswMAutoConfigurationApi autoConfigApi, String featureName, OcsLogger logger) {
        try {
            autoConfigApi.activate(getIdentifier(featureName))
            logger.info("Activating " + getIdentifier(featureName) + ".")
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.error("$exception")
        }
    }

    /**
     * Set value of Boolean parameter in Mode Management domain
     * @param autoConfigApi Instance of IBswMAutoConfigurationApi
     * @param parameterName Name of the parameter
     * @param value Boolean value to be set
     * @param OcsLogger Instance for OCS specific logging
     */
    static void setParameters(IBswMAutoConfigurationApi autoConfigApi, String parameterName, Boolean value, OcsLogger logger) {
        try {
            autoConfigApi.set(parameterName).to(value)
            logger.info("Setting " + parameterName + " to " + value + ".")
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.error("$exception")
        }
    }


    /**
     * Set value of BigInteger parameter in Mode Management domain
     * @param autoConfigApi Instance of IBswMAutoConfigurationApi
     * @param parameterName Name of the parameter
     * @param value BigInteger value to be set
     * @param OcsLogger Instance for OCS specific logging
     */
    static void setParameters(IBswMAutoConfigurationApi autoConfigApi, String parameterName, BigInteger value, OcsLogger logger) {
        try {
            autoConfigApi.set(parameterName).to(value)
            logger.info("Setting " + parameterName + " to " + value + ".")
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.error("$exception")
        }
    }

    /**
     * Set value of BigDecimal parameter in Mode Management domain
     * @param autoConfigApi Instance of IBswMAutoConfigurationApi
     * @param parameterName Name of the parameter
     * @param value BigDecimal value to be set
     * @param OcsLogger Instance for OCS specific logging
     */
    static void setParameters(IBswMAutoConfigurationApi autoConfigApi, String parameterName, BigDecimal value, OcsLogger logger) {
        try {
            autoConfigApi.set(parameterName).to(value)
            logger.info("Setting " + parameterName + " to " + value + ".")
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.error("$exception")
        }
    }

    /**
     * Set value of BigDecimal parameter in Mode Management domain
     * @param autoConfigApi Instance of IBswMAutoConfigurationApi
     * @param parameterName Name of the parameter
     * @param value BigDecimal value to be set
     * @param OcsLogger Instance for OCS specific logging
     */
    static void setParameters(IBswMAutoConfigurationApi autoConfigApi, String parameterName, String value, OcsLogger logger) {
        try {
            autoConfigApi.set(parameterName).to(value)
            logger.info("Setting " + parameterName + " to " + value + ".")
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.error("$exception")
        }
    }

    /**
     * Get identifier of features in Mode Management Comfort Editor
     * @param rfIdentifier Identifier of the feature
     */
    static String getIdentifier(String rfIdentifier) {
        final Pattern regexPattern = ~'/([a-zA-Z0-9_:\\- ]+)|<span style="color:#.{6}">(.+?)</span>|<b>( \\*warning\\*)</b>'
        String returnValue = ""

        Matcher matcherResult = (rfIdentifier =~ regexPattern)
        for (int x = 0; x < matcherResult.size(); x++) {
            if (matcherResult[x] instanceof ArrayList) {
                ArrayList resultArray = matcherResult[x] as ArrayList
                for (int y = 1; y < resultArray.size(); y++) {
                    if (resultArray[y] != null) {
                        // warnings are individual findings which needs to be appended to the previous match as they belong together
                        // so in case it is not a *warning* the / delimiter must be added between root features / sub features etc
                        if ((resultArray[y] != " *warning*") && (resultArray[y] != " *not available*")) {
                            returnValue += "/"
                        }
                        returnValue += resultArray[y]
                    }
                }
            }
        }
        return returnValue
    }
}
