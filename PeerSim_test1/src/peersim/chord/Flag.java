package peersim.chord;

public class Flag {
	
	public static int sort_flag = 0;  // 是否需要按照chordId排序
	
	public static int drop_packet_rate = 0; // 丢弃message的概率
	
	public static int look_ahead_level = 0;  // 需要向前看几跳 
	
	public Flag()
	{
	}
	
	public static void set_Sort_Flag()
	{
		sort_flag = 1;
	}
	
	public static void reset_Sort_Flag()
	{
		sort_flag = 0;
	}
}
