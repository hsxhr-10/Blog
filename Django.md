- [Django model “doesn't declare an explicit app_label”](https://stackoverflow.com/questions/40206569/django-model-doesnt-declare-an-explicit-app-label)
- [【Django Models】虚拟化提取Models公共的功能](https://www.cnblogs.com/inns/p/5562162.html)
- [pycharm中django的objects无代码提示、自动补全的真香方案](https://blog.csdn.net/ch_improve/article/details/110182289)

---

## 常用命令：

### django-admin

- 创建项目：django-admin startproject $project_name
- 根据模板创建子应用：django-admin startapp --template $app_template_dir $app_name

### python manage.py

- 创建超级用户：python manage.py createsuperuser
- 同步数据表：python manage.py makemigrations && python manage.py migrate
- 运行测试：python manage.py test
- 运行服务：python manage.py runserver
