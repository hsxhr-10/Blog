- [pycharm中django的objects无代码提示、自动补全的真香方案](https://blog.csdn.net/ch_improve/article/details/110182289)
- [解决 Django 时区设置不生效问题](https://stackoverflow.com/questions/30588926/why-the-time-is-different-from-my-time-zone-in-settings-py)
- Django 的其中一个理念：提供各种抽象层封装、框架，目的是隔离应用代码和底层平台，方便切换不同平台时，无需修改上层代码

---

## 常用命令

### django-admin

- 创建项目：django-admin startproject $project_name
- 根据模板创建子应用：django-admin startapp --template $app_template_dir $app_name

### manage.py

- 创建超级用户：python manage.py createsuperuser
- 同步数据表：python manage.py makemigrations && python manage.py migrate
- 运行测试：python manage.py test
- 运行服务：python manage.py runserver

---

## Model

### 模型定义

- [普通字段类型](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#field-types)
    - [BigAutoField()](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#bigautofield)
    - [IntegerField()](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#integerfield)
    - [FloatField()](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#floatfield)
    - [DecimalField(max_digits=10, decimal_places=5)](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#decimalfield)
    - [BooleanField(default=False)](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#booleanfield)
    - [CharField(max_length=10)](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#charfield)
    - [EmailField(max_length=10)](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#emailfield)
    - [DateField(auto_now_add=True), DateField(auto_now=True)](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#datefield)
    - [DateTimeField(auto_now_add=True), DateTimeField(auto_now=True)](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#datetimefield)
    - [FileField(upload_to=$func_return_file_save_path)](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#filefield)
    - [ImageField(upload_to=$func_return_file_save_path, height_field=10, width_field=10)](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#imagefield)
    - [GenericIPAddressField()](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#genericipaddressfield)
    - [TextField()](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#textfield)
    - [URLField()](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#urlfield)
    - [UUIDField()](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#uuidfield)
- [关系字段类型](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#module-django.db.models.fields.related)
- [约束](https://docs.djangoproject.com/zh-hans/3.2/ref/models/fields/#field-options)

### migration

#### 步骤

1. 更改 model
2. 创建迁移：python manage.py makemigrations
3. 应用到数据库：python manage.py migrate

#### migrations 对应的实际 SQL

- python manage.py sqlmigrate $app_name $migration_name

### CRUD

- 增：$model.objects.create(**kw)
- 删：$instance.delete(**kw)
- 查：
    - 简单查询：
        - $model.objects.all()
        - $model.objects.filter(**kw)
        - $model.objects.get(**kw)
    - 详细：https://docs.djangoproject.com/en/3.2/topics/db/queries/#making-queries
- 改：$instance.attr = value

### 问题

- [Django model “doesn't declare an explicit app_label”](https://stackoverflow.com/questions/40206569/django-model-doesnt-declare-an-explicit-app-label)
- [How to revert the last migration?](https://stackoverflow.com/questions/32123477/how-to-revert-the-last-migration)
- [【Django Models】虚拟化提取Models公共的功能](https://www.cnblogs.com/inns/p/5562162.html)

---
