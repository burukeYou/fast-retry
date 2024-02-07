package com.burukeyou.demo.retry.data;

import lombok.Data;

@Data
public class WeatherResult {

    public String data;

    public WeatherResult(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "【" + data + "】";
    }
}
