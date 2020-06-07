package cn.zhanyiping.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ComponentScan("cn.zhanyiping")
@EnableJpaRepositories("cn.zhanyiping.dao.backup")
@EntityScan("cn.zhanyiping.domain.entity")
@SpringBootApplication
public class BackupDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackupDataApplication.class, args);
    }
}
