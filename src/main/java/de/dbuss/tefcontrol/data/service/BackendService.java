package de.dbuss.tefcontrol.data.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class BackendService {

    @Async
    public ListenableFuture<String> longRunningTask() {


        try {
            // Simulate a long running task
            Thread.sleep(60000);
        } catch (InterruptedException e) {

         //   return AsyncResult.forValue("Backend Job abgebrochen:" + e.getMessage());
            e.printStackTrace();
            return AsyncResult.forValue("Backend Job abgebrochen:" + e.getMessage());
        }
        return AsyncResult.forValue("Backend Job finished!");


    }
}
