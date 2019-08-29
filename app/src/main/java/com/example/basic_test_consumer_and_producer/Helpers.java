package com.example.basic_test_consumer_and_producer;

public class Helpers {
    public static long getRandomLongBetweenRange(double min, double max){
        long x = (long) ((Math.random()*((max-min)+1))+min);
        return x;
    }
}
