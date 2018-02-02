#reversetable
将数据库中的表，逆向生成Java实体类(JavaBean)

填充配置文件，名字随意，具体配置如下

`驱动`
driver=com.mysql.jdbc.Driver

`URL地址`
url=jdbc:mysql://localhost:3306/xxx?useUnicode=true&characterEncoding=UTF-8

`数据库用户名`
user=root

`数据库密码`
password=xx

`具体逆向表（目前只能单一导入）`
table=t_article

`用于标识是否将该数据库下的所有表全部逆向生成实体类，
非具体数据库名，具体数据库名在URL需要填写`
database=xxx

`需要存入到的包`
package=com.neal.main.entity

`作者注释信息`
author=Neal

然后再new一个TableConfiguration的带配置文件路径的有参对象，调用start()方法启动

目前支持的字段类型有
"bit"
"tinyint"
"smallint"
"int"
"bigint"
"float"
"numeric"
"decimal"
"real"
"money"
"varchar"
"char"
"nvarchar"
"nchar"
"text"
"datetime"
"image"
"Timestamp"
可自行修改源码进行添加，修改sqlTypeToJavaType()方法，即可
