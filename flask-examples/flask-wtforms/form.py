from wtforms import Form, BooleanField, StringField, PasswordField, validators


class UserForm(Form):
    username = StringField("username", [
        validators.DataRequired(),
        validators.Length(min=2, max=20)
    ])
    email = StringField("email", [
        validators.DataRequired(),
        validators.Length(min=6, max=35)
    ])
    password = PasswordField("password", [
        validators.DataRequired(),
        validators.Length(min=10, max=30),
        validators.EqualTo("confirm", message="两次输入的密码必须一致")
    ])
    confirm = PasswordField("Repeat Password", [
        validators.DataRequired(),
        validators.Length(min=10, max=30),
    ])
    is_vip = BooleanField("is_vip", [validators.DataRequired()])
