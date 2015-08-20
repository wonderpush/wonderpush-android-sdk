/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.wonderpush.sdk;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;

/**
 * Parser for URL query strings.
 */
class QueryStringParser {

    private final String queryString;

    /**
     * The position of the current parameter.
     */
    private int paramBegin;
    private int paramEnd = -1;
    private int paramNameEnd;
    private String paramName;
    private String paramValue;

    /**
     * Construct a parser from the given URL query string.
     *
     * @param queryString
     *            the query string, i.e. the part of the URL starting
     *            after the '?' character
     */
    public QueryStringParser(String queryString) {
        this.queryString = queryString;
    }

    /**
     * Move to the next parameter in the query string.
     *
     * @return <code>true</code> if a parameter has been found; <code>false</code> if there are no more parameters
     */
    public boolean next() {
        int len = queryString.length();
        while (true) {
            if (paramEnd == len) {
                return false;
            }
            paramBegin = paramEnd == -1 ? 0 : paramEnd + 1;
            int idx = queryString.indexOf('&', paramBegin);
            paramEnd = idx == -1 ? len : idx;
            if (paramEnd > paramBegin) {
                idx = queryString.indexOf('=', paramBegin);
                paramNameEnd = idx == -1 || idx > paramEnd ? paramEnd : idx;
                paramName = null;
                paramValue = null;
                return true;
            }
        }
    }

    /**
     * Search for a parameter with a name in a given collection.
     * This method iterates over the parameters until a parameter with
     * a matching name has been found. Note that the current parameter is not
     * considered.
     *
     * @param names
     */
    public boolean search(Collection<String> names) {
        while (next()) {
            if (names.contains(getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the name of the current parameter.
     * Calling this method is only allowed if {@link #next()} has been called
     * previously and the result of this call was <code>true</code>. Otherwise the
     * result of this method is undefined.
     *
     * @return the name of the current parameter
     */
    public String getName() {
        if (paramName == null) {
            paramName = queryString.substring(paramBegin, paramNameEnd);
        }
        return paramName;
    }

    /**
     * Get the value of the current parameter.
     * Calling this method is only allowed if {@link #next()} has been called
     * previously and the result of this call was <code>true</code>. Otherwise the
     * result of this method is undefined.
     *
     * @return the decoded value of the current parameter
     */
    public String getValue() {
        if (paramValue == null) {
            if (paramNameEnd == paramEnd) {
                return null;
            }
            try {
                paramValue = URLDecoder.decode(queryString.substring(paramNameEnd + 1, paramEnd), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new Error(ex);
            }
        }
        return paramValue;
    }

    /**
     * Create a WonderPush.RequestParams from this query string
     */
    public static WonderPush.RequestParams getRequestParams(String queryString) {
        if (null == queryString)
            return null;

        QueryStringParser parser = new QueryStringParser(queryString);
        WonderPush.RequestParams result = new WonderPush.RequestParams();
        while (parser.next()) {
            result.put(parser.getName(), parser.getValue());
        }
        return result;
    }

}
