package de.dbuss.tefcontrol;

import java.util.HashMap;
import java.util.Map;

public class GlobalProperties {

    public static Map<String, String> cache = new HashMap<>();
    public static final String GLOBAL_USER_NAME="Michael";


    public static Map<String, String> getCache(){
        return cache;
    }

    public static void putCache(String key, String value){
        cache.put(key,value);
    }

}
