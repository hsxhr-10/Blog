CREATE TABLE product (
    `id` bigint(11) NOT NULL AUTO_INCREMENT,
    `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记, 0 是未删除, 1 是已删除',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建的时间',
    `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改的时间',
    `product_id` varchar(255) NOT NULL UNIQUE COMMENT '商品ID',
    `name` varchar(45) NOT NULL COMMENT '商品名称',
    `factory_id` varchar(255) NOT NULL UNIQUE COMMENT '关联的生产厂家ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息';


INSERT INTO product(product_id, name, factory_id)
VALUES
    ("ebc3cdbc-cdbc-42dd-908a-3a27062578a5", "商品A", "a1d760f2-275e-4efb-ae02-dc4d5434fb10"),
    ("a473f8af-bdbd-418d-a986-50e10bd9673c", "商品K", "bce11a79-c28c-4968-ab2e-b2e5ffa7797b"),
    ("3c5a0f8e-a5e7-4ba2-87fc-599c129f2cd2", "商品M", "44395768-dd93-46ac-b70f-0774494796b2"),
    ("4cb68b17-2c3c-4a0a-985c-5249ed86cd1c", "商品Q", "4c9b64f2-a29b-4f10-a442-afc7dede3096"),
    ("dedd9fb5-d206-4e7d-b6e7-b3f941006fdb", "商品L", "3bd621bf-6dd4-4bd3-b253-0e9c9cc4da36"),
    ("ae631a9b-cf29-4d19-8af1-3e282ec0b090", "商品Z", "da3a189c-dd68-4e1c-9889-7541df58e44a");
