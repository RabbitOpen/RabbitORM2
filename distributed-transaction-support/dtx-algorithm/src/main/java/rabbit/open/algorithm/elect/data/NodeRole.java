package rabbit.open.algorithm.elect.data;

/**
 * 节点角色
 * @author xiaoqianbin
 * @date 2019/11/27
 **/
public enum NodeRole {

	LEADER("主节点"), FOLLOWER("从节点"), OBSERVER("吃瓜群众");

	NodeRole(String desc) {

	}
}
