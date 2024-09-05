package com.stripe.android.stripe3ds2.init

import com.stripe.android.stripe3ds2.exceptions.InvalidInputException

// NOTE: Copied from reference app spec

/**
 * The ConfigParameters class shall represent the configuration parameters that
 * are required by the 3DS SDK for initialization. The following are
 * characteristics of the configuration parameters:
 *  *  All related configuration parameters can be placed in a single group.
 * Note: A group is not pre-defined. The 3DS SDK implementer can define it to
 * logically group configuration parameters.
 *  *  Explicit parameter grouping is optional. If a group name is not
 * provided, then parameters are grouped under a default group.
 *  *  Duplicate parameter names cannot be used within a given group or the
 * default group.
 *
 * The app creates a ConfigParameters object and sets the required parameter values.
 */
interface ConfigParameters {

    /**
     * The addParam method shall add a configuration parameter either to the
     * specified group or to the default group.
     *
     * @param groupName Group to which the configuration parameter is to be added.
     * Note: If a group is not specified, then the default group shall be used.
     * @param paramName Name of the configuration parameter.
     * @param paramValue Value of the configuration parameter.
     * @throws InvalidInputException This exception shall be thrown if paramName
     * is null.
     */
    @Throws(InvalidInputException::class)
    fun addParam(groupName: String?, paramName: String, paramValue: String?)

    /**
     * The getParamValue method shall return a configuration parameter’s value
     * either from the specified group or from the default group.
     *
     * @param groupName Group from which the configuration parameter’s value is to
     * be returned. Note: If the group is null, then the default group shall be
     * used.
     * @param paramName Name of the configuration parameter.
     * @return The getParamValue method returns the value of the specified
     * configuration parameter as a string.
     * @throws InvalidInputException This exception shall be thrown if paramName
     * is null.
     */
    @Throws(InvalidInputException::class)
    fun getParamValue(groupName: String?, paramName: String): String?

    /**
     * The removeParam method shall remove a configuration parameter either from
     * the specified groupName or from the default groupName. It should return the value
     * of the parameter that it removes.
     *
     * @param groupName Group from which the configuration parameter is to be
     * removed. Note: If groupName is null, then the default groupName shall be used.
     * @param paramName Name of the configuration parameter.
     * @return The removeParam method should return the value of the parameter
     * that it removes.
     * @throws InvalidInputException This exception shall be thrown if paramName
     * is null.
     */
    @Throws(InvalidInputException::class)
    fun removeParam(groupName: String?, paramName: String): String?
}
