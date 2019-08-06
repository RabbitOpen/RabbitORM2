package rabbit.open.orm.dml.name;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Element;

import rabbit.open.orm.exception.EmptyAliasException;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.pool.SessionFactory;

public class NamedSQL extends SQLObject {

    private static final String FETCH = "fetch";

    private static final String JOIN_FETCH = "join-fetch";

    private static final String ALIAS = "alias";

    private static final String ENTITY = "entity";

    // 变量关键字的正则表达式
    private static final String VARIABLES_REPLACE_WORD = "\\$\\{(.*?)\\}";

    // 字段变量关键字
    private static final String FIELDS_REPLACE_WORD = "\\#\\{(.*?)\\}";

    private String targetAlias;
    
    private List<FetcherDescriptor> fetchDescriptors;

    private List<JoinFetcherDescriptor> joinFetchDescriptors;
    
    public NamedSQL(String sql, String queryName, Element element) {
        super(sql, queryName);
        analyseNameSQL(sql);
        targetAlias = element.attributeValue(ALIAS);
        joinFetchDescriptors = readJoinFetchers(element);
        fetchDescriptors = readFetchers(element);
    }

    /**
     * <b>Description 从节点中读取many2many、one2many关联查询对象 </b>
     * @param element
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<JoinFetcherDescriptor> readJoinFetchers(Element element) {
        List<JoinFetcherDescriptor> joinFetchers = new ArrayList<>();
        Iterator<Element> iterator = element.elementIterator(JOIN_FETCH);
        while(iterator.hasNext()){
            Element jf = iterator.next();
            JoinFetcherDescriptor fetcher = new JoinFetcherDescriptor(jf.attributeValue(ENTITY), 
                    jf.attributeValue(ALIAS));
            joinFetchers.add(fetcher);
        }
        return joinFetchers;
    }

    /**
     * <b>Description  从节点中读取many2one关联查询对象</b>
     * @param element
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<FetcherDescriptor> readFetchers(Element element) {
        List<FetcherDescriptor> fetchers = new ArrayList<>();
        Iterator<Element> iterator;
        iterator = element.elementIterator(FETCH);
        while(iterator.hasNext()){
            Element jf = iterator.next();
            FetcherDescriptor fetcher = new FetcherDescriptor(jf.attributeValue(ENTITY), jf.attributeValue(ALIAS));
            fetcher.setFetchDescriptors(readFetchers(jf));
            fetcher.setJoinFetchDescriptors(readJoinFetchers(jf));
            fetchers.add(fetcher);
        }
        return fetchers;
    }

    /**
     * 
     * <b>Description: 分析命名查询sql</b><br>
     * @param sql
     * 
     */
    private void analyseNameSQL(String sql) {
        Pattern pattern = Pattern.compile(VARIABLES_REPLACE_WORD);
        Matcher matcher = pattern.matcher(sql);
        int index = 0;
        while (matcher.find()) {
            if (SessionFactory.isEmpty(matcher.group(1))) {
                throw new RabbitDMLException(
                        "empty field is defined in NamedSQL[" + this.name + "]");
            }
            fieldsMapping.put(index++, matcher.group(1).trim());
            this.sql = this.sql.replace(matcher.group(0), "?");
        }
    }

    /**
     * <b>Description 替换字段sql片段</b>
     * @param fieldsSql
     */
    public String replaceFields(String fieldsSql) {
        Pattern pattern = Pattern.compile(FIELDS_REPLACE_WORD);
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            return sql.replace(matcher.group(0), fieldsSql);
        }
        return sql;
    }
    
    public String getAlias() {
        if (SessionFactory.isEmpty(targetAlias)) {
            throw new EmptyAliasException(name);
        }
        return targetAlias;
    }
    
    public List<JoinFetcherDescriptor> getJoinFetchDescriptors() {
        return joinFetchDescriptors;
    }
    
    public List<FetcherDescriptor> getFetchDescriptors() {
        return fetchDescriptors;
    }
}
