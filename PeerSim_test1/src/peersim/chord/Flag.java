package peersim.chord;

public class Flag {
	
	public static int sort_flag = 0;  // 是否需要按照chordId排序
	
	public static double drop_packet_rate = 0.8; // 恶意节点丢弃message的概率
	
	public static double malicious_node_rate = 0.4; // 有多少恶意节点
	
	public static int look_ahead_level = -1;  // 需要向前看几跳  -1=off 0=0-level 1=1-level 2=2-level
	
	public static int K = 3; //0-只去一个finger，1-取两个finger中优的那个，2-3个finger，以此类推  
	
	public Flag(){}
	
	public static void set_Sort_Flag()
	{
		sort_flag = 1;
	}
	
	public static void reset_Sort_Flag()
	{
		sort_flag = 0;
	}
}
