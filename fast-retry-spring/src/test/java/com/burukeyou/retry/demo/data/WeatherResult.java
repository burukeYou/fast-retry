package com.burukeyou.retry.demo.data;

import lombok.Data;

@Data
public class WeatherResult {

    public String data;

    private Integer count;

    public WeatherResult(String data, Integer count) {
        this.data = data;
        this.count = count;
    }

    public WeatherResult(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "【" + data + "】";
    }
}
