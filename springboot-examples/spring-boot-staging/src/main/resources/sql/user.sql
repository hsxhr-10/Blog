CREATE TABLE user (
    `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '用户id',
    `user_name` varchar(45) NOT NULL COMMENT '姓名',
    `age` MEDIUMINT NOT NULL COMMENT '年龄',
    `sex` varchar(45) NOT NULL DEFAULT 'female' COMMENT '性别',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '用户记录创建的时间',
    `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '用户资料修改的时间',
    `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基本信息';
