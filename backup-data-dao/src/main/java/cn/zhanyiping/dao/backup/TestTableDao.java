package cn.zhanyiping.dao.backup;

import cn.zhanyiping.domain.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TestTableDao extends JpaRepository<Test, Long> {

    @Query(nativeQuery=true, value = "select count(*) from test1 where create_time <= :createTime limit 30")
    Integer selectCountByCreateTime(@Param("createTime") String createTime);

    @Query(nativeQuery=true, value = "select * from test1 where create_time <= :createTime limit :limitCount")
    List<Test> selectListByCreateTime(@Param("createTime") String createTime , @Param("limitCount") Integer limitCount);

    @Transactional
    @Modifying
    @Query(nativeQuery=true, value = "delete from test1 where id in(:ids)")
    Integer batchDeleteByIds(@Param("ids")List<Long> ids);

}
