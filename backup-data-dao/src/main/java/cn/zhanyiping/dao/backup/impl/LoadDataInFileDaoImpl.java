package cn.zhanyiping.dao.backup.impl;

import cn.zhanyiping.dao.backup.LoadDataInFileDao;
import cn.zhanyiping.domain.entity.Test;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LoadDataInFileDaoImpl implements LoadDataInFileDao {

    private static Pattern linePattern = Pattern.compile("_(\\w)");

    /**
     * 表的列名
     */
    private final static String TABLE_COLUMNS = "id,name,create_time";
    /**
     * 归档的sql语句
     */
    private final static String INSERT_SAL = "load data local infile \"sql.csv\" ignore into table test (id,name,create_time)";
    /**
     * 查询数据的sql前缀
     */
    private final static String SELECT_PREFIX = "select id,name,create_time from test where id in (";



    /**
     * 归档数据源
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Connection conn = null;

    /**
     * 将数据从输入流批量导入到数据库。
     * @param inputStream   输入流。
     * @return int         成功插入的行数。
     */
    private int realInsertByInputStream(InputStream inputStream) throws SQLException {
        if (null == inputStream) {
            log.info("输入流为NULL，没有数据导入。");
            return 0;
        }
        conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
        PreparedStatement statement = conn.prepareStatement(INSERT_SAL);
        //mysql-connector-java 5
//        if (statement.isWrapperFor(com.mysql.jdbc.Statement.class)) {
//            com.mysql.jdbc.PreparedStatement mysqlStatement = statement.unwrap(com.mysql.jdbc.PreparedStatement.class);
//            mysqlStatement.setLocalInfileInputStream(dataStream);
//            return mysqlStatement.executeUpdate();
//        }
        //mysql-connector-java 6
        if (statement.isWrapperFor(com.mysql.cj.api.jdbc.Statement.class)) {
            com.mysql.cj.jdbc.PreparedStatement mysqlStatement = statement.unwrap(com.mysql.cj.jdbc.PreparedStatement.class);
            mysqlStatement.setLocalInfileInputStream(inputStream);
            return mysqlStatement.executeUpdate();
        }
//        //mysql-connector-java 8，在本地测试未成功报 The used command is not allowed with this MySQL version ，估计和mysql版本有关，本地版本是5.7
//        if (statement.isWrapperFor(com.mysql.cj.jdbc.JdbcStatement.class)) {
//            com.mysql.cj.jdbc.JdbcPreparedStatement mysqlStatement = statement.unwrap(com.mysql.cj.jdbc.JdbcPreparedStatement.class);
//            mysqlStatement.setLocalInfileInputStream(dataStream);
//            return mysqlStatement.executeUpdate();
//        }
        return 0;
    }

    /**
     * 往 StringBuilder 里追加数据
     * @param builder StringBuilder
     * @param object  数据
     * @param endFlag  结束的标识,true 就是结束
     */
    private void builderAppend(StringBuilder builder, Object object , boolean endFlag) {
        builder.append(objectToString(object));
        if (endFlag) {
            builder.append("\n");
        } else {
            builder.append("\t");
        }
    }

    /**
     * append数据时的一些逻辑处理，如果数据本身含有特殊符号需要进行替换
     * @param object 数据
     */
    private String objectToString(Object object) {
        if (null == object) {
            return "\\N";
        }
        if (object instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) object);
        } else {
            String str = String.valueOf(object);
            if (str.contains("\t")) {
                str = str.replaceAll("\t", " ");
            }
            if (str.contains("\n")) {
                str = str.replaceAll("\n" ," ");
            }
            return str;
        }
    }

    /**
     * 通过 LOAD DATA LOCAL INFILE 批量导入数据到数据库
     * @param builder 拼接的数据
     */
    private int insertDataByLoadData(StringBuilder builder) throws SQLException, IOException {
        int rows = 0;
        InputStream input = null;
        try {
            byte[] bytes = builder.toString().getBytes();
            if (bytes.length > 0) {
                input = new ByteArrayInputStream(bytes);
                //批量插入数据。
                long beginTime = System.currentTimeMillis();
                rows = realInsertByInputStream(input);
                long endTime = System.currentTimeMillis();
                log.info(INSERT_SAL+":【插入" + rows + "行数据至MySql中，耗时" + (endTime - beginTime) + "ms。】");
            }
        } finally {
            if (null != input) {
                input.close();
            }
            if (null != conn) {
                conn.close();
            }
        }
        return rows;
    }

    /**
     * 批量插入数据
     * @param list 数据列表
     * @return 插入的行数
     * @throws Exception 抛出的异常
     */
    @Override
    public int batchInsert(List<Test> list) throws Exception{
        String[] columnArray = TABLE_COLUMNS.split(",");
        StringBuilder sb = new StringBuilder();
        for (Test operateLog :list) {
            for (int i = 0, size = columnArray.length ; i < size ; i++) {
                if (Objects.isNull(columnArray[i])) {
                    continue;
                }
                builderAppend(sb , getObjectValue(columnArray[i] , operateLog) , i == size - 1);
            }
        }
        int insertRow = insertDataByLoadData(sb);
        log.info("insert归档积分操作记录表数量insertRow："+insertRow+" 需插入的数量："+list.size());
        return insertRow;
    }

    /**
     * 获取object对象的name属性值
     * @param name 属性名称
     * @param object 源对象
     * @return 获取的属性值
     * @throws IntrospectionException 异常
     */
    private Object getObjectValue(String name , Object object) throws IntrospectionException {
        PropertyDescriptor pd = new PropertyDescriptor(lineToHump(name), object.getClass());
        //获取get方法
        Method getMethod = pd.getReadMethod();
        return ReflectionUtils.invokeMethod(getMethod, object);
    }

    /**
     * 下划线转驼峰
     * @param str
     * @return
     */
    private static String lineToHump(String str) {
        assert str != null;
        Matcher matcher = linePattern.matcher(str.trim());
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 查询备份表的数据，是否已经备份，已经备份后再删除主表数据
     * @return
     * @throws Exception
     */
    @Override
    public List<Test> selectList (List<Test> list) throws Exception {
        Statement statement = null;
        ResultSet rs = null;
        try {
            conn = jdbcTemplate.getDataSource().getConnection();
            //建立操作对象
            statement = conn.createStatement();
            //结果集
            String selectSql = getSelectSql(list);
            log.info("查询的sql语句为：", selectSql);
            rs = statement.executeQuery(selectSql);
            //处理结果集转换为实体类列表
            List<Test> testList = getTestList(rs);
            log.info("查询到的数据为", JSON.toJSONString(testList));
            return testList;
        } catch (Throwable t) {
            log.error("出现异常", t);
            throw t;
        } finally {
            //依次关闭结果集，操作对象，数据库对象
            if(Objects.nonNull(rs)){
                rs.close();
            }
            if(Objects.nonNull(statement)){
                statement.close();
            }
            if(Objects.nonNull(conn)){
                conn.close();
            }
        }
    }

    /**
     * 构造查询用的sql语句
     * @param list
     * @return
     */
    private String getSelectSql(List<Test> list) {
        StringBuilder stringBuilder = new StringBuilder();
        if(CollectionUtils.isEmpty(list)) {
            throw new RuntimeException("查询的数据不能为空");
        }
        for (Test operateLog : list) {
            stringBuilder.append(operateLog.getId());
            stringBuilder.append(",");
        }
        if (StringUtils.isBlank(stringBuilder.toString().trim())){
            throw new RuntimeException("查询的数据不能为空");
        }
        String substring = stringBuilder.substring(0, stringBuilder.lastIndexOf(","));
        return SELECT_PREFIX + substring + ")";
    }

    /**
     * 处理ResultSet转换为对应的实体类列表
     * @param rs
     * @return
     * @throws Exception
     */
    private List<Test> getTestList(ResultSet rs) throws Exception {
        List<Test> list = new ArrayList<>();
        while (rs.next()) {
            Test obj = new Test();
            obj.setId(rs.getLong("id"));
            list.add(obj);
        }
        return list;
    }

}