/*
* Copyright (C) 2014 SlimRoms Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.internal.util.slim;

import java.lang.reflect.Method;

public class SlimInfo {

    private static final String BUILD_TYPE_PROP = "ro.slim.buildtype";
    private static final String SLIM_VERSION_PROP = "ro.slim.version";

    public static String getSlimBuildType() {
        return getProperty(BUILD_TYPE_PROP);
    }

    public static String getSlimVersion() {
        return getProperty(SLIM_VERSION_PROP);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String getProperty(String name) {
        String value = null;
        try {
            Class c = Class.forName("android.os.SystemProperties");
            Method m = c.getMethod("get", String.class);
            value = (String) m.invoke(null, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
}
