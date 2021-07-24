from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import (
    Column,
    Integer,
    DateTime,
    String,
    DECIMAL,
    text
)
from sqlalchemy.dialects.mysql import TINYINT

_Base = declarative_base()


def to_dict(result):
    data = []
    for row in result:
        r = dict()
        for column in row.__table__.columns:
            r[column.name] = str(getattr(row, column.name))
        data.append(r)

    return data


class _BaseMixin(_Base):
    """ 基类 ORM, 包含一些必须的字段 """
    __abstract__ = True
    __bind_key__ = 'extension_model'

    id = Column(Integer, primary_key=True)
    is_deleted = Column(TINYINT, nullable=False, default=0)
    create_time = Column(DateTime, nullable=False, default=text("CURRENT_TIMESTAMP"))
    update_time = Column(DateTime, nullable=False, default=text("CURRENT_TIMESTAMP"))


class Factory(_BaseMixin):
    __tablename__ = "factory"

    factory_id = Column(String(255), nullable=False, unique=True)
    name = Column(String(45), nullable=False)


class Product(_BaseMixin):
    """ Factory 和 Product 一对多 """
    __tablename__ = "product"

    product_id = Column(String(255), nullable=False, unique=True)
    name = Column(String(45), nullable=False)
    factory_id = Column(String(255), nullable=False, unique=True)


class Orders(_BaseMixin):
    __tablename__ = "orders"

    order_id = Column(String(255), nullable=False, unique=True)
    price = Column(DECIMAL(13, 5), nullable=False, default=0)


class OrdersProduct(_BaseMixin):
    """ Orders 和 Product 多对多 """
    __tablename__ = "orders_product"

    order_id = Column(String(255), nullable=False, unique=True)
    product_id = Column(String(255), nullable=False, unique=True)
