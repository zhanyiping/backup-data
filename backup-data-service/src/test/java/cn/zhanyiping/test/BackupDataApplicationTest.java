package cn.zhanyiping.test;

import cn.zhanyiping.dao.backup.TestTableDao;
import cn.zhanyiping.service.bushiness.BackupDataService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@SpringBootTest(classes=BackupDataApplicationTest.class)
@RunWith(SpringRunner.class)
@ComponentScan("cn.zhanyiping")
@EnableJpaRepositories("cn.zhanyiping.dao.backup")
@EntityScan("cn.zhanyiping.domain.entity")
public class BackupDataApplicationTest {

    @Autowired
    private BackupDataService backupDataService;

    @Autowired
    private TestTableDao testTableDao;

    @Test
    public void backupData() {
        backupDataService.backupData();
    }

    @Test
    public void contextLoads() {
        for (int i = 0; i < 100; i++) {
            cn.zhanyiping.domain.entity.Test test = new cn.zhanyiping.domain.entity.Test();
            test.setCreateTime(LocalDateTime.now());
            test.setName("name" + i);
            testTableDao.save(test);
        }
    }

}
