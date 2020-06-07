package cn.zhanyiping.service.bushiness.impl;

import cn.zhanyiping.dao.backup.LoadDataInFileDao;
import cn.zhanyiping.dao.backup.TestTableDao;
import cn.zhanyiping.domain.entity.Test;
import cn.zhanyiping.service.bushiness.BackupDataService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class BackupDataServiceImpl implements BackupDataService {

    @Autowired
    private LoadDataInFileDao loadDataInFileDao;

    @Autowired
    private TestTableDao testTableDao;

    private final static Integer SELECT_COUNT = 10000;

    private final static Integer INSERT_COUNT = 2000;

    @Override
    public void backupData() {
        try {
            //归档的条件为归档三个月前的记录表数据
//            String currentTime = LocalDateTime.now().minusMonths(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            //查询待归档数量
            int backupCount = testTableDao.selectCountByCreateTime(currentTime);
            log.info("开始执行，此次待归档数据为："+ backupCount + "条,归档条件为："+ currentTime );
            //循环次数
            int cycleCount = backupCount / SELECT_COUNT;
            if (backupCount % SELECT_COUNT != 0){
                cycleCount++;
            }
            int totalInsertCount = 0;
            log.info("此次归档数据循环次数为："+ cycleCount );
            for (int i =0 ; i < cycleCount ; i++) {
                //查询待归档数据，一次查询一万条，归档一万条
                List<Test> list = testTableDao.selectListByCreateTime(currentTime, SELECT_COUNT);
                if (CollectionUtils.isEmpty(list)) {
                    continue;
                }
                totalInsertCount += batchInsertMap(list);
            }
            if (backupCount == totalInsertCount) {
                log.debug("此次归档数据为："+ totalInsertCount + "条" );
            } else {
                log.error("归档数据出现异常，此次归档数据为："+ totalInsertCount + "条" );
            }
        } catch (Throwable t) {
            log.error("归档数据出现异常出现异常" , t);
        }
    }
    /**
     * 循环归档开始
     * @param testList
     * @return
     * @throws Exception
     */
    private int batchInsertMap(List<Test> testList) throws Exception {
       int insertCount = 0 ;
            //如果数量大于每次插入的数量，需分批插入,一次插入2000跳
            if (testList.size() > INSERT_COUNT) {
                List<List<Test>> partitionList = Lists.partition(testList, INSERT_COUNT);
                for (List<Test> itemList :partitionList) {
                    insertCount += realBackup(itemList);
                }
            } else {
                insertCount += realBackup(testList);
            }
        return insertCount;
    }

    /**
     * 归档并且删除
     * @param testList
     * @return
     * @throws Exception
     */
    private int realBackup(List<Test> testList) throws Exception {
        int insertCount = loadDataInFileDao.batchInsert(testList);
        batchDelete(insertCount, testList);
        return insertCount;
    }

    /**
     * 批量删除归档完数据
     * @param insertCount
     * @param testList
     * @return
     */
    private void batchDelete(int insertCount , List<Test> testList) throws Exception {
        try {
            //查询归档数据
            List<Test> list = loadDataInFileDao.selectList(testList);
            List<Long> idList = new ArrayList<>(list.size());
            list.forEach(test -> idList.add(test.getId()));
            int deleteCount = testTableDao.batchDeleteByIds(idList);
            if (deleteCount == insertCount) {
                log.debug("批量删除成功，删除｛" + deleteCount + "｝条");
            } else {
                log.error("部分删除成功，删除成功｛" + deleteCount + "｝条");
            }
        } catch (Throwable t) {
            log.error("删除数据异常", t);
        }
    }

}
