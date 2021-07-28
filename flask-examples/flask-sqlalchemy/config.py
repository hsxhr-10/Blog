"""
connection_url 格式: dialect+driver://username:password@host:port/database?参数
"""
from enum import Enum


class DatabaseType(Enum):
    MYSQL = "MySQL"


class MySQLConfig:
    CONNECTION_URL_TEMPLATE = "{dialect}+{driver}://{username}:{password}@{host}:{port}/{database}?charset={charset}"

    def __init__(self, dialect="mysql", driver="pymysql", username="root", password=None,
                 host="localhost", port=3306, database="test", charset="utf8mb4"):
        self.dialect = dialect
        self.driver = driver
        self.username = username
        self.password = password
        self.host = host
        self.port = port
        self.database = database
        self.charset = charset

        self.connection_url = MySQLConfig.CONNECTION_URL_TEMPLATE.format(
            dialect=self.dialect, driver=self.driver, username=self.username,
            password=self.password, host=self.host, port=self.port,
            database=self.database, charset=self.charset
        )


mysql_config = MySQLConfig(username="xxx", password="xxx", host="xxx")
