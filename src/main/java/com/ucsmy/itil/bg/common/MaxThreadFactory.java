package com.ucsmy.itil.bg.common;

import java.util.concurrent.ThreadFactory;

public class MaxThreadFactory implements ThreadFactory {
    private int counter;
    private String name;

    public MaxThreadFactory(String name) {
        counter = 0;
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable run) {
        Thread t = new Thread(run, name + "-Thread-" + counter++);
        return t;
    }

}
