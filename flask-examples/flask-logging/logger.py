import logging
from enum import Enum

import loguru

from exception import LogLevelError


class LoggerType(Enum):
    CONCURRENT_SAFE_LOGGER = "ConcurrentSafeLogger"


class LoggerLevel(Enum):
    DEBUG = "DEBUG"
    INFO = "INFO"
    WARNING = "WARNING"
    ERROR = "ERROR"
    CRITICAL = "CRITICAL"


class BaseLogger:
    """ 基于 logging 模块的日志基类 """

    FORMATTER = "%(asctime)s [%(pathname)s:%(lineno)d:%(funcName)s] %(message)s"

    def __init__(self, name, level):
        """
        :param name: logger 名字
        :param level: 日志等级
        """
        if level not in [item.value for item in LoggerLevel]:
            raise LogLevelError

        self.name = name
        self.level = level
        self.formatter = BaseLogger.FORMATTER
        self.logger = logging.getLogger(name)
        self.logger.setLevel(level)

    def get_logger(self):
        return self.logger


class ConcurrentSafeLogger(BaseLogger):
    """ 基于 loguru 模块的日志类

    使用:

        from logger import ConcurrentSafeLogger

        PATH_LOG = "test.log"

        logger = ConcurrentSafeLogger(name="ConcurrentSafeLogger", level="INFO", log_path=PATH_LOG).get_logger()

        logger.debug('This is a debug message')
        logger.info('This is an info message')
        logger.warning('This is a warning message')
        logger.error('This is an error message')
        logger.critical('This is a critical message')
    """

    FORMATTER = "<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> " + \
        " | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>"

    ROTATION_BY_SECOND = "{} seconds"
    ROTATION_BY_MINUTE = "{} minutes"
    ROTATION_BY_HOUR = "{} hours"
    ROTATION_BY_DAY = "{} days"
    ROTATION_BY_WEEK = "{} weeks"
    ROTATION_BY_SIZE = "{} MB"

    COMPRESSION_TYPE = "tar.gz"

    def __init__(self, name, level, log_path, enqueue=True):
        """
        :param log_path: 日志文件绝对路径
        :param enqueue: 多进程安全, 异步安全
        """
        BaseLogger.__init__(self, name, level)

        loguru.logger.remove()
        loguru.logger.add(
            sink=log_path,
            level=level,
            format=ConcurrentSafeLogger.FORMATTER,
            colorize=None,
            enqueue=enqueue,
            rotation=ConcurrentSafeLogger.ROTATION_BY_DAY.format(1),
            compression=ConcurrentSafeLogger.COMPRESSION_TYPE
        )
        self.logger = loguru.logger
