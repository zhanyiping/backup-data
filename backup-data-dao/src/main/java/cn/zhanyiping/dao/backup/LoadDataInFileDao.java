package cn.zhanyiping.dao.backup;

import cn.zhanyiping.domain.entity.Test;
import java.util.List;

public interface LoadDataInFileDao {

    /**
     * 批量插入
     * @return
     * @throws Exception
     */
    int batchInsert(List<Test> list) throws Exception;

    /**
     * 查询备份表
     * @return
     * @throws Exception
     */
    List<Test> selectList(List<Test> list) throws Exception;

}
