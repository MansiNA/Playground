package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectQSEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Async
@Service
public class BackendService {

    public ListenableFuture<String> longRunningTask() {
        try {
            // Simulate a long running task
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return AsyncResult.forValue("OK");
    }


    public ListenableFuture<ProjectQSEntity> getQsResult(JdbcTemplate jdbcTemplate, ProjectQSEntity projectQS, Map<Integer, List<Map<String, Object>>> rowsMap ) {

        String sql = projectQS.getSql();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        if (rows.isEmpty()) {
            projectQS.setResult(Constants.OK);
        } else {
            rowsMap.put(projectQS.getId(), rows);
            projectQS.setResult(Constants.FAILED);
        }
        return AsyncResult.forValue(projectQS);
    }
}
