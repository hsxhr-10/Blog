CREATE TABLE orders_product (
    `id` bigint(11) NOT NULL AUTO_INCREMENT,
    `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记, 0 是未删除, 1 是已删除',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建的时间',
    `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改的时间',
    `order_id` varchar(255) NOT NULL UNIQUE COMMENT '订单ID',
    `product_id` varchar(255) NOT NULL UNIQUE COMMENT '商品ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表和商品表的多对多关系';


INSERT INTO orders_product(order_id, product_id)
VALUES
    ("70f9a7c6-bdd3-45e1-9bf9-0d61ce736b5e", "ae631a9b-cf29-4d19-8af1-3e282ec0b090"),
    ("23ff2f2a-bc09-4444-b415-121bd79df5df", "dedd9fb5-d206-4e7d-b6e7-b3f941006fdb"),
    ("d4cf20e1-818d-40dc-bcfa-0f13422d1bcf", "4cb68b17-2c3c-4a0a-985c-5249ed86cd1c"),
    ("7c4e743d-5c92-4a3c-8346-51e7512b1913", "3c5a0f8e-a5e7-4ba2-87fc-599c129f2cd2"),
    ("7580b5da-1c9f-4e42-b5ce-ba6ff2459ac6", "a473f8af-bdbd-418d-a986-50e10bd9673c"),
    ("29cbd137-b431-4e07-861f-5df996248d91", "ebc3cdbc-cdbc-42dd-908a-3a27062578a5");
