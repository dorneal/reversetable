package com.neal.main.domain;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 定制你的表属性
 *
 * @author Neal
 */
public class TableConfiguration {
    /**
     * 配置文件位置
     */
    private String propertyName;
    /**
     * 实体类生成所在包的路径
     */
    private String packageOutPath;
    /**
     * 作者名
     */
    private String authorName;
    /**
     * 表名
     */
    private String tableName;
    /**
     * 数据库名
     */
    private String databaseName;
    /**
     * 拿到对应数据库中所有实体类（实体类需要与其他表名做区分）
     */
    private List<String> tableNames;
    /**
     * 列名（字段）集合
     */
    private List<String> columnNames;
    /**
     * 列名类型集合
     */
    private List<String> columnTypeNames;
    /**
     * 是否需要导入java.util.*
     */
    private boolean fUtil = false;
    /**
     * 是否需要导入java.sql.*
     */
    private boolean fSql = false;

    /**
     * 构造，初始化
     */
    private TableConfiguration(String propertyName) {
        this.propertyName = propertyName;
        // 使用Properties类读取reverse.properties配置文件
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream(propertyName)) {
            properties.load(inputStream);
        } catch (IOException e) {
            System.out.println("没找到这个配置文件 " + e);
        }
        this.databaseName = properties.getProperty("database");
        this.tableName = properties.getProperty("table");
        this.packageOutPath = properties.getProperty("package");
        this.authorName = properties.getProperty("author");
    }

    /**
     * 创建多个实体类
     */
    private void genEntity(List<String> tableNames, Connection connection) {
        //递归生成文件
        for (String tableName : tableNames) {
            this.genEntity(tableName, connection);
        }
    }

    /**
     * 创建单个实体类
     */
    private void genEntity(String tableName, Connection connection) {
        fUtil = false;
        fSql = false;
        String sql = "SELECT * FROM " + tableName;
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            ResultSetMetaData setMetaData = preparedStatement.getMetaData();
            // 统计字段（列）
            int size = setMetaData.getColumnCount();
            columnNames = new ArrayList<>();
            columnTypeNames = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                columnNames.add(setMetaData.getColumnName(i + 1));
                columnTypeNames.add(setMetaData.getColumnTypeName(i + 1));
                if ("DATETIME".equalsIgnoreCase(columnTypeNames.get(i))) {
                    fUtil = true;
                }
                if ("IMAGE".equalsIgnoreCase(columnTypeNames.get(i))
                        || "TEXT".equalsIgnoreCase(columnTypeNames.get(i))
                        || "TIMESTAMP".equalsIgnoreCase(columnTypeNames.get(i))) {
                    fSql = true;
                }
            }
            System.out.println(columnNames);
            System.out.println(columnTypeNames);
        } catch (SQLException e) {
            System.out.println("未拿到字段集" + e);
        }
        // 将代码写入内存中去
        String content = parse(tableName);

        // 写入文件
        try {
            File directory = new File("");
            String outputPath = directory.getAbsolutePath() + "/src/"
                    + this.packageOutPath.replace(".", "/")
                    + "/";
            System.out.println("路径为：" + outputPath);
            //路径检查，不存在则创建
            File path = new File(outputPath);
            if (!path.exists()) {
                if (path.mkdir()) {
                    System.out.println("路径已被创建");
                }
            }
            System.out.println(path.exists());
            outputPath += initSml(initCap(tableName)) + ".java";
            File file = new File(outputPath);
            if (!file.exists()) {
                if (file.createNewFile()) {
                    System.out.println("文件已被创建");
                }
            }
            //写入到磁盘
            FileWriter fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw);
            // 将内存中的数据写入磁盘
            pw.println(content);
            pw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getAllEntityTable(Connection connection) {
        ResultSet rs;
        try {
            DatabaseMetaData dmd = connection.getMetaData();
            /*
             * TABLE_CAT String ==> 表类别(可为null)
             * TABLE_MODE String ==> 表模式(可为null)
             * TABLE_NAME String ==> 表名称
             * TABLE_TYPE String ==> 表类型
             * */
            rs = dmd.getTables(null, null, "%", null);
            while (rs.next()) {
                tableNames.add(rs.getString("TABLE_NAME"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 写入注释
     *
     * @param tableName 表名
     * @return 注释
     */
    private String parse(String tableName) {
        StringBuffer sb = new StringBuffer();
        sb.append("package ").append(this.packageOutPath).append(";\r\n");
        sb.append("\r\n");
        if (fUtil) {
            sb.append("import java.util.*;\r\n");
        }
        if (fSql) {
            sb.append("import java.sql.*;\r\n");
        }
        sb.append("\r\n");
        //注释部分
        sb.append("/**\r\n");
        sb.append(" * ").append(tableName).append("实体类\r\n");
        sb.append(" *\r");
        sb.append(" * @author ").append(this.authorName).append(" ")
                .append(new Timestamp(System.currentTimeMillis())).append("\r\n");
        sb.append(" */");

        //实体部分
        sb.append("\npublic class ").append(initSml(initCap(tableName))).append(" {\r\n");
        // 实体类属性
        processAllAttrs(sb);
        // get set方法
        processAllMethod(sb);
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 写入实体类属性
     *
     * @param sb StringBuffer
     */
    private void processAllAttrs(StringBuffer sb) {
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append("\tprivate ").append(sqlTypeToJavaType(columnTypeNames.get(i)))
                    .append(" ").append(initSml(columnNames.get(i))).append(";\r\n");
        }
    }

    /**
     * 写入GET，SET方法
     *
     * @param sb StringBuffer
     */
    private void processAllMethod(StringBuffer sb) {
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append("\r\tpublic void set").append(initSml(initCap(columnNames.get(i) + "(")))
                    .append(sqlTypeToJavaType(columnTypeNames.get(i)))
                    .append(" ").append(initSml(columnNames.get(i))).append(") {\r\n");
            sb.append("\t\tthis.").append(initSml(columnNames.get(i)))
                    .append(" = ").append(initSml(columnNames.get(i)))
                    .append(";\r\n");
            sb.append("\t}\n");
            sb.append("\n");
            sb.append("\tpublic ").append(sqlTypeToJavaType(columnTypeNames.get(i)))
                    .append(" get").append(initSml(initCap(initCap(columnNames.get(i)))))
                    .append("() {\r\n");
            sb.append("\t\treturn ").append(initSml(columnNames.get(i))).append(";\r\n");
            sb.append("\t}\r\n");
        }
    }

    /**
     * 将输入字符串的首字母改成大写
     *
     * @param str 字符串
     * @return 开头大写的字符串
     */
    private String initCap(String str) {
        char[] ch = str.toCharArray();
        final char startChar = 'a';
        final char endChar = 'z';
        if (ch[0] >= startChar && ch[0] <= endChar) {
            ch[0] -= 32;
        }
        return new String(ch);
    }

    /**
     * 将字符串‘_’删除后，后一位转成大写
     *
     * @param str 字符串
     * @return String
     */
    private String initSml(String str) {
        char[] ch = str.toCharArray();
        List<Character> list = new ArrayList<>();
        for (int i = 0, j = 0; i < ch.length; i++, j++) {
            if (ch[i] == '_') {
                if (i < ch.length - 1) {
                    if (ch[i + 1] >= 'A' && ch[i + 1] <= 'Z') {
                        list.add(ch[i + 1]);
                    } else {
                        list.add((char) (ch[i + 1] - 32));
                    }
                    i++;
                } else {
                    break;
                }
            } else {
                list.add(ch[i]);
            }
        }
        char[] c = new char[list.size()];
        for (int i = 0; i < list.size(); i++) {
            c[i] = list.get(i);
        }
        return new String(c);
    }

    /**
     * 数据库类型映射Java类型
     *
     * @param sqlType 字符串转类型
     * @return String
     */
    private String sqlTypeToJavaType(String sqlType) {
        final String bit = "bit";
        final String tinyint = "tinyint";
        final String smallint = "smallint";
        final String newInt = "int";
        final String bigint = "bigint";
        final String newFloat = "float";
        final String numeric = "numeric";
        final String decimal = "decimal";
        final String real = "real";
        final String money = "money";
        final String varchar = "varchar";
        final String newChar = "char";
        final String nvarchar = "nvarchar";
        final String nchar = "nchar";
        final String text = "text";
        final String datetime = "datetime";
        final String image = "image";
        final String timestamp = "Timestamp";
        if (bit.equalsIgnoreCase(sqlType)) {
            return "boolean";
        } else if (tinyint.equalsIgnoreCase(sqlType)) {
            return "byte";
        } else if (smallint.equalsIgnoreCase(sqlType)) {
            return "short";
        } else if (newInt.equalsIgnoreCase(sqlType)
                || bigint.equalsIgnoreCase(sqlType)) {
            return "long";
        } else if (newFloat.equalsIgnoreCase(sqlType)) {
            return "float";
        } else if (decimal.equalsIgnoreCase(sqlType)
                || numeric.equalsIgnoreCase(sqlType)
                || real.equalsIgnoreCase(sqlType)
                || money.equalsIgnoreCase(sqlType)) {
            return "double";
        } else if (varchar.equalsIgnoreCase(sqlType)
                || newChar.equalsIgnoreCase(sqlType)
                || nvarchar.equalsIgnoreCase(sqlType)
                || nchar.equalsIgnoreCase(sqlType)
                || text.equalsIgnoreCase(sqlType)) {
            return "String";
        } else if (datetime.equalsIgnoreCase(sqlType)) {
            return "Date";
        } else if (image.equalsIgnoreCase(sqlType)) {
            return "Blob";
        } else if (timestamp.equalsIgnoreCase(sqlType)) {
            return "Timestamp";
        }
        return null;
    }

    /**
     * 调用此方法启动
     */
    private void start() {
        Properties properties = new Properties();
        InputStream inputStream = getClass().getResourceAsStream(propertyName);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String driver = properties.getProperty("driver");
        String user = properties.getProperty("user");
        String url = properties.getProperty("url");
        String pass = properties.getProperty("password");
        Connection conn = null;
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        if (databaseName != null && !"".equals(databaseName)
                && tableName != null && !"".equals(tableName)) {
            System.out.println("databaseName 和 tableName 不能同时存在");
        } else {
            // 如果配置文件中有数据库名字，则可以拿到其他其中所有的实体类
            if (databaseName != null && !"".equals(databaseName)) {
                // 获取所有实体表名字
                tableNames = new ArrayList<>();
                if (conn != null) {
                    getAllEntityTable(conn);
                }
                System.out.println(tableNames);
                // 为每个实体表生成实体类
                genEntity(tableNames, conn);
            } else {
                genEntity(tableName, conn);
            }
        }
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 出口
     */
    public static void main(String[] args) {
        new TableConfiguration("../resource/reverse.properties").start();
    }
}
