
            官网：http://www.rabbit-open.top/rabbit/orm2

================================== V 2.0 =========================================
	
    1、调整了框架架构，解耦了数据源，使得RABBIT ORM可以使用其它数据源工作
    
    2、不再支持Column 注解中的usedForMapping功能。统一通过API增添过滤条件
    
    3、强化了SQLQuery和JDBCQuery。两者都不再支持直接在代码写sql实现，转为在xml中写sql
    
    4、强化了Query类。动态添加内链接条件不再有表个数的限制
    
    5、解耦了方言实现和功能代码之间的关联
	
	
            