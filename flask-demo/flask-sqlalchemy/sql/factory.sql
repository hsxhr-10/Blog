CREATE TABLE factory (
    `id` bigint(11) NOT NULL AUTO_INCREMENT,
    `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记, 0 是未删除, 1 是已删除',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建的时间',
    `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改的时间',
    `factory_id` varchar(255) NOT NULL UNIQUE COMMENT '生产厂家ID',
    `name` varchar(45) NOT NULL COMMENT '生产厂家名称',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产厂家信息';


INSERT INTO factory(factory_id, name)
VALUES
    ("a1d760f2-275e-4efb-ae02-dc4d5434fb10", "工厂1号"),
    ("bce11a79-c28c-4968-ab2e-b2e5ffa7797b", "工厂2号"),
    ("44395768-dd93-46ac-b70f-0774494796b2", "工厂3号"),
    ("4c9b64f2-a29b-4f10-a442-afc7dede3096", "工厂4号"),
    ("3bd621bf-6dd4-4bd3-b253-0e9c9cc4da36", "工厂5号"),
    ("da3a189c-dd68-4e1c-9889-7541df58e44a", "工厂6号");
