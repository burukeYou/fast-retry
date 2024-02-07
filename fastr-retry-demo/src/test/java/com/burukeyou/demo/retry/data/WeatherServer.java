package com.burukeyou.demo.retry.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  模拟天气服务
 */
public class WeatherServer {

    // Map<城市,请求次数>
    private static Map<String,Integer> countMap = new ConcurrentHashMap<>();

    /**
     * 获取某天气， 查询第5次后才返回结果
     * @param city
     * @return
     */
    public synchronized static WeatherResult getWeather(String city){
        Integer count = countMap.get(city);
        if (count == null){
            count = 0;
        }
        countMap.put(city,count+1);
        if (count < 5){
            return null;
        }
        return new WeatherResult(city + "_结果_" + count);
    }

}
