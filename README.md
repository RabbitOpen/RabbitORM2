
     官网：http://www.rabbit-open.top/rabbit/orm2
    
================================== V 2.6.4 =========================================   
    
    1、修复RabbitDataSource被其它框架引用时事务支持失败的bug
    
    2、新增对可能造成连接泄漏操作的监控
    
================================== V 2.6.3 =========================================   
    
    1、迁移Spring事务支持逻辑，从SessionFactory中迁移到DataSource。以支持三方框架直接使用数据源

================================== V 2.6.2 =========================================   
    
    1、增强MultiDropFilter，允许相同字段的or条件
    
================================== V 2.6.1 =========================================   
    
    1、解耦数据源对SessionFactory的依赖
    
    2、将慢sql的相关配置迁移到数据源配置项中
    
================================== V 2.6.0 =========================================   
    
    1、修复实体类meta缓存信息在多线程环境下可能会被污染的bug。
    
    2、修改SpringDaoAdapter中日志成员变量的class取值。
    
================================== V 2.5.6 =========================================   
    
    1、加强查询、删除、更新对象过滤 条件api，新增addNotNullFilter
    
    2、优化SpringDaoAdaptor，使之支持实体服务类的多层级继承

================================== V 2.5.5 =========================================   
    
    1、调整多分支or条件方法名setMultiDropFilter为addMultiDropFilter，可以加多个OR条件
    
    2、修正querySpecifiedFields方法查询结果集策略，允许查询出所有字段都为空的对象。
    
================================== V 2.5.4 =========================================   
    
    1、修复querySpecifiedFields查询的字段不能为空的bug
    
    2、SessionFactory新增scanJar布尔属性，默认false，表示扫描实体类时是否需要扫描jar文件
    

================================== V 2.5.3 =========================================   
    
    1、修复打印慢sql时不能有空参数的异常
 
    
================================== V 2.5.2 =========================================   
    
    1、调整DataSourceMonitor的休眠方式，以信号量的方式进行睡眠等待。用以提升连接池关闭的速度 
    
    2、新增查询API querySpecifiedFields，支持不带主键的查询方式
    
    3、新增相同实体类映射同一表的异常提示
    
    4、重构部分方法名
    
================================== V 2.5.1 =========================================   
    
    1、修复慢sql显示值显示永远为第一条数据的内容的bug
    
    2、新增显示慢sql时屏蔽参数值的功能，配置SessionFactory的showMaskedSlowSql属性
    
    3、新增过滤条件为list时，入参不能为空的异常提醒

================================== V 2.5.0 =========================================   
    
    1、新增慢sql统计分析支持
    
================================== V 2.4.9 =========================================   
    
    1、AbstractQuery新增filterFields 方法，支持只查询特定字段
    
    2、扩展Entity方法，支持设置需要过滤的字段
    
    3、优化读写分离数据源，在事务流程中一直使用写数据源
    
================================== V 2.4.8 =========================================   
    
    1、新增注解Proxy类，代理所有自定义注解，优化注解访问，提升反射效率
    
    2、强化PackageScanner解决PackageScanner无法扫描springboot项目jar文件中的class文件的问题

================================== V 2.4.5 =========================================   
    
    1、优化PackageScanner，解决PackageScanner无法扫描jar文件中的class文件的问题

    2、优化SQLParser，解决SQLParser无法扫描jar文件中的xml文件的问题
    
    3、优化SQLParser，解决无网络环境下无法解析命名sql文件的bug
    
    4、强化命名查询，新增通过VO对象来设值过滤条件的api
    
    5、开放sql日志api，允许开发人员通过api显示sql日志
    
    6、修复IN条件不支持Date数组类型的bug
    
================================== V 2.4.4 =========================================   
    
    1、优化DMLFilter，简化自定义DMLFilter的使用
    
    2、调整showSQL方法执行时间点，允许显示加密后的数据
 

================================== V 2.4.3 =========================================   
    
    1、修改Insert类，新增空插入抛异常支持
    
    2、修改Update类、对所有数据库都不再支持更新主键字段
    
    3、重构部份代码逻辑      
    
================================== V 2.4.2 =========================================   
    
    1、新增对SQLite3的支持
    
    2、AbstractQuery、NamedQuery 新增list和unique方法。
    
    3、扩展查询对象功能， 允许同一个查询对象重复执行execute、count、list以及unique方法
    
    4、优化PackageScanner功能。使之在扫描jar文件时不会因为依赖包的缺失（NoClassDefFoundError）而终止扫描
    

================================== V 2.4.1 =========================================   
    
    1、扩展@Column注解，新增keyWord方法，标记当前字段名是数据库关键字。在生成dml语句时就会特殊处理该字段
    
    2、重构ddl代码，去掉部分重复代码
    
    3、将SpringDaoAdapter中的createNameMappedQuery更名为createFieldsMappingQuery
    
    4、新增支持【主表对象】简单OR类型过滤条件的API ----> setMultiDropFilter。MultiDropFilter不参与分表计算因子
    

================================== V 2.4 =========================================   
    
    1、完善RabbitDataSource新增SQLException关闭连接功能，主要是为应对网络闪断异常。是对DataSourceMonitor的补充完善。


================================== V 2.3 =========================================   
    
    1、新增分库支持。  规则：中间表必须和主表一个库， 关联表必须和主表一个库


================================== V 2.2 =========================================  

    1、新增分区表的支持, 分区表策略对SQLQuery、NamedQuery对象生效， 需要sql开发人员自己确认使用哪张表
    
                        分区表可以关联查询其它表，但是不能被其它表关联查询
                        外键字段不参与分区规则计算                
        

    2、修正2.0重构后meta信息中joinMetas信息在使用过程中被污染的bug。 修正为每次使用时都clone一个副本
  
    
 ================================== V 2.1 =========================================  
    
    1、新增dml操作前置过滤器 DMLFilter
    
               
 ================================== V 2.0 =========================================
    
    1、调整了框架架构，解耦了数据源，使得RABBIT ORM可以使用其它数据源工作
    
    2、不再支持Column 注解中的usedForMapping功能。统一通过API增添过滤条件
    
    3、强化了SQLQuery和JDBCQuery。两者都不再支持直接在代码写sql实现，转为在xml中写sql
    
    4、强化了Query类。动态添加内链接条件不再有表个数的限制
    
    5、解耦了方言实现和功能代码之间的关联




 
    
    
    
    
          