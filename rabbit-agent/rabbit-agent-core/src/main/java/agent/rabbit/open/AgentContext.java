package agent.rabbit.open;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AgentContext implements EnhanceRemission {

    // contextEntry开始执行的时间点
    private long start;

    // contextEntry执行结束的时间点
    private long end;

    // 当前context对应的入口方法
    private Method contextEntry;

    private AgentContext parent;

    // 自己在调用链中的层级
    private int level = 0;

    // 父级缩进
    private String parentIndent = "";

    private List<AgentContext> children = new ArrayList<>();

    public AgentContext(Method contextEntry) {
        this.contextEntry = contextEntry;
        this.start = System.currentTimeMillis();
    }

    /**
     * 操作结束
     */
    public void finish() {
        this.end = System.currentTimeMillis();
    }
    public long getCost() {
        return end - start;
    }

    /**
     * 告知父级context自己的信息
     * @param parent
     */
    public void setParent(AgentContext parent) {
        if (null != parent) {
            parent.children.add(this);
            this.parent = parent;
            this.level = parent.getLevel() + 1;
        }
    }

    public AgentContext getParent() {
        return parent;
    }

    private List<AgentContext> getChildren() {
        return children;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 获取调用链信息
     */
    public String getStacks() {
        return getStacks(true);
    }

    /**
     * 获取当前函数执行的信息
     * @param last  是否是父类函数中的最后一个子函数
     */
    public String getStacks(boolean last) {
        StringBuilder sb = getStackString(last);
        for (int i = 0; i < getChildren().size(); i++) {
            sb.append(getChildren().get(i).getStacks(i == getChildren().size() - 1));
        }
        return sb.toString();
    }

    /**
     * 获取当前函数执行的信息
     * @param last  是否是父类函数中的最后一个子函数
     */
    private StringBuilder getStackString(boolean last) {
        StringBuilder sb = new StringBuilder();
        if (getLevel() > 0) {
            sb.append(getParent().getParentIndent() + "|\t\n");
            sb.append(getParent().getParentIndent() + "|---");
            if (last) {
                parentIndent = getParent().getParentIndent() + " \t";
            } else {
                parentIndent = getParent().getParentIndent() + "|\t";
            }
            sb.append(contextEntry.getName() + ": children(" + children.size() + "), cost(" + getCost() + " ms)\n");
        } else {
            sb.append(contextEntry.getName() + ": children(" + children.size() + "), cost(" + getCost() + " ms)\n");
            parentIndent = "";
        }
        return sb;
    }

    private String getParentIndent() {
        return parentIndent;
    }
}
